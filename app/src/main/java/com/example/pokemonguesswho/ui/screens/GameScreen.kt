package com.example.pokemonguesswho.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
fun GameScreen(viewModel: PokemonViewModel) {
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
            GameHeader(
                onGridColumnsChange = { viewModel.setGridColumns(it) },
                onToggleEliminated = { viewModel.toggleShowEliminated() },
                showEliminated = gameState.showEliminated,
                currentColumns = gameState.gridColumns,
                totalCards = gameState.board.size
            )
            
            // Selected Pokemon section
            if (gameState.selectedPokemon != null) {
                SelectedPokemonSection(
                    pokemon = gameState.selectedPokemon!!,
                    onDeselect = { viewModel.selectPokemon(GamePokemon(
                        pokemonId = 0,
                        name = "",
                        imageUrl = "",
                        types = emptyList()
                    )) }
                )
            }
            
            // Main game board
            PokemonGrid(
                pokemon = gameManager.getVisiblePokemon(gameState.board, gameState.showEliminated),
                selectedPokemon = gameState.selectedPokemon,
                onCardClick = { pokemon ->
                    viewModel.togglePokemonElimination(pokemon)
                },
                onCardSelect = { pokemon -> viewModel.selectPokemon(pokemon) },
                gridColumns = gameState.gridColumns,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun GameHeader(
    onGridColumnsChange: (Int) -> Unit,
    onToggleEliminated: () -> Unit,
    showEliminated: Boolean,
    currentColumns: Int,
    totalCards: Int
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            // Control buttons
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle eliminated cards
                Button(
                    onClick = onToggleEliminated,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Icon(
                        imageVector = if (showEliminated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(if (showEliminated) "Hide X" else "Show X", fontSize = 10.sp)
                }
                
                // Grid layout selector
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .weight(2f)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val availableColumns = (1..6).filter { it <= totalCards }
                    availableColumns.forEach { col ->
                        Button(
                            onClick = { onGridColumnsChange(col) },
                            modifier = Modifier
                                .padding(2.dp)
                                .height(36.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (col == currentColumns) Color(0xFF6200EE) else Color(0xFFBBBBBB)
                            ),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(col.toString(), fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedPokemonSection(
    pokemon: GamePokemon,
    onDeselect: () -> Unit
) {
    if (pokemon.name.isEmpty()) return
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEB3B))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Your Pokemon",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = pokemon.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                AsyncImage(
                    model = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier
                        .height(100.dp)
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            IconButton(
                onClick = onDeselect,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Deselect")
            }
        }
    }
}

@Composable
fun PokemonGrid(
    pokemon: List<GamePokemon>,
    selectedPokemon: GamePokemon?,
    onCardClick: (GamePokemon) -> Unit,
    onCardSelect: (GamePokemon) -> Unit,
    gridColumns: Int,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = gridColumns,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        modifier = modifier
    ) { columns ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(pokemon) { poke ->
                Box(
                    modifier = Modifier
                        .clickable {
                            onCardSelect(poke)
                            onCardClick(poke)
                        }
                ) {
                    PokemonCardComponent(
                        pokemon = poke,
                        onCardClick = { onCardClick(it) },
                        isSelected = selectedPokemon?.pokemonId == poke.pokemonId
                    )
                }
            }
        }
    }
}
