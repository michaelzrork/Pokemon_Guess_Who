package com.example.pokemonguesswho.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pokemonguesswho.data.GamePokemon

@Composable
fun PokemonCardComponent(
    pokemon: GamePokemon,
    onCardClick: (GamePokemon) -> Unit,
    isSelected: Boolean = false,
    compact: Boolean = false
) {
    val primaryType = pokemon.types.firstOrNull() ?: "Normal"
    val typeColor = getTypeColor(primaryType)
    val borderColor = if (isSelected) Color(0xFFFFD700) else typeColor
    val eliminatedAlpha = if (pokemon.isEliminated) 0.4f else 1f
    val borderWidth = if (isSelected) 3.dp else 2.dp

    Card(
        modifier = Modifier
            .aspectRatio(0.65f)
            .alpha(eliminatedAlpha)
            .clickable { onCardClick(pokemon) },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(width = borderWidth, color = borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // -- HEADER BAR: Name + HP --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(typeColor.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = if (compact) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pokemon.name,
                    fontSize = if (compact) 9.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${pokemon.hp} HP",
                    fontSize = if (compact) 8.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // -- ART AREA --
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        typeColor.copy(alpha = 0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = pokemon.imageUrl,
                    contentDescription = pokemon.name,
                    modifier = Modifier
                        .fillMaxSize(0.85f),
                    contentScale = ContentScale.Fit
                )

                // Elimination overlay
                if (pokemon.isEliminated) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Red.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "X",
                            fontSize = if (compact) 28.sp else 40.sp,
                            color = Color.Red.copy(alpha = 0.8f),
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // -- TYPE ICONS ROW --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pokemon.types.forEach { type ->
                    TypeIcon(
                        type = type,
                        size = if (compact) 14.dp else 18.dp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }

            // -- STATS BAR (hidden in compact mode) --
            if (!compact) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(typeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CompactStat("ATK", pokemon.attack)
                    CompactStat("DEF", pokemon.defense)
                    CompactStat("SPD", pokemon.speed)
                }
            }
        }
    }
}

@Composable
private fun CompactStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 7.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value.toString(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
    }
}

/**
 * Draws a type icon as a colored circle with a simple symbol drawn via Canvas.
 * No external assets needed.
 */
@Composable
fun TypeIcon(type: String, size: Dp = 18.dp) {
    val typeColor = getTypeColor(type)
    val symbol = getTypeSymbol(type)

    Box(
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            // Filled circle background
            drawCircle(
                color = typeColor,
                radius = this.size.minDimension / 2f
            )
            // Thin border
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = this.size.minDimension / 2f,
                style = Stroke(width = 1.dp.toPx())
            )
            // Draw the type symbol
            drawTypeSymbol(symbol, typeColor)
        }
    }
}

private fun DrawScope.drawTypeSymbol(symbol: TypeSymbol, typeColor: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val symbolColor = Color.White.copy(alpha = 0.9f)

    when (symbol) {
        TypeSymbol.FLAME -> {
            // Simple flame shape
            val path = Path().apply {
                moveTo(cx, cy - r * 0.5f)
                cubicTo(cx + r * 0.4f, cy - r * 0.1f, cx + r * 0.3f, cy + r * 0.3f, cx, cy + r * 0.4f)
                cubicTo(cx - r * 0.3f, cy + r * 0.3f, cx - r * 0.4f, cy - r * 0.1f, cx, cy - r * 0.5f)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.DROP -> {
            // Water drop
            val path = Path().apply {
                moveTo(cx, cy - r * 0.45f)
                cubicTo(cx + r * 0.35f, cy + r * 0.0f, cx + r * 0.3f, cy + r * 0.35f, cx, cy + r * 0.4f)
                cubicTo(cx - r * 0.3f, cy + r * 0.35f, cx - r * 0.35f, cy + r * 0.0f, cx, cy - r * 0.45f)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.LEAF -> {
            // Leaf shape
            val path = Path().apply {
                moveTo(cx - r * 0.35f, cy + r * 0.3f)
                cubicTo(cx - r * 0.2f, cy - r * 0.3f, cx + r * 0.2f, cy - r * 0.4f, cx + r * 0.35f, cy - r * 0.3f)
                cubicTo(cx + r * 0.2f, cy + r * 0.1f, cx - r * 0.1f, cy + r * 0.4f, cx - r * 0.35f, cy + r * 0.3f)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.BOLT -> {
            // Lightning bolt
            val path = Path().apply {
                moveTo(cx + r * 0.1f, cy - r * 0.5f)
                lineTo(cx - r * 0.15f, cy - r * 0.05f)
                lineTo(cx + r * 0.05f, cy - r * 0.05f)
                lineTo(cx - r * 0.1f, cy + r * 0.5f)
                lineTo(cx + r * 0.15f, cy + r * 0.05f)
                lineTo(cx - r * 0.05f, cy + r * 0.05f)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.STAR -> {
            // Star shape
            val outerR = r * 0.45f
            val innerR = r * 0.2f
            val path = Path()
            for (i in 0 until 10) {
                val angle = Math.toRadians((i * 36.0 - 90.0)).toFloat()
                val rad = if (i % 2 == 0) outerR else innerR
                val x = cx + rad * kotlin.math.cos(angle)
                val y = cy + rad * kotlin.math.sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.CIRCLE -> {
            // Simple filled circle (for Normal, Fairy, etc.)
            drawCircle(symbolColor, radius = r * 0.3f, center = Offset(cx, cy))
        }
        TypeSymbol.FIST -> {
            // Fighting fist - simplified as a rounded rectangle
            drawCircle(symbolColor, radius = r * 0.35f, center = Offset(cx, cy))
            drawCircle(typeColor, radius = r * 0.15f, center = Offset(cx, cy))
        }
        TypeSymbol.SKULL -> {
            // Poison - circle with dot
            drawCircle(symbolColor, radius = r * 0.35f, center = Offset(cx, cy))
            drawCircle(typeColor, radius = r * 0.12f, center = Offset(cx, cy - r * 0.08f))
        }
        TypeSymbol.TRIANGLE -> {
            // Ground/Rock - triangle
            val path = Path().apply {
                moveTo(cx, cy - r * 0.4f)
                lineTo(cx + r * 0.35f, cy + r * 0.3f)
                lineTo(cx - r * 0.35f, cy + r * 0.3f)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.WING -> {
            // Flying - chevron/wing
            val path = Path().apply {
                moveTo(cx - r * 0.35f, cy + r * 0.15f)
                lineTo(cx, cy - r * 0.3f)
                lineTo(cx + r * 0.35f, cy + r * 0.15f)
                lineTo(cx, cy)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.EYE -> {
            // Psychic - eye shape
            drawCircle(symbolColor, radius = r * 0.3f, center = Offset(cx, cy))
            drawCircle(typeColor, radius = r * 0.15f, center = Offset(cx, cy))
            drawCircle(symbolColor, radius = r * 0.06f, center = Offset(cx, cy))
        }
        TypeSymbol.DIAMOND -> {
            // Ice/Dragon - diamond
            val path = Path().apply {
                moveTo(cx, cy - r * 0.45f)
                lineTo(cx + r * 0.3f, cy)
                lineTo(cx, cy + r * 0.45f)
                lineTo(cx - r * 0.3f, cy)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.GEAR -> {
            // Steel - hexagon
            val path = Path()
            for (i in 0 until 6) {
                val angle = Math.toRadians((i * 60.0 - 30.0)).toFloat()
                val x = cx + r * 0.38f * kotlin.math.cos(angle)
                val y = cy + r * 0.38f * kotlin.math.sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, symbolColor, style = Fill)
        }
        TypeSymbol.CRESCENT -> {
            // Dark - crescent
            drawCircle(symbolColor, radius = r * 0.32f, center = Offset(cx, cy))
            drawCircle(typeColor, radius = r * 0.25f, center = Offset(cx + r * 0.12f, cy - r * 0.1f))
        }
        TypeSymbol.GHOST_SHAPE -> {
            // Ghost - ghost silhouette
            val path = Path().apply {
                moveTo(cx - r * 0.3f, cy + r * 0.35f)
                lineTo(cx - r * 0.3f, cy - r * 0.1f)
                cubicTo(cx - r * 0.3f, cy - r * 0.45f, cx + r * 0.3f, cy - r * 0.45f, cx + r * 0.3f, cy - r * 0.1f)
                lineTo(cx + r * 0.3f, cy + r * 0.35f)
                lineTo(cx + r * 0.15f, cy + r * 0.2f)
                lineTo(cx, cy + r * 0.35f)
                lineTo(cx - r * 0.15f, cy + r * 0.2f)
                close()
            }
            drawPath(path, symbolColor, style = Fill)
        }
    }
}

private enum class TypeSymbol {
    FLAME, DROP, LEAF, BOLT, STAR, CIRCLE, FIST, SKULL,
    TRIANGLE, WING, EYE, DIAMOND, GEAR, CRESCENT, GHOST_SHAPE
}

private fun getTypeSymbol(type: String): TypeSymbol {
    return when (type.lowercase()) {
        "fire" -> TypeSymbol.FLAME
        "water" -> TypeSymbol.DROP
        "grass" -> TypeSymbol.LEAF
        "electric" -> TypeSymbol.BOLT
        "normal" -> TypeSymbol.CIRCLE
        "fighting" -> TypeSymbol.FIST
        "poison" -> TypeSymbol.SKULL
        "ground" -> TypeSymbol.TRIANGLE
        "rock" -> TypeSymbol.TRIANGLE
        "flying" -> TypeSymbol.WING
        "psychic" -> TypeSymbol.EYE
        "bug" -> TypeSymbol.LEAF
        "ghost" -> TypeSymbol.GHOST_SHAPE
        "dragon" -> TypeSymbol.DIAMOND
        "ice" -> TypeSymbol.DIAMOND
        "dark" -> TypeSymbol.CRESCENT
        "steel" -> TypeSymbol.GEAR
        "fairy" -> TypeSymbol.STAR
        else -> TypeSymbol.CIRCLE
    }
}

fun getTypeColor(type: String): Color {
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
