package com.example.pokemonguesswho.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.ui.screens.GameScreenUpdated
import com.example.pokemonguesswho.ui.screens.MainMenuScreen

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Game : Screen("game")
}

@Composable
fun AppNavigation(viewModel: PokemonViewModel) {
    val navController = rememberNavController()
    val gameState by viewModel.gameState.collectAsState()

    // Auto-navigate to game screen when board is populated
    LaunchedEffect(gameState.board.size) {
        if (gameState.board.isNotEmpty()) {
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
                onStartGame = { /* handled by viewModel + LaunchedEffect */ },
                onJoinGame = { /* handled by viewModel + LaunchedEffect */ }
            )
        }
        composable(Screen.Game.route) {
            GameScreenUpdated(viewModel)
        }
    }
}
