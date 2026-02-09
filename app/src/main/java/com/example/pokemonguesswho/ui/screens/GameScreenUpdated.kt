package com.example.pokemonguesswho.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
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
import com.example.pokemonguesswho.ui.components.TypeIcon
import com.example.pokemonguesswho.ui.components.getTypeColor

@Composable
fun GameScreenUpdated(viewModel: PokemonViewModel, onEndGame: () -> Unit = {}) {
    val gameState by viewModel.gameState.collectAsState()
    val lobbyState by viewModel.lobbyState.collectAsState()
    val opponentFoundMessage by viewModel.opponentFoundMessage.collectAsState()
    val isShuffling by viewModel.isShuffling.collectAsState()
    val shuffleDisplayPokemon by viewModel.shuffleDisplayPokemon.collectAsState()
    val gameManager = GameManager()

    // Show loading animation while shuffling (host navigates here immediately)
    if (isShuffling) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF6200EE)),
            contentAlignment = Alignment.Center
        ) {
            ShufflingAnimation(pokemon = shuffleDisplayPokemon)
        }
        return
    }

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

            // Main game board
            PokemonGridUpdated(
                pokemon = gameManager.getVisiblePokemon(gameState.board, gameState.showEliminated),
                myPokemon = gameState.myPokemon,
                onCardClick = { pokemon ->
                    viewModel.togglePokemonElimination(pokemon)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MyPokemonTopBar(
    pokemon: GamePokemon,
    showEliminated: Boolean,
    onToggleEliminated: () -> Unit,
    onEndGame: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF6200EE))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Your Pokemon card — same as board cards, with gold border + label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "YOUR POKEMON",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Box(modifier = Modifier.width(110.dp)) {
                PokemonCardComponent(
                    pokemon = pokemon,
                    onCardClick = { },
                    isSelected = true,
                    compact = false
                )
            }
        }

        // Type glossary + action buttons
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onToggleEliminated,
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (showEliminated)
                            Color.White.copy(alpha = 0.2f)
                        else
                            Color.White.copy(alpha = 0.35f)
                    )
                ) {
                    Icon(
                        imageVector = if (showEliminated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showEliminated) "Hide Eliminated" else "Show Eliminated",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                FilledIconButton(
                    onClick = onEndGame,
                    modifier = Modifier.size(30.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "End Game",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Type glossary — 4x4 grid
            Text(
                text = "TYPE GLOSSARY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                letterSpacing = 1.sp
            )
            // 4 rows of 4 types each
            allPokemonTypes.chunked(4).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowTypes.forEach { type ->
                        TypeGlossaryChip(
                            type = type,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

private val allPokemonTypes = listOf(
    "Normal", "Fire", "Water", "Electric", "Grass", "Ice",
    "Fighting", "Poison", "Ground", "Flying", "Psychic", "Bug",
    "Rock", "Ghost", "Dragon", "Fairy"
)

@Composable
private fun TypeGlossaryChip(type: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TypeIcon(type = type, size = 12.dp)
        Text(
            text = type,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1
        )
    }
}

@Composable
fun PokemonGridUpdated(
    pokemon: List<GamePokemon>,
    myPokemon: GamePokemon?,
    onCardClick: (GamePokemon) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        modifier = modifier,
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

@Composable
private fun StatChip(label: String, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = value.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}
