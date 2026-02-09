package com.example.pokemonguesswho.ui

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pokemonguesswho.data.LobbyState
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.ui.screens.GameScreenUpdated
import com.example.pokemonguesswho.ui.screens.LobbyScreen
import com.example.pokemonguesswho.ui.screens.MainMenuScreen

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object ClientLobby : Screen("client_lobby")
    object Game : Screen("game")
}

@Composable
fun AppNavigation(viewModel: PokemonViewModel) {
    val navController = rememberNavController()
    val gameState by viewModel.gameState.collectAsState()
    val lobbyState by viewModel.lobbyState.collectAsState()
    val isShuffling by viewModel.isShuffling.collectAsState()

    // Auto-navigate to game when board is populated
    LaunchedEffect(lobbyState, gameState.board.size, isShuffling) {
        val currentRoute = navController.currentDestination?.route
        Log.d("GameFlow", "NAV LaunchedEffect: lobbyState=$lobbyState, boardSize=${gameState.board.size}, isHost=${gameState.isHost}, isShuffling=$isShuffling, currentRoute=$currentRoute")

        // Client lobby → game when connected and board ready
        if (lobbyState == LobbyState.CONNECTED && gameState.board.isNotEmpty() && !isShuffling && currentRoute == Screen.ClientLobby.route) {
            Log.d("GameFlow", "NAV: Client lobby → Game (connected + board ready)")
            navController.navigate(Screen.Game.route) {
                popUpTo(Screen.MainMenu.route) { inclusive = false }
                launchSingleTop = true
            }
        }
        // Main menu → game when host board is ready (navigate while still showing loading animation)
        if (gameState.board.isNotEmpty() && gameState.isHost && isShuffling && currentRoute == Screen.MainMenu.route) {
            Log.d("GameFlow", "NAV: Main menu → Game (host board ready, isShuffling still true)")
            navController.navigate(Screen.Game.route) {
                launchSingleTop = true
            }
        } else if (gameState.board.isNotEmpty() && gameState.isHost && currentRoute == Screen.MainMenu.route) {
            Log.d("GameFlow", "NAV: Host board ready but condition NOT met: isShuffling=$isShuffling")
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                viewModel = viewModel,
                onStartGame = {
                    viewModel.startNewGame()
                    // Navigation happens automatically via LaunchedEffect
                    // once the board is populated (after shuffle completes)
                },
                onJoinGame = {
                    viewModel.startJoinGame()
                    navController.navigate(Screen.ClientLobby.route) {
                        launchSingleTop = true
                    }
                },
                onResumeGame = {
                    if (viewModel.restoreSavedGame()) {
                        navController.navigate(Screen.Game.route) {
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
        composable(Screen.ClientLobby.route) {
            LobbyScreen(
                viewModel = viewModel,
                isHost = false,
                onBack = {
                    viewModel.resetLobby()
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Game.route) {
            // Back press goes to main menu without losing game state (it's saved)
            BackHandler {
                navController.popBackStack(Screen.MainMenu.route, inclusive = false)
            }

            GameScreenUpdated(
                viewModel = viewModel,
                onEndGame = {
                    viewModel.endGame()
                    navController.popBackStack(Screen.MainMenu.route, inclusive = false)
                }
            )
        }
    }
}
