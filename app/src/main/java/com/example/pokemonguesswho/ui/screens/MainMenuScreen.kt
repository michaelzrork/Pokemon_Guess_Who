package com.example.pokemonguesswho.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pokemonguesswho.data.GamePokemon
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.ui.CustomColor

@Composable
fun MainMenuScreen(
    viewModel: PokemonViewModel,
    onStartGame: () -> Unit,
    onJoinGame: () -> Unit,
    onResumeGame: () -> Unit = {}
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingProgress by viewModel.loadingProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val hasSavedGame = viewModel.hasSavedGame()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background)
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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
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
                        color = MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${(loadingProgress * 151).toInt()} / 151",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                errorMessage != null -> {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    // Pokeball
                    PokeballIcon(modifier = Modifier.size(120.dp))

                    Spacer(modifier = Modifier.height(20.dp))

                    // Game Logo
                    GameLogo()

                    Spacer(modifier = Modifier.height(32.dp))

                    // Resume Game Button (only shown if there's a saved game)
                    if (hasSavedGame) {
                        Button(
                            onClick = onResumeGame,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Resume Game",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onTertiary
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
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Start a Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    // Join a Game Button
                    Button(
                        onClick = onJoinGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Join a Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSecondary
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
            color = MaterialTheme.colorScheme.onBackground
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
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        } ?: CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun PokeballIcon(modifier: Modifier = Modifier) {
    val charcoalColor = CustomColor.charcoal
    val pokeballRedColor = CustomColor.pokeballRed
    val whiteColor = CustomColor.white

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = w / 2f
        val strokeWidth = w * 0.04f
        val bandHeight = h * 0.06f

        // Outer circle - dark outline
        drawCircle(
            color = charcoalColor,
            radius = radius,
            style = Fill
        )

        // Top half - red
        drawArc(
            color = pokeballRedColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            size = size
        )

        // Bottom half - white
        drawArc(
            color = whiteColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            size = size
        )

        // Center band - dark
        drawLine(
            color = charcoalColor,
            start = Offset(0f, cy),
            end = Offset(w, cy),
            strokeWidth = bandHeight,
            cap = StrokeCap.Butt
        )

        // Outer ring
        drawCircle(
            color = charcoalColor,
            radius = radius,
            style = Stroke(width = strokeWidth)
        )

        // Center button - outer ring
        drawCircle(
            color = charcoalColor,
            radius = radius * 0.22f,
            center = Offset(cx, cy),
            style = Fill
        )

        // Center button - white fill
        drawCircle(
            color = whiteColor,
            radius = radius * 0.15f,
            center = Offset(cx, cy),
            style = Fill
        )

        // Center button - inner ring
        drawCircle(
            color = charcoalColor,
            radius = radius * 0.15f,
            center = Offset(cx, cy),
            style = Stroke(width = strokeWidth * 0.6f)
        )

        // Highlight on top-left for 3D effect
        drawCircle(
            color = whiteColor.copy(alpha = 0.25f),
            radius = radius * 0.35f,
            center = Offset(cx - radius * 0.2f, cy - radius * 0.35f),
            style = Fill
        )
    }
}

@Composable
fun GameLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // "POKEMON" text
        Text(
            text = "POKEMON",
            style = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                color = CustomColor.yellowAccent,
                shadow = Shadow(
                    color = CustomColor.purple700,
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
        // "GUESS WHO" text - bigger, more prominent
        Text(
            text = "GUESS WHO",
            style = TextStyle(
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CustomColor.yellowAccent,
                        CustomColor.amber,
                        CustomColor.orange
                    )
                ),
                shadow = Shadow(
                    color = CustomColor.black.copy(alpha = 0.5f),
                    offset = Offset(3f, 3f),
                    blurRadius = 6f
                )
            )
        )
        // Subtle tagline
        Text(
            text = "Gotta Guess 'Em All!",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            letterSpacing = 2.sp
        )
    }
}
