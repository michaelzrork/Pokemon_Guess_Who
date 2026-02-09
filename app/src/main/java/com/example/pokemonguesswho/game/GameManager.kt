package com.example.pokemonguesswho.game

import com.example.pokemonguesswho.data.GamePokemon
import kotlin.random.Random

data class GameState(
    val board: List<GamePokemon> = emptyList(),
    val selectedPokemon: GamePokemon? = null,
    val showEliminated: Boolean = true,
    val gridColumns: Int = 3,
    val gameId: String = "",
    val playerId: String = "",
    val opponentId: String? = null,
    val isHost: Boolean = true
)

class GameManager {
    
    fun generateGameBoard(pokemonList: List<GamePokemon>, boardSize: Int = 24): List<GamePokemon> {
        val shuffled = pokemonList.toMutableList().apply { shuffle() }
        val availablePokemon = shuffled.take(boardSize)
        return availablePokemon.map { it.copy(isEliminated = false) }
    }
    
    fun togglePokemonElimination(pokemon: GamePokemon): GamePokemon {
        return pokemon.copy(isEliminated = !pokemon.isEliminated)
    }
    
    fun getVisiblePokemon(board: List<GamePokemon>, showEliminated: Boolean): List<GamePokemon> {
        return if (showEliminated) {
            board
        } else {
            board.filter { !it.isEliminated }
        }
    }
    
    fun selectPokemon(pokemon: GamePokemon): GamePokemon {
        return pokemon.copy()
    }
    
    fun getGridColumns(totalCards: Int): List<Int> {
        // Available column options based on card count
        val options = mutableListOf(1)
        
        if (totalCards >= 2) options.add(2)
        if (totalCards >= 3) options.add(3)
        if (totalCards >= 4) options.add(4)
        if (totalCards >= 6) options.add(6)
        
        return options
    }
}

class GameSessionManager {
    private val sessions = mutableMapOf<String, GameSessionData>()
    
    fun createSession(playerId: String, isHost: Boolean): String {
        val sessionId = generateSessionId()
        sessions[sessionId] = GameSessionData(
            sessionId = sessionId,
            hostId = if (isHost) playerId else "",
            guestId = if (!isHost) playerId else "",
            isActive = true
        )
        return sessionId
    }
    
    fun joinSession(sessionId: String, playerId: String) {
        sessions[sessionId]?.let {
            it.guestId = playerId
        }
    }
    
    fun getSession(sessionId: String): GameSessionData? {
        return sessions[sessionId]
    }
    
    fun closeSession(sessionId: String) {
        sessions[sessionId]?.isActive = false
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${Random.nextInt(10000)}"
    }
}

data class GameSessionData(
    val sessionId: String,
    var hostId: String,
    var guestId: String,
    var isActive: Boolean
)
