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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pokemonguesswho.data.GamePokemon
import com.example.pokemonguesswho.data.LobbyState
import com.example.pokemonguesswho.data.PokemonViewModel
import com.example.pokemonguesswho.game.GameManager
import com.example.pokemonguesswho.ui.CustomColor
import com.example.pokemonguesswho.ui.components.PokemonCardComponent
import com.example.pokemonguesswho.ui.components.TypeIcon
import com.example.pokemonguesswho.ui.components.getTypeColor

@Composable
fun GameScreenUpdated(
    viewModel: PokemonViewModel,
    onConfirmExit: () -> Unit = {},
    onDismissExit: () -> Unit = {}
) {
    val gameState by viewModel.gameState.collectAsState()
    val lobbyState by viewModel.lobbyState.collectAsState()
    val opponentFoundMessage by viewModel.opponentFoundMessage.collectAsState()
    val isShuffling by viewModel.isShuffling.collectAsState()
    val shuffleDisplayPokemon by viewModel.shuffleDisplayPokemon.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showExitDialog by viewModel.showExitDialog.collectAsState()
    val revealedCardIds by viewModel.revealedCardIds.collectAsState()
    val gameManager = GameManager()

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = onDismissExit,
            title = {
                Text(
                    text = "Leave Game?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you'd like to exit your game?")
            },
            confirmButton = {
                TextButton(onClick = onConfirmExit) {
                    Text(
                        "Yes",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissExit) {
                    Text(
                        "No",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Show loading animation while shuffling (host navigates here immediately)
    if (isShuffling) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            ShufflingAnimation(pokemon = shuffleDisplayPokemon)
        }
        return
    }

    // After process death: NavController restored Game route but data is still loading.
    // Show a loading indicator until the board is restored.
    if (gameState.board.isEmpty() && (isLoading || isShuffling)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.tertiary)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Waiting for player status bar (host only, before opponent connects)
            if (gameState.isHost && lobbyState == LobbyState.WAITING_FOR_OPPONENT) {
                WaitingForPlayerBar()
            }

            // Top bar: My Pokemon card + Hide/Show toggle + Type Glossary
            gameState.myPokemon?.let { myPoke ->
                MyPokemonTopBar(
                    pokemon = myPoke,
                    board = gameState.board
                )
            }

            // Main game board
            PokemonGridUpdated(
                pokemon = gameManager.getVisiblePokemon(gameState.board, gameState.showEliminated),
                myPokemon = gameState.myPokemon,
                revealedCardIds = revealedCardIds,
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
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        Icon(
            imageVector = Icons.Default.Bluetooth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = "Waiting for opponent to join...",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onPrimary,
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
        colors = CardDefaults.cardColors(containerColor = CustomColor.greenSuccess),
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
                tint = CustomColor.white,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = message,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CustomColor.white
            )
        }
    }
}

@Composable
fun MyPokemonTopBar(
    pokemon: GamePokemon,
    board: List<GamePokemon> = emptyList()
) {
    // Compute which types still have at least one non-eliminated card on the board
    val activeTypes = remember(board) {
        board.filter { !it.isEliminated }
            .flatMap { it.types }
            .map { it.lowercase() }
            .toSet()
    }

    // Measure card column height so glossary column can match it
    var cardColumnHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Your Pokemon card
        Box(
            modifier = Modifier
                .width(120.dp)
                .onGloballyPositioned { coordinates ->
                    cardColumnHeightPx = coordinates.size.height
                }
        ) {
            PokemonCardComponent(
                pokemon = pokemon,
                onCardClick = { },
                isSelected = true,
                compact = false
            )
        }

        // Type glossary — match height of card
        val glossaryHeight = with(density) { cardColumnHeightPx.toDp() }

        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (cardColumnHeightPx > 0) Modifier.height(glossaryHeight) else Modifier)
        ) {
            // 4 rows of 4 types each — each row gets exact 1/4 of height
            allPokemonTypes.chunked(4).forEach { rowTypes ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowTypes.forEach { type ->
                        TypeGlossaryChip(
                            type = type,
                            isActive = activeTypes.contains(type.lowercase()),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
private fun TypeGlossaryChip(
    type: String,
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val alpha = if (isActive) 1f else 0.25f
    // Measure available height so the icon can fill remaining space after text
    var chipHeightPx by remember { mutableIntStateOf(0) }
    var textHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val iconSize = with(density) {
        ((chipHeightPx - textHeightPx).coerceAtLeast(0)).toDp().coerceIn(14.dp, 32.dp)
    }

    Column(
        modifier = modifier
            .onGloballyPositioned { chipHeightPx = it.size.height },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        TypeIcon(type = type, size = iconSize, alpha = alpha)
        Text(
            text = type,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (isActive) 0.85f else 0.25f),
            maxLines = 1,
            modifier = Modifier.onGloballyPositioned { textHeightPx = it.size.height }
        )
    }
}

@Composable
fun PokemonGridUpdated(
    pokemon: List<GamePokemon>,
    myPokemon: GamePokemon?,
    revealedCardIds: Set<Int> = emptySet(),
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
            val isRevealed = revealedCardIds.contains(poke.pokemonId)
            PokemonCardComponent(
                pokemon = poke,
                onCardClick = { onCardClick(it) },
                isSelected = myPokemon?.pokemonId == poke.pokemonId,
                faceDown = !isRevealed
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
