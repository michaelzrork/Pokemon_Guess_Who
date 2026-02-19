package com.example.pokemonguesswho.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pokemonguesswho.data.GamePokemon
import com.example.pokemonguesswho.ui.CustomColor

@Composable
fun PokemonCardComponent(
    pokemon: GamePokemon,
    onCardClick: (GamePokemon) -> Unit,
    isSelected: Boolean = false,
    compact: Boolean = false,
    faceDown: Boolean = false
) {
    val primaryType = pokemon.types.firstOrNull() ?: "Normal"
    val typeColor = getTypeColor(primaryType)
    val borderColor = if (isSelected) CustomColor.gold else typeColor
    val borderWidth = if (isSelected) 3.dp else 2.dp

    // Two independent flip axes:
    // 1. "reveal" flip: faceDown=true → 180°, faceDown=false → 0° (initial board reveal)
    // 2. "elimination" flip: isEliminated → 180° (gameplay toggle)
    // When faceDown, show the back. When revealed and eliminated, also show the back.
    val targetRotation = when {
        faceDown -> 180f
        pokemon.isEliminated -> 180f
        else -> 0f
    }

    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    // Determine which side is showing
    val isFrontVisible = rotation < 90f

    Card(
        modifier = Modifier
            .aspectRatio(0.65f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onCardClick(pokemon) },
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = if (isFrontVisible) borderWidth else 4.dp,
            color = if (isFrontVisible) borderColor else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (isFrontVisible) {
            // ===== FRONT SIDE =====
            CardFrontContent(pokemon, typeColor, compact)
        } else {
            // ===== BACK SIDE (mirrored so it reads correctly when flipped) =====
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f }
            ) {
                CardBackContent()
            }
        }
    }
}

@Composable
private fun CardFrontContent(
    pokemon: GamePokemon,
    typeColor: Color,
    compact: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val artTintAlpha = if (isDark) 0.35f else 0.08f
    val subtleTintAlpha = if (isDark) 0.35f else 0.12f

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
                color = CustomColor.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${pokemon.hp} HP",
                fontSize = if (compact) 8.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                color = CustomColor.white.copy(alpha = 0.9f)
            )
        }

        // -- ART AREA --
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(typeColor.copy(alpha = artTintAlpha)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = pokemon.imageUrl,
                contentDescription = pokemon.name,
                modifier = Modifier.fillMaxSize(0.85f),
                contentScale = ContentScale.Fit
            )
        }

        // -- TYPE ICONS + EVOLUTION STAGE ROW --
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
                    size = if (compact) 16.dp else 24.dp
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = pokemon.evolutionStage,
                fontSize = if (compact) 7.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                color = getEvolutionStageColor(pokemon.evolutionStage),
                modifier = Modifier
                    .background(
                        getEvolutionStageColor(pokemon.evolutionStage).copy(alpha = subtleTintAlpha),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        // -- STATS BAR (hidden in compact mode) --
        if (!compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(typeColor.copy(alpha = subtleTintAlpha))
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

@Composable
private fun CardBackContent() {
    // Official Pokemon card back colors
    val pokemonCardBlue = Color(0xFF1A3A6B)
    val pokemonCardBlueDark = Color(0xFF0F2444)
    val pokemonCardBlueLight = Color(0xFF2B5EA6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(pokemonCardBlueLight, pokemonCardBlue, pokemonCardBlueDark),
                    radius = 600f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Textured pattern overlay - subtle diagonal lines
        Canvas(modifier = Modifier.fillMaxSize()) {
            val lineSpacing = 8.dp.toPx()
            val lineColor = Color.White.copy(alpha = 0.04f)
            val strokeW = 1.dp.toPx()

            // Diagonal lines going one way
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x + size.height, size.height),
                    strokeWidth = strokeW
                )
                x += lineSpacing
            }
            // Diagonal lines going the other way
            x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = strokeW
                )
                x += lineSpacing
            }
        }

        // Inner decorative border
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val borderColor = Color.White.copy(alpha = 0.15f)
            val rect = Rect(0f, 0f, size.width, size.height)
            val cornerRadius = 4.dp.toPx()

            // Draw inner rounded rectangle border
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(rect.left, rect.top),
                size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Pokeball - drawn via Canvas, matching the app icon style
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.55f)
                .aspectRatio(1f)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension / 2f

            val pokeRed = Color(0xFFFD0000)
            val pokeWhite = Color(0xFFFFFFFF)
            val pokeBlack = Color(0xFF1A1A1A)
            val pokeGrayLight = Color(0xFFE8E8E8)
            val stripeHeight = radius * 0.13f
            val buttonOuterR = radius * 0.28f
            val buttonInnerR = radius * 0.18f

            // Shadow behind pokeball
            drawCircle(
                color = Color.Black.copy(alpha = 0.3f),
                radius = radius * 1.02f,
                center = Offset(cx + 2.dp.toPx(), cy + 2.dp.toPx())
            )

            // Black outline circle
            drawCircle(
                color = pokeBlack,
                radius = radius,
                center = Offset(cx, cy)
            )

            // Red top semicircle
            val topHalf = Path().apply {
                addArc(
                    Rect(cx - radius * 0.92f, cy - radius * 0.92f, cx + radius * 0.92f, cy + radius * 0.92f),
                    180f, 180f
                )
                close()
            }
            drawPath(topHalf, pokeRed)

            // Subtle gradient highlight on top half
            val topHighlight = Path().apply {
                addArc(
                    Rect(cx - radius * 0.92f, cy - radius * 0.92f, cx + radius * 0.92f, cy + radius * 0.92f),
                    180f, 180f
                )
                close()
            }
            drawPath(
                topHighlight,
                Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
                    startY = cy - radius * 0.92f,
                    endY = cy
                )
            )

            // White bottom semicircle
            val bottomHalf = Path().apply {
                addArc(
                    Rect(cx - radius * 0.92f, cy - radius * 0.92f, cx + radius * 0.92f, cy + radius * 0.92f),
                    0f, 180f
                )
                close()
            }
            drawPath(bottomHalf, pokeWhite)

            // Subtle shadow on bottom half
            drawPath(
                bottomHalf,
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.08f)),
                    startY = cy,
                    endY = cy + radius * 0.92f
                )
            )

            // Center stripe (black band)
            drawRect(
                color = pokeBlack,
                topLeft = Offset(cx - radius, cy - stripeHeight),
                size = androidx.compose.ui.geometry.Size(radius * 2, stripeHeight * 2)
            )

            // Center button - outer black ring
            drawCircle(
                color = pokeBlack,
                radius = buttonOuterR,
                center = Offset(cx, cy)
            )

            // Center button - white fill
            drawCircle(
                color = pokeWhite,
                radius = buttonInnerR,
                center = Offset(cx, cy)
            )

            // Center button - subtle inner shadow ring
            drawCircle(
                color = pokeGrayLight,
                radius = buttonInnerR,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Glossy highlight on button
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = buttonInnerR * 0.35f,
                center = Offset(cx - buttonInnerR * 0.2f, cy - buttonInnerR * 0.2f)
            )
        }
    }
}

@Composable
private fun CompactStat(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-2).dp)
    ) {
        Text(
            text = label,
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            lineHeight = 8.sp
        )
        Text(
            text = value.toString(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 10.sp
        )
    }
}

/**
 * Draws a type icon as a colored circle with a simple symbol drawn via Canvas.
 * No external assets needed.
 */
@Composable
fun TypeIcon(type: String, size: Dp = 18.dp, alpha: Float = 1f) {
    val typeColor = getTypeColor(type)
    val symbol = getTypeSymbol(type)

    val white = CustomColor.white

    Box(
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size).graphicsLayer { this.alpha = alpha }) {
            // Filled circle background
            drawCircle(
                color = typeColor,
                radius = this.size.minDimension / 2f
            )
            // Thin border
            drawCircle(
                color = white.copy(alpha = 0.4f),
                radius = this.size.minDimension / 2f,
                style = Stroke(width = 1.dp.toPx())
            )
            // Draw the type symbol
            drawTypeSymbol(symbol, typeColor, white)
        }
    }
}

private fun DrawScope.drawTypeSymbol(symbol: TypeSymbol, typeColor: Color, white: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f
    val symbolColor = white.copy(alpha = 0.9f)

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

@Composable
fun getTypeColor(type: String): Color {
    return when (type.lowercase()) {
        "normal" -> CustomColor.typeNormal
        "fire" -> CustomColor.typeFire
        "water" -> CustomColor.typeWater
        "electric" -> CustomColor.typeElectric
        "grass" -> CustomColor.typeGrass
        "ice" -> CustomColor.typeIce
        "fighting" -> CustomColor.typeFighting
        "poison" -> CustomColor.typePoison
        "ground" -> CustomColor.typeGround
        "flying" -> CustomColor.typeFlying
        "psychic" -> CustomColor.typePsychic
        "bug" -> CustomColor.typeBug
        "rock" -> CustomColor.typeRock
        "ghost" -> CustomColor.typeGhost
        "dragon" -> CustomColor.typeDragon
        "dark" -> CustomColor.typeDark
        "steel" -> CustomColor.typeSteel
        "fairy" -> CustomColor.typeFairy
        else -> CustomColor.typeUnknown
    }
}

@Composable
fun getEvolutionStageColor(stage: String): Color {
    return when (stage) {
        "Basic" -> CustomColor.typeGrass
        "Stage 1" -> CustomColor.typeWater
        "Stage 2" -> CustomColor.typeFire
        "Legendary" -> CustomColor.gold
        else -> CustomColor.typeNormal
    }
}
