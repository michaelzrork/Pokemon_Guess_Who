package com.example.pokemonguesswho.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.game.GameState
import com.example.pokemonguesswho.network.PokemonApiClient
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BoardData(
    val pokemonIds: List<Int>
)

class PokemonViewModel : ViewModel() {

    private val gameManager = GameManager()
    private val gson = Gson()

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

    fun startNewGame() {
        viewModelScope.launch {
            _isShuffling.value = true

            // Run the shuffling animation - rapidly cycle through random pokemon
            val allPokemon = _pokemonList.value
            repeat(20) {
                _shuffleDisplayPokemon.value = allPokemon.random()
                delay(100)
            }

            // Generate the actual board
            val board = gameManager.generateGameBoard(allPokemon)
            val myPokemon = board.random()

            // Show the final picks slowing down
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
                gridColumns = 3
            )
        }
    }

    /**
     * Export the current board as a JSON string that can be shared via text.
     * Contains just the pokemon IDs so the other player can load the same board.
     */
    fun exportBoardAsJson(): String {
        val board = _gameState.value.board
        val boardData = BoardData(pokemonIds = board.map { it.pokemonId })
        return gson.toJson(boardData)
    }

    /**
     * Import a board from a JSON string shared by the host.
     * Looks up each pokemon ID in the cached list to rebuild the full board.
     */
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

                // Shuffling animation for joining player too
                repeat(15) {
                    _shuffleDisplayPokemon.value = allPokemon.random()
                    delay(100)
                }

                val myPokemon = board.random()

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
                    gridColumns = 3
                )
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

    fun setGridColumns(columns: Int) {
        _gameState.value = _gameState.value.copy(gridColumns = columns)
    }

    fun toggleShowEliminated() {
        _gameState.value = _gameState.value.copy(
            showEliminated = !_gameState.value.showEliminated
        )
    }
}
