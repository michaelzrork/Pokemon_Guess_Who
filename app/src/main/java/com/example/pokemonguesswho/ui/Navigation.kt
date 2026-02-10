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
    val isLoading by viewModel.isLoading.collectAsState()

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
            // Prevent back gesture from sending the app to background, which can
            // cause a blank-screen bug when returning from the launcher.
            BackHandler { /* consume — MainMenu is the root screen */ }

            MainMenuScreen(
                viewModel = viewModel,
                onStartGame = {
                    viewModel.startNewGame()
                    // Navigate immediately — GameScreenUpdated shows loading
                    // animation while isShuffling is true (no menu flash)
                    navController.navigate(Screen.Game.route) {
                        launchSingleTop = true
                    }
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
            // After process death, NavController restores the Game route but the
            // ViewModel is fresh (empty board). Wait for Pokemon data to load,
            // then try to restore from SharedPreferences; if that fails, go back
            // to the main menu.
            LaunchedEffect(gameState.board.isEmpty(), isShuffling, isLoading) {
                if (gameState.board.isEmpty() && !isShuffling && !isLoading) {
                    if (!viewModel.restoreSavedGame()) {
                        navController.popBackStack(Screen.MainMenu.route, inclusive = false)
                    }
                }
            }

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
