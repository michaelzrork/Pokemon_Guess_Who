package com.example.pokemonguesswho.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pokemon_prefs")

class PreferencesManager(private val context: Context) {
    companion object {
        private val GAME_STATE_KEY = stringPreferencesKey("game_state")
        private val PLAYER_ID_KEY = stringPreferencesKey("player_id")
    }

    suspend fun saveGameState(state: String) {
        context.dataStore.edit { preferences ->
            preferences[GAME_STATE_KEY] = state
        }
    }

    fun getGameState() = context.dataStore.data.map { preferences ->
        preferences[GAME_STATE_KEY] ?: ""
    }

    suspend fun savePlayerId(playerId: String) {
        context.dataStore.edit { preferences ->
            preferences[PLAYER_ID_KEY] = playerId
        }
    }

    fun getPlayerId() = context.dataStore.data.map { preferences ->
        preferences[PLAYER_ID_KEY] ?: ""
    }
}
