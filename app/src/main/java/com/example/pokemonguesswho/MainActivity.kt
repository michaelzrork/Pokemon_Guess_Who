package com.example.pokemonguesswho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.ui.AppNavigation
import com.example.pokemonguesswho.ui.AppTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var viewModel: PokemonViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(PokemonViewModel::class.java)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}
