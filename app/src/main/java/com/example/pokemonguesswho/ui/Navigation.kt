package com.example.pokemonguesswho.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.ui.screens.GameScreenUpdated
import com.example.pokemonguesswho.ui.screens.MainMenuScreen

sealed class Screen(val route: String) {
    object MainMenu : Screen("main_menu")
    object Game : Screen("game")
    object Multiplayer : Screen("multiplayer")
}

@Composable
fun AppNavigation(viewModel: PokemonViewModel) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.MainMenu.route
    ) {
        composable(Screen.MainMenu.route) {
            MainMenuScreen(
                onStartGame = { navController.navigate(Screen.Game.route) },
                onMultiplayer = { navController.navigate(Screen.Multiplayer.route) }
            )
        }
        composable(Screen.Game.route) {
            GameScreenUpdated(viewModel)
        }
    }
}
