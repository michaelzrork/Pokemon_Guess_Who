package com.example.pokemonguesswho.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pokemonguesswho.data.GamePokemon

@Composable
fun PokemonCardComponent(
    pokemon: GamePokemon,
    onCardClick: (GamePokemon) -> Unit,
    isSelected: Boolean = false
) {
    val eliminatedAlpha = if (pokemon.isEliminated) 0.5f else 1f
    val elevationDp = if (isSelected) 12.dp else 4.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable { onCardClick(pokemon) }
            .alpha(eliminatedAlpha),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevationDp
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFFEB3B) else Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pokemon Name
            Text(
                text = pokemon.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            // Pokemon Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFFF5F5F5), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                
                // Elimination indicator
                AnimatedVisibility(
                    visible = pokemon.isEliminated,
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(
                                color = Color.Red.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 48.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Types
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                pokemon.types.forEach { type ->
                    TypeBadgeComponent(type)
                    Spacer(modifier = Modifier.padding(2.dp))
                }
            }
            
            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBoxComponent("HP", pokemon.hp)
                StatBoxComponent("ATK", pokemon.attack)
                StatBoxComponent("DEF", pokemon.defense)
            }
        }
    }
}

@Composable
fun TypeBadgeComponent(type: String) {
    val typeColor = getTypeColor(type)
    Card(
        modifier = Modifier.padding(2.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = typeColor)
    ) {
        Text(
            text = type,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(4.dp, 2.dp)
        )
    }
}

@Composable
fun StatBoxComponent(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value.toString(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2196F3)
        )
    }
}

fun getTypeColorForBadge(type: String): Color {
    return when (type.lowercase()) {
        "normal" -> Color(0xFFA8A878)
        "fire" -> Color(0xFFF08030)
        "water" -> Color(0xFF6890F0)
        "electric" -> Color(0xFFF8D030)
        "grass" -> Color(0xFF78C850)
        "ice" -> Color(0xFF98D8D8)
        "fighting" -> Color(0xFFC03028)
        "poison" -> Color(0xFFA040A0)
        "ground" -> Color(0xFFE0C068)
        "flying" -> Color(0xFFA890F0)
        "psychic" -> Color(0xFFF85888)
        "bug" -> Color(0xFFA8B820)
        "rock" -> Color(0xFFB8A038)
        "ghost" -> Color(0xFF705898)
        "dragon" -> Color(0xFF7038F8)
        "dark" -> Color(0xFF705848)
        "steel" -> Color(0xFFB8B8D0)
        "fairy" -> Color(0xFFEE99AC)
        else -> Color(0xFF68A090)
    }
}
