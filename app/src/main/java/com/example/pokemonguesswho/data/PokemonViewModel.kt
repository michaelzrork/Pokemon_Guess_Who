package com.example.pokemonguesswho.data

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.game.GameState
import com.example.pokemonguesswho.network.PokemonApiClient
import com.example.pokemonguesswho.network.bluetooth.BluetoothConnectionManager
import com.example.pokemonguesswho.network.bluetooth.BluetoothDeviceInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BoardData(
    val pokemonIds: List<Int>,
    val hostPokemonId: Int = -1
)

data class SavedGameData(
    val boardPokemonIds: List<Int>,
    val eliminatedIds: List<Int>,
    val myPokemonId: Int,
    val showEliminated: Boolean,
    val cardSizeDp: Float,
    val isHost: Boolean
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
    private val prefs = application.getSharedPreferences("pokemon_game", Context.MODE_PRIVATE)

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

    init {
        loadPokemon()
    }

    private fun loadPokemon() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _loadingProgress.value = 0f
                val response = PokemonApiClient.pokemonService.getPokemonList(limit = 151)
                val pokemonDataList = mutableListOf<GamePokemon>()
                val total = response.results.size

                response.results.forEachIndexed { index, result ->
                    try {
                        val details = PokemonApiClient.pokemonService.getPokemonByName(result.name)
                        val imageUrl = details.sprites.other?.officialArtwork?.frontDefault
                            ?: details.sprites.frontDefault
                            ?: ""

                        val types = details.types.sortedBy { it.slot }
                            .map { it.type.name.replaceFirstChar { c -> c.uppercase() } }

                        val hpStat = details.stats.find { it.stat.name == "hp" }?.baseStat ?: 0
                        val attackStat = details.stats.find { it.stat.name == "attack" }?.baseStat ?: 0
                        val defenseStat = details.stats.find { it.stat.name == "defense" }?.baseStat ?: 0
                        val spAtkStat = details.stats.find { it.stat.name == "sp-atk" }?.baseStat ?: 0
                        val spDefStat = details.stats.find { it.stat.name == "sp-def" }?.baseStat ?: 0
                        val speedStat = details.stats.find { it.stat.name == "speed" }?.baseStat ?: 0

                        val gamePokemon = GamePokemon(
                            pokemonId = details.id,
                            name = result.name.replaceFirstChar { c -> c.uppercase() },
                            imageUrl = imageUrl,
                            types = types,
                            hp = hpStat,
                            attack = attackStat,
                            defense = defenseStat,
                            spAtk = spAtkStat,
                            spDef = spDefStat,
                            speed = speedStat
                        )
                        pokemonDataList.add(gamePokemon)
                    } catch (e: Exception) {
                        // Skip pokemon that fail to load
                    }
                    _loadingProgress.value = (index + 1).toFloat() / total.toFloat()
                }

                _pokemonList.value = pokemonDataList
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load Pokemon: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ---- HOST FLOW ----

    /**
     * Start a new game as the host.
     * Generates the board, runs shuffle animation, then starts BT server.
     */
    fun startNewGame() {
        viewModelScope.launch {
            // Clear any previous board so navigation doesn't trigger early
            _gameState.value = GameState()
            _isShuffling.value = true

            val allPokemon = _pokemonList.value
            // Shuffling animation
            repeat(20) {
                _shuffleDisplayPokemon.value = allPokemon.random()
                delay(100)
            }

            // Generate the actual board
            val board = gameManager.generateGameBoard(allPokemon)
            val myPokemon = board.random()

            // Slow down reveal
            repeat(8) {
                _shuffleDisplayPokemon.value = board.random()
                delay(150 + (it * 50L))
            }

            // Reveal your assigned pokemon
            _shuffleDisplayPokemon.value = myPokemon
            delay(800)

            _isShuffling.value = false
            _shuffleDisplayPokemon.value = null

            _gameState.value = GameState(
                board = board,
                myPokemon = myPokemon,
                showEliminated = true,
                cardSizeDp = 120f,
                isHost = true
            )
            saveGameState()

            // Start Bluetooth server and wait for opponent
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

            val board = boardData.pokemonIds.mapNotNull { id ->
                pokemonMap[id]?.copy(isEliminated = false)
            }

            if (board.size < 2) return false

            viewModelScope.launch {
                _isShuffling.value = true

                repeat(15) {
                    _shuffleDisplayPokemon.value = allPokemon.random()
                    delay(100)
                }

                // Pick a pokemon that isn't the host's pokemon
                val available = if (boardData.hostPokemonId > 0) {
                    board.filter { it.pokemonId != boardData.hostPokemonId }
                } else {
                    board
                }
                val myPokemon = available.random()

                repeat(6) {
                    _shuffleDisplayPokemon.value = board.random()
                    delay(150 + (it * 50L))
                }

                _shuffleDisplayPokemon.value = myPokemon
                delay(800)

                _isShuffling.value = false
                _shuffleDisplayPokemon.value = null

                _gameState.value = GameState(
                    board = board,
                    myPokemon = myPokemon,
                    showEliminated = true,
                    cardSizeDp = 120f,
                    isHost = false
                )
                saveGameState()
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
        saveGameState()
    }

    fun setCardSize(size: Float) {
        _gameState.value = _gameState.value.copy(cardSizeDp = size)
    }

    fun toggleShowEliminated() {
        _gameState.value = _gameState.value.copy(
            showEliminated = !_gameState.value.showEliminated
        )
        saveGameState()
    }

    fun resetLobby() {
        bluetoothManager.disconnect()
        _lobbyState.value = LobbyState.IDLE
    }

    // ---- GAME PERSISTENCE ----

    fun hasSavedGame(): Boolean {
        return prefs.contains("saved_game")
    }

    private fun saveGameState() {
        val state = _gameState.value
        if (state.board.isEmpty() || state.myPokemon == null) return

        val savedData = SavedGameData(
            boardPokemonIds = state.board.map { it.pokemonId },
            eliminatedIds = state.board.filter { it.isEliminated }.map { it.pokemonId },
            myPokemonId = state.myPokemon.pokemonId,
            showEliminated = state.showEliminated,
            cardSizeDp = state.cardSizeDp,
            isHost = state.isHost
        )
        prefs.edit().putString("saved_game", gson.toJson(savedData)).apply()
    }

    fun restoreSavedGame(): Boolean {
        val json = prefs.getString("saved_game", null) ?: return false
        val allPokemon = _pokemonList.value
        if (allPokemon.isEmpty()) return false

        return try {
            val savedData = gson.fromJson(json, SavedGameData::class.java)
            val pokemonMap = allPokemon.associateBy { it.pokemonId }
            val eliminatedSet = savedData.eliminatedIds.toSet()

            val board = savedData.boardPokemonIds.mapNotNull { id ->
                pokemonMap[id]?.copy(isEliminated = id in eliminatedSet)
            }
            val myPokemon = pokemonMap[savedData.myPokemonId]

            if (board.size < 2 || myPokemon == null) return false

            _gameState.value = GameState(
                board = board,
                myPokemon = myPokemon,
                showEliminated = savedData.showEliminated,
                cardSizeDp = savedData.cardSizeDp,
                isHost = savedData.isHost
            )
            _lobbyState.value = LobbyState.CONNECTED
            true
        } catch (e: Exception) {
            false
        }
    }

    fun endGame() {
        prefs.edit().remove("saved_game").apply()
        bluetoothManager.disconnect()
        _gameState.value = GameState()
        _lobbyState.value = LobbyState.IDLE
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.cleanup()
    }
}
