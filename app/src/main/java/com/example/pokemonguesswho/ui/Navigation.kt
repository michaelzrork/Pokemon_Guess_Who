package com.example.pokemonguesswho.ui

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
fun AppNavigation(
    viewModel: PokemonViewModel,
    onRequestDiscoverability: ((Boolean) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val gameState by viewModel.gameState.collectAsState()
    val lobbyState by viewModel.lobbyState.collectAsState()

    // Auto-navigate from client lobby to game when connected and board is populated
    LaunchedEffect(lobbyState, gameState.board.size) {
        if (lobbyState == LobbyState.CONNECTED && gameState.board.isNotEmpty()) {
            val currentRoute = navController.currentDestination?.route
            if (currentRoute == Screen.ClientLobby.route) {
                navController.navigate(Screen.Game.route) {
                    popUpTo(Screen.MainMenu.route) { inclusive = false }
                    launchSingleTop = true
                }
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
                    // Request discoverability (one-time prompt per session), then start game
                    onRequestDiscoverability { _ ->
                        viewModel.startNewGame()
                        navController.navigate(Screen.Game.route) {
                            launchSingleTop = true
                        }
                    }
                },
                onJoinGame = {
                    viewModel.startJoinGame()
                    navController.navigate(Screen.ClientLobby.route) {
                        launchSingleTop = true
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
            GameScreenUpdated(viewModel)
        }
    }
}
