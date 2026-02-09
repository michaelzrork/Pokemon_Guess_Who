package com.example.pokemonguesswho.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val gameManager = GameManager()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header with game controls
        GameHeaderUpdated(
            onToggleEliminated = { viewModel.toggleShowEliminated() },
            showEliminated = gameState.showEliminated,
            onShareBoard = {
                val json = viewModel.exportBoardAsJson()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Pokemon Board", json))
                Toast.makeText(context, "Game code copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            gridColumns = gameState.gridColumns,
            onZoomIn = {
                val newCol = (gameState.gridColumns - 1).coerceIn(1, 6)
                viewModel.setGridColumns(newCol)
            },
            onZoomOut = {
                val newCol = (gameState.gridColumns + 1).coerceIn(1, 6)
                viewModel.setGridColumns(newCol)
            }
        )

        // Your assigned Pokemon banner
        gameState.myPokemon?.let { myPoke ->
            MyPokemonBanner(pokemon = myPoke)
        }

        // Main game board
        PokemonGridUpdated(
            pokemon = gameManager.getVisiblePokemon(gameState.board, gameState.showEliminated),
            myPokemon = gameState.myPokemon,
            onCardClick = { pokemon ->
                viewModel.togglePokemonElimination(pokemon)
            },
            gridColumns = gameState.gridColumns,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp)
        )
    }
}

@Composable
fun GameHeaderUpdated(
    onToggleEliminated: () -> Unit,
    showEliminated: Boolean,
    onShareBoard: () -> Unit,
    gridColumns: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
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

            // Control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle eliminated cards
                Button(
                    onClick = onToggleEliminated,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (showEliminated) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle",
                        modifier = Modifier.padding(end = 4.dp),
                        tint = Color.White
                    )
                    Text(if (showEliminated) "Hide X" else "Show X", fontSize = 11.sp)
                }

                // Zoom controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledIconButton(
                        onClick = onZoomOut,
                        enabled = gridColumns < 6,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Zoom Out",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "${gridColumns}col",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    FilledIconButton(
                        onClick = onZoomIn,
                        enabled = gridColumns > 1,
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Zoom In",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Share board button
                Button(
                    onClick = onShareBoard,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Board",
                        modifier = Modifier.padding(end = 4.dp),
                        tint = Color.White
                    )
                    Text("Share", fontSize = 11.sp)
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
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = gridColumns,
        transitionSpec = {
            (fadeIn() + scaleIn(initialScale = 0.95f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.95f))
        },
        modifier = modifier,
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
