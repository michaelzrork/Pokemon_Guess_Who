package com.example.pokemonguesswho.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.game.GameState
import com.example.pokemonguesswho.network.bluetooth.BluetoothConnectionManager
import com.example.pokemonguesswho.network.bluetooth.BluetoothDeviceInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

data class BoardData(
    val pokemonIds: List<Int>,
    val hostPokemonId: Int = -1
)

enum class LobbyState {
    IDLE,
    WAITING_FOR_OPPONENT,
    SCANNING,
    DEVICE_LIST,
    CONNECTING,
    CONNECTED,
    ERROR
}

class PokemonViewModel(application: Application) : AndroidViewModel(application) {

    private val gameManager = GameManager()
    private val gson = Gson()
    val bluetoothManager = BluetoothConnectionManager(application.applicationContext)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _pokemonList = MutableStateFlow<List<GamePokemon>>(emptyList())
    val pokemonList: StateFlow<List<GamePokemon>> = _pokemonList.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0f)
    val loadingProgress: StateFlow<Float> = _loadingProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Shuffling animation state
    private val _isShuffling = MutableStateFlow(false)
    val isShuffling: StateFlow<Boolean> = _isShuffling.asStateFlow()

    private val _shuffleDisplayPokemon = MutableStateFlow<GamePokemon?>(null)
    val shuffleDisplayPokemon: StateFlow<GamePokemon?> = _shuffleDisplayPokemon.asStateFlow()

    // Lobby state
    private val _lobbyState = MutableStateFlow(LobbyState.IDLE)
    val lobbyState: StateFlow<LobbyState> = _lobbyState.asStateFlow()

    // Expose Bluetooth discovered devices (only confirmed hosted games)
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = bluetoothManager.discoveredDevices

    // Whether we're still scanning/probing for games
    val isScanning: StateFlow<Boolean> = bluetoothManager.isScanning

    // Expose connected device name
    val connectedDeviceName: StateFlow<String?> = bluetoothManager.connectedDeviceName

    // "Player Found!" notification for host game screen overlay
    private val _opponentFoundMessage = MutableStateFlow<String?>(null)
    val opponentFoundMessage: StateFlow<String?> = _opponentFoundMessage.asStateFlow()

    // Exit confirmation dialog state
    private val _showExitDialog = MutableStateFlow(false)
    val showExitDialog: StateFlow<Boolean> = _showExitDialog.asStateFlow()

    // Card reveal animation: IDs of cards whose images have loaded and should flip face-up
    private val _revealedCardIds = MutableStateFlow<Set<Int>>(emptySet())
    val revealedCardIds: StateFlow<Set<Int>> = _revealedCardIds.asStateFlow()

    // Whether the board is ready to be shown (my pokemon image loaded, board set)
    private val _boardReady = MutableStateFlow(false)
    val boardReady: StateFlow<Boolean> = _boardReady.asStateFlow()

    fun showExitConfirmation() {
        _showExitDialog.value = true
    }

    fun dismissExitConfirmation() {
        _showExitDialog.value = false
    }

    // Gen 1 Pokemon (IDs 1-151) used for shuffle animation visuals
    private var gen1Pokemon: List<GamePokemon> = emptyList()
    private val imageLoader = ImageLoader(application)

    init {
        loadPokemon()
    }

    private fun loadPokemon() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadingProgress.value = 0f

                // Load all Pokemon data from bundled JSON asset (instant, no network)
                val pokemonDataList = withContext(Dispatchers.IO) {
                    val jsonString = getApplication<Application>().assets
                        .open("pokemon_all.json")
                        .bufferedReader()
                        .use { it.readText() }
                    val listType = object : TypeToken<List<GamePokemon>>() {}.type
                    gson.fromJson<List<GamePokemon>>(jsonString, listType)
                }

                _pokemonList.value = pokemonDataList
                gen1Pokemon = pokemonDataList.filter { it.pokemonId <= 151 }

                // Preload only Gen 1 images (151) for the shuffle animation
                val total = gen1Pokemon.size
                val completed = AtomicInteger(0)

                val preloadJobs = gen1Pokemon.map { pokemon ->
                    async(Dispatchers.IO) {
                        preloadImage(pokemon.imageUrl)
                        val done = completed.incrementAndGet()
                        _loadingProgress.value = done.toFloat() / total.toFloat()
                    }
                }
                preloadJobs.awaitAll()

                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load Pokemon: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /** Preload a single image into Coil's disk cache. */
    private suspend fun preloadImage(url: String) {
        try {
            suspendCancellableCoroutine { cont ->
                val request = ImageRequest.Builder(getApplication())
                    .data(url)
                    .size(512, 512)
                    .listener(
                        onSuccess = { _, _ -> cont.resume(Unit) },
                        onError = { _, _ -> cont.resume(Unit) }
                    )
                    .build()
                imageLoader.enqueue(request)
            }
        } catch (_: Exception) { }
    }

    /** Preload images for a list of board Pokemon in parallel, returning when all are cached. */
    private suspend fun preloadBoardImages(board: List<GamePokemon>) {
        board.map { pokemon ->
            viewModelScope.async(Dispatchers.IO) {
                preloadImage(pokemon.imageUrl)
            }
        }.awaitAll()
    }

    /**
     * Preload board images one-by-one in order, revealing each card as it loads.
     * A small stagger delay ensures the flip wave looks smooth even when images
     * are already cached.
     */
    private suspend fun revealBoardImagesSequentially(board: List<GamePokemon>) {
        for (pokemon in board) {
            withContext(Dispatchers.IO) {
                preloadImage(pokemon.imageUrl)
            }
            _revealedCardIds.value = _revealedCardIds.value + pokemon.pokemonId
            // Small stagger so flips cascade visually even when images are cached
            delay(60)
        }
    }

    // ---- HOST FLOW ----

    /**
     * Start a new game as the host.
     * Generates the board, runs shuffle animation, then starts BT server.
     */
    fun startNewGame() {
        viewModelScope.launch {
            // Clear any previous board and reveal state
            _gameState.value = GameState()
            _revealedCardIds.value = emptySet()
            _boardReady.value = false
            _isShuffling.value = true

            val allPokemon = _pokemonList.value
            val shuffleSource = gen1Pokemon.ifEmpty { allPokemon }

            // Generate board while animation plays
            val board = gameManager.generateGameBoard(allPokemon)
            val myPokemon = board.random()

            // Preload only the selected pokemon image during shuffle
            val myPokemonPreloadJob = async(Dispatchers.IO) {
                preloadImage(myPokemon.imageUrl)
            }

            // Shuffle animation uses Gen 1 Pokemon (already cached)
            repeat(15) {
                _shuffleDisplayPokemon.value = shuffleSource.random()
                delay(250)
            }

            // Wait for my pokemon image to be ready
            myPokemonPreloadJob.await()

            // Set the board (all cards start face-down) and stop shuffling
            _shuffleDisplayPokemon.value = null
            _gameState.value = GameState(
                board = board,
                myPokemon = myPokemon,
                showEliminated = true,
                isHost = true
            )
            _isShuffling.value = false
            _boardReady.value = true

            // Now reveal cards one-by-one with a flip wave
            revealBoardImagesSequentially(board)

            // Start Bluetooth server for opponent to join
            _lobbyState.value = LobbyState.WAITING_FOR_OPPONENT
            startBluetoothServer()
        }
    }

    private fun startBluetoothServer() {
        // Set board data on the GATT server before starting
        val boardJson = exportBoardAsJson()
        bluetoothManager.setBoardData(boardJson)

        bluetoothManager.startServerAsync(
            scope = viewModelScope,
            onConnected = {
                // Client received the board data
                viewModelScope.launch {
                    _lobbyState.value = LobbyState.CONNECTED

                    // Show "Player Found!" notification on game screen
                    val name = bluetoothManager.connectedDeviceName.value ?: "Opponent"
                    _opponentFoundMessage.value = "Player Found! $name joined!"

                    // Clean up BLE resources after a short delay
                    delay(1000)
                    bluetoothManager.disconnect()

                    delay(2000)
                    _opponentFoundMessage.value = null
                }
            },
            onError = {
                _lobbyState.value = LobbyState.ERROR
            }
        )
    }

    // ---- CLIENT FLOW ----

    /**
     * Start scanning for available host devices.
     */
    fun startJoinGame() {
        _lobbyState.value = LobbyState.SCANNING
        bluetoothManager.startDiscovery()

        // After a short delay, move to device list state
        viewModelScope.launch {
            delay(500)
            _lobbyState.value = LobbyState.DEVICE_LIST
        }
    }

    /**
     * Connect to a specific host device and receive the board.
     */
    fun connectToHost(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            _lobbyState.value = LobbyState.CONNECTING

            // Connect via BLE GATT, send JOIN, read board data, disconnect — all in one call
            val boardJson = bluetoothManager.connectAndGetBoardData(device.address)

            if (boardJson != null) {
                val success = joinGameFromJson(boardJson)
                if (success) {
                    _lobbyState.value = LobbyState.CONNECTED
                } else {
                    _lobbyState.value = LobbyState.ERROR
                }
            } else {
                _lobbyState.value = LobbyState.ERROR
            }
        }
    }

    // ---- SHARED ----

    private fun exportBoardAsJson(): String {
        val board = _gameState.value.board
        val hostPokemonId = _gameState.value.myPokemon?.pokemonId ?: -1
        val boardData = BoardData(
            pokemonIds = board.map { it.pokemonId },
            hostPokemonId = hostPokemonId
        )
        return gson.toJson(boardData)
    }

    fun joinGameFromJson(json: String): Boolean {
        return try {
            val boardData = gson.fromJson(json, BoardData::class.java)
            val allPokemon = _pokemonList.value
            val pokemonMap = allPokemon.associateBy { it.pokemonId }
            val shuffleSource = gen1Pokemon.ifEmpty { allPokemon }

            val board = boardData.pokemonIds.mapNotNull { id ->
                pokemonMap[id]?.copy(isEliminated = false)
            }

            if (board.size < 2) return false

            viewModelScope.launch {
                _revealedCardIds.value = emptySet()
                _boardReady.value = false
                _isShuffling.value = true

                // Pick a pokemon that isn't the host's pokemon
                val available = if (boardData.hostPokemonId > 0) {
                    board.filter { it.pokemonId != boardData.hostPokemonId }
                } else {
                    board
                }
                val myPokemon = available.random()

                // Preload only the selected pokemon image during shuffle
                val myPokemonPreloadJob = async(Dispatchers.IO) {
                    preloadImage(myPokemon.imageUrl)
                }

                // Shuffle animation uses Gen 1 Pokemon (already cached)
                repeat(12) {
                    _shuffleDisplayPokemon.value = shuffleSource.random()
                    delay(250)
                }

                // Wait for my pokemon image to be ready
                myPokemonPreloadJob.await()

                // Set board (all cards face-down), then clear shuffling
                _gameState.value = GameState(
                    board = board,
                    myPokemon = myPokemon,
                    showEliminated = true,
                    isHost = false
                )

                _shuffleDisplayPokemon.value = null
                _isShuffling.value = false
                _boardReady.value = true

                // Reveal cards one-by-one with a flip wave
                revealBoardImagesSequentially(board)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun togglePokemonElimination(pokemon: GamePokemon) {
        val updated = gameManager.togglePokemonElimination(pokemon)
        val newBoard = _gameState.value.board.map {
            if (it.pokemonId == pokemon.pokemonId) updated else it
        }
        _gameState.value = _gameState.value.copy(board = newBoard)
    }

    fun toggleShowEliminated() {
        _gameState.value = _gameState.value.copy(
            showEliminated = !_gameState.value.showEliminated
        )
    }

    fun resetLobby() {
        bluetoothManager.disconnect()
        _lobbyState.value = LobbyState.IDLE
    }

    fun endGame() {
        bluetoothManager.disconnect()
        _gameState.value = GameState()
        _lobbyState.value = LobbyState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}
