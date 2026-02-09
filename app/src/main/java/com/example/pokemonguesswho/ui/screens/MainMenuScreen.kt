package com.example.pokemonguesswho.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pokemonguesswho.data.GamePokemon
import com.example.pokemonguesswho.data.PokemonViewModel

@Composable
fun MainMenuScreen(
    viewModel: PokemonViewModel,
    onStartGame: () -> Unit,
    onJoinGame: () -> Unit,
    onResumeGame: () -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val isShuffling by viewModel.isShuffling.collectAsState()
    val shuffleDisplayPokemon by viewModel.shuffleDisplayPokemon.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasSavedGame = viewModel.hasSavedGame()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF6200EE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading -> {
                    // Loading state with progress
                    Text(
                        text = "Loading Pokemon...",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val animatedProgress by animateFloatAsState(
                        targetValue = loadingProgress,
                        animationSpec = tween(300),
                        label = "progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(horizontal = 32.dp),
                        color = Color(0xFFFFEB3B),
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(loadingProgress * 151).toInt()} / 151",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                isShuffling -> {
                    // Shuffling animation
                    ShufflingAnimation(pokemon = shuffleDisplayPokemon)
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        fontSize = 16.sp,
                        color = Color.Red.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    Box(modifier = Modifier.size(16.dp))

                    // Resume Game Button (only shown if there's a saved game)
                    if (hasSavedGame) {
                        Button(
                            onClick = onResumeGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFEB3B)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Resume Game",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                color = Color.Black
                            )
                        }
                    }

                    // Start a Game Button
                    Button(
                        onClick = onStartGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3700B3)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Start a Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            color = Color.White
                        )
                    }

                    // Join a Game Button
                    Button(
                        onClick = onJoinGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF03DAC5)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Join a Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShufflingAnimation(pokemon: GamePokemon?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Loading...",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFFEB3B)
        )

        Spacer(modifier = Modifier.height(24.dp))

        pokemon?.let { poke ->
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(80)) + scaleIn(initialScale = 0.8f, animationSpec = tween(80)),
                exit = fadeOut(animationSpec = tween(80)) + scaleOut(targetScale = 1.2f, animationSpec = tween(80))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = poke.imageUrl,
                        contentDescription = poke.name,
                        modifier = Modifier
                            .size(180.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = poke.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        } ?: CircularProgressIndicator(color = Color(0xFFFFEB3B))
    }
}
