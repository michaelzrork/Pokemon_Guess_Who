package com.example.pokemonguesswho.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pokemonguesswho.data.GamePokemon
import com.example.pokemonguesswho.data.LobbyState
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.ui.components.PokemonCardComponent
import com.example.pokemonguesswho.ui.components.pinchToZoom

@Composable
fun GameScreenUpdated(viewModel: PokemonViewModel, onEndGame: () -> Unit = {}) {
    val gameState by viewModel.gameState.collectAsState()
    val lobbyState by viewModel.lobbyState.collectAsState()
    val opponentFoundMessage by viewModel.opponentFoundMessage.collectAsState()
    val gameManager = GameManager()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            // Waiting for player status bar (host only, before opponent connects)
            if (gameState.isHost && lobbyState == LobbyState.WAITING_FOR_OPPONENT) {
                WaitingForPlayerBar()
            }

            // Top bar: My Pokemon card (compact) + Hide/Show toggle + End Game
            gameState.myPokemon?.let { myPoke ->
                MyPokemonTopBar(
                    pokemon = myPoke,
                    showEliminated = gameState.showEliminated,
                    onToggleEliminated = { viewModel.toggleShowEliminated() },
                    onEndGame = onEndGame
                )
            }

            // Main game board with pinch-to-zoom
            PokemonGridUpdated(
                pokemon = gameManager.getVisiblePokemon(gameState.board, gameState.showEliminated),
                myPokemon = gameState.myPokemon,
                onCardClick = { pokemon ->
                    viewModel.togglePokemonElimination(pokemon)
                },
                cardSizeDp = gameState.cardSizeDp,
                onCardSizeChange = { newSize ->
                    viewModel.setCardSize(newSize)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        // "Player Found!" notification overlay
        AnimatedVisibility(
            visible = opponentFoundMessage != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            OpponentFoundBanner(message = opponentFoundMessage ?: "")
        }
    }
}

@Composable
fun WaitingForPlayerBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6200EE))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            color = Color(0xFFFFEB3B),
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Waiting for opponent to join...",
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OpponentFoundBanner(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun MyPokemonTopBar(
    pokemon: GamePokemon,
    showEliminated: Boolean,
    onToggleEliminated: () -> Unit,
    onEndGame: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Pokemon card
            Box(
                modifier = Modifier.width(70.dp)
            ) {
                PokemonCardComponent(
                    pokemon = pokemon,
                    onCardClick = { },
                    isSelected = true,
                    compact = true
                )
            }

            // Name + stats
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = pokemon.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
                // Stats in compact rows
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatChip("HP", pokemon.hp)
                    StatChip("ATK", pokemon.attack)
                    StatChip("DEF", pokemon.defense)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatChip("SpA", pokemon.spAtk)
                    StatChip("SpD", pokemon.spDef)
                    StatChip("SPD", pokemon.speed)
                }
                // Types
                Text(
                    text = pokemon.types.joinToString(" / "),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hide/Show eliminated toggle button
                FilledIconButton(
                    onClick = onToggleEliminated,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (showEliminated)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = if (showEliminated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showEliminated) "Hide Eliminated" else "Show Eliminated",
                        modifier = Modifier.size(16.dp)
                    )
                }

                // End Game button
                FilledIconButton(
                    onClick = onEndGame,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End Game",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PokemonGridUpdated(
    pokemon: List<GamePokemon>,
    myPokemon: GamePokemon?,
    onCardClick: (GamePokemon) -> Unit,
    cardSizeDp: Float,
    onCardSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .pinchToZoom(
                currentSize = cardSizeDp,
                minSize = 80f,
                maxSize = 200f,
                onSizeChange = onCardSizeChange
            )
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = cardSizeDp.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(
                count = pokemon.size,
                key = { pokemon[it].pokemonId }
            ) { index ->
                val poke = pokemon[index]
                PokemonCardComponent(
                    pokemon = poke,
                    onCardClick = { onCardClick(it) },
                    isSelected = myPokemon?.pokemonId == poke.pokemonId
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value.toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}
