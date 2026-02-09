package com.example.pokemonguesswho.ui

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
    val navigateToGame by viewModel.navigateToGame.collectAsState()

    // Host flow: one-shot navigation event fires after board is set
    // (isShuffling stays true so MainMenuScreen keeps showing "Loading..." — no flash)
    LaunchedEffect(navigateToGame) {
        if (navigateToGame) {
            navController.navigate(Screen.Game.route) {
                launchSingleTop = true
            }
            viewModel.onNavigatedToGame()
        }
    }

    // Client flow: navigate from lobby → game when connected and board ready
    LaunchedEffect(lobbyState, gameState.board.size, isShuffling) {
        val currentRoute = navController.currentDestination?.route
        if (lobbyState == LobbyState.CONNECTED && gameState.board.isNotEmpty() && !isShuffling && currentRoute == Screen.ClientLobby.route) {
            navController.navigate(Screen.Game.route) {
                popUpTo(Screen.MainMenu.route) { inclusive = false }
                launchSingleTop = true
            }
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
