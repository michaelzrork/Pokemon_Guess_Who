package com.example.pokemonguesswho.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pokemonguesswho.data.GamePokemon
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.ui.components.PokemonCardComponent

@Composable
fun GameScreenUpdated(viewModel: PokemonViewModel) {
    val gameState by viewModel.gameState.collectAsState()
    val pokemonList by viewModel.pokemonList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val gameManager = GameManager()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading Pokemon...", style = MaterialTheme.typography.headlineSmall)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: ${errorMessage}", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (gameState.board.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { viewModel.startNewGame() }) {
                    Text("Start New Game")
                }
            }
        } else {
            // Header with game controls
            GameHeaderUpdated(
                onToggleEliminated = { viewModel.toggleShowEliminated() },
                showEliminated = gameState.showEliminated
            )

            // Your assigned Pokemon banner
            gameState.myPokemon?.let { myPoke ->
                MyPokemonBanner(pokemon = myPoke)
            }

            // Main game board with pinch-to-zoom
            PokemonGridUpdated(
                pokemon = gameManager.getVisiblePokemon(gameState.board, gameState.showEliminated),
                myPokemon = gameState.myPokemon,
                onCardClick = { pokemon ->
                    viewModel.togglePokemonElimination(pokemon)
                },
                gridColumns = gameState.gridColumns,
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun GameHeaderUpdated(
    onToggleEliminated: () -> Unit,
    showEliminated: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = "Pokemon Guess Who",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Control buttons
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle eliminated cards
                Button(
                    onClick = onToggleEliminated,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (showEliminated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        modifier = Modifier.padding(end = 4.dp),
                        tint = Color.White
                    )
                    Text(if (showEliminated) "Hide X" else "Show X", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MyPokemonBanner(pokemon: GamePokemon) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEB3B))
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = pokemon.imageUrl,
                contentDescription = pokemon.name,
                modifier = Modifier
                    .height(60.dp)
                    .padding(end = 12.dp),
                contentScale = ContentScale.Fit
            )
            Column {
                Text(
                    text = "Your Pokemon",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = pokemon.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PokemonGridUpdated(
    pokemon: List<GamePokemon>,
    myPokemon: GamePokemon?,
    onCardClick: (GamePokemon) -> Unit,
    gridColumns: Int,
    onGridColumnsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use mutable state so the gesture lambda always reads the latest value
    var currentColumns by remember { mutableIntStateOf(gridColumns) }
    currentColumns = gridColumns
    var cumulativeScale by remember { mutableFloatStateOf(1f) }

    AnimatedContent(
        targetState = gridColumns,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.95f))
        },
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    cumulativeScale *= zoom
                    // Pinch out (spread fingers = zoom in) → fewer columns
                    if (cumulativeScale > 1.3f) {
                        val newColumns = (currentColumns - 1).coerceIn(1, 6)
                        if (newColumns != currentColumns) {
                            onGridColumnsChange(newColumns)
                            currentColumns = newColumns
                        }
                        cumulativeScale = 1f
                    }
                    // Pinch in (squeeze fingers = zoom out) → more columns
                    else if (cumulativeScale < 0.75f) {
                        val newColumns = (currentColumns + 1).coerceIn(1, 6)
                        if (newColumns != currentColumns) {
                            onGridColumnsChange(newColumns)
                            currentColumns = newColumns
                        }
                        cumulativeScale = 1f
                    }
                }
            },
        label = "GridLayoutTransition"
    ) { columns ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                count = pokemon.size,
                key = { pokemon[it].pokemonId }
            ) { index ->
                val poke = pokemon[index]
                Box(
                    modifier = Modifier
                        .clickable {
                            onCardClick(poke)
                        }
                ) {
                    PokemonCardComponent(
                        pokemon = poke,
                        onCardClick = { onCardClick(it) },
                        isSelected = myPokemon?.pokemonId == poke.pokemonId
                    )
                }
            }
        }
    }
}
