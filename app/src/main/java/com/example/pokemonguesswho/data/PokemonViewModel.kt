package com.example.pokemonguesswho.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.game.GameState
import com.example.pokemonguesswho.network.PokemonApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PokemonViewModel : ViewModel() {
    
    private val gameManager = GameManager()
    
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    private val _pokemonList = MutableStateFlow<List<GamePokemon>>(emptyList())
    val pokemonList: StateFlow<List<GamePokemon>> = _pokemonList.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadPokemon()
    }
    
    private fun loadPokemon() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = PokemonApiClient.pokemonService.getPokemonList(limit = 151)
                val pokemonDataList = mutableListOf<GamePokemon>()
                
                response.results.forEach { result ->
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
        val board = gameManager.generateGameBoard(_pokemonList.value)
        _gameState.value = GameState(
            board = board,
            selectedPokemon = null,
            showEliminated = true,
            gridColumns = 3
        )
    }
    
    fun selectPokemon(pokemon: GamePokemon) {
        _gameState.value = _gameState.value.copy(selectedPokemon = pokemon)
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
