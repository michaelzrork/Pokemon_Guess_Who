package com.example.pokemonguesswho.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.example.pokemonguesswho.R

object CustomColor {
    // Pokemon Brand Colors
    val pokemonYellowBright @Composable get() = colorResource(id = R.color.pokemon_yellow_bright) // #FFDE00
    val pokemonYellow @Composable get() = colorResource(id = R.color.pokemon_yellow)              // #FFCB05
    val pokemonYellowShadow @Composable get() = colorResource(id = R.color.pokemon_yellow_shadow) // #C7A008
    val pokemonYellowDark @Composable get() = colorResource(id = R.color.pokemon_yellow_dark)     // #B3A125
    val pokemonBlueLight @Composable get() = colorResource(id = R.color.pokemon_blue_light)       // #2A75bb
    val pokemonBlue @Composable get() = colorResource(id = R.color.pokemon_blue)                  // #3B4CCA
    val pokemonBlueDark @Composable get() = colorResource(id = R.color.pokemon_blue_dark)         // #3C5AA6
    val pokemonRedBright @Composable get() = colorResource(id = R.color.pokemon_red_bright)       // #FF0000
    val pokemonRed @Composable get() = colorResource(id = R.color.pokemon_red)                    // #FD0000
    val pokemonRedDark @Composable get() = colorResource(id = R.color.pokemon_red_dark)           // #CC0000

    // Utility & Legacy Colors
    val purple200 @Composable get() = colorResource(id = R.color.purple_200)      // #FFBB86FC
    val purple500 @Composable get() = colorResource(id = R.color.purple_500)      // #FF6200EE
    val purple700 @Composable get() = colorResource(id = R.color.purple_700)      // #FF3700B3
    val teal200 @Composable get() = colorResource(id = R.color.teal_200)          // #FF03DAC5
    val teal700 @Composable get() = colorResource(id = R.color.teal_700)          // #FF018786
    val black @Composable get() = colorResource(id = R.color.black)               // #FF000000
    val blackFade @Composable get() = Color(0xFF121212)
    val grayDark @Composable get() = Color(0xFF1E1E1E)
    val white @Composable get() = colorResource(id = R.color.white)               // #FFFFFFFF
    val whiteShadow @Composable get() = colorResource(id = R.color.white_shadow)  // #DDDDDD
    val whiteDim @Composable get() = Color(0xFFFAFAFA)
    val redLight @Composable get() = Color(0xFFCF6679)
    val redDark @Composable get() = Color(0xFFB00020)

    // UI Accent Colors
    val gold @Composable get() = Color(0xFFFFD700)
    val yellowAccent @Composable get() = Color(0xFFFFEB3B)
    val amber @Composable get() = Color(0xFFFFC107)
    val orange @Composable get() = Color(0xFFFF9800)
    val charcoal @Composable get() = Color(0xFF2D2D2D)
    val pokeballRed @Composable get() = Color(0xFFFF1C1C)
    val grayLight @Composable get() = Color(0xFFF5F5F5)
    val redAccent @Composable get() = Color(0xFFE53935)
    val greenSuccess @Composable get() = Color(0xFF4CAF50)
    val gray @Composable get() = Color(0xFF888888)
    val grayMedium @Composable get() = Color(0xFF444444)

    // Pokemon Type Colors (standard community colors)
    val typeNormal @Composable get() = Color(0xFFA8A77A)
    val typeFire @Composable get() = Color(0xFFEE8130)
    val typeWater @Composable get() = Color(0xFF6390F0)
    val typeElectric @Composable get() = Color(0xFFF7D02C)
    val typeGrass @Composable get() = Color(0xFF7AC74C)
    val typeIce @Composable get() = Color(0xFF96D9D6)
    val typeFighting @Composable get() = Color(0xFFC22E28)
    val typePoison @Composable get() = Color(0xFFA33EA1)
    val typeGround @Composable get() = Color(0xFFE2BF65)
    val typeFlying @Composable get() = Color(0xFFA98FF3)
    val typePsychic @Composable get() = Color(0xFFF95587)
    val typeBug @Composable get() = Color(0xFFA6B91A)
    val typeRock @Composable get() = Color(0xFFB6A136)
    val typeGhost @Composable get() = Color(0xFF735797)
    val typeDragon @Composable get() = Color(0xFF6F35FC)
    val typeDark @Composable get() = Color(0xFF705746)
    val typeSteel @Composable get() = Color(0xFFB7B7CE)
    val typeFairy @Composable get() = Color(0xFFD685AD)
    val typeUnknown @Composable get() = Color(0xFF777777)
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            // Primary: Pokemon Blue — used for main CTA buttons, status bars, prominent icons
            primary = CustomColor.pokemonBlueDark,
            onPrimary = CustomColor.whiteDim,
            primaryContainer = CustomColor.pokemonBlue,
            onPrimaryContainer = CustomColor.white,
            inversePrimary = CustomColor.pokemonBlueLight,
            // Secondary: Pokemon Red — used for "Join a Game", "Scan Again" buttons
            secondary = CustomColor.pokemonRed,
            onSecondary = CustomColor.white,
            secondaryContainer = CustomColor.pokemonRedDark,
            onSecondaryContainer = CustomColor.white,
            // Tertiary: Pokemon Yellow — used for contrasting accents, spinners, loading indicators, "Resume Game"
            tertiary = CustomColor.pokemonYellow,
            onTertiary = CustomColor.black,
            tertiaryContainer = CustomColor.pokemonYellowShadow,
            onTertiaryContainer = CustomColor.black,
            // Surfaces
            background = CustomColor.blackFade,
            onBackground = CustomColor.white,
            surface = CustomColor.grayDark,
            onSurface = CustomColor.white,
            surfaceVariant = Color(0xFF2E2E2E),
            onSurfaceVariant = Color(0xFFAAAAAA),
            inverseSurface = CustomColor.white,
            inverseOnSurface = CustomColor.black,
            surfaceTint = CustomColor.pokemonBlueDark,
            // Error
            error = CustomColor.redLight,
            onError = CustomColor.white,
            errorContainer = CustomColor.redDark,
            onErrorContainer = CustomColor.white,
            // Borders
            outline = Color(0xFF555555),
            outlineVariant = Color(0xFF3A3A3A),
            scrim = CustomColor.black
        )
    } else {
        lightColorScheme(
            // Primary: Pokemon Blue — used for main CTA buttons, status bars, prominent icons
            primary = CustomColor.pokemonBlue,
            onPrimary = CustomColor.white,
            primaryContainer = CustomColor.pokemonBlueLight,
            onPrimaryContainer = CustomColor.white,
            inversePrimary = CustomColor.pokemonYellowDark,
            // Secondary: Pokemon Red — used for "Join a Game", "Scan Again" buttons
            secondary = CustomColor.pokemonRed,
            onSecondary = CustomColor.white,
            secondaryContainer = CustomColor.pokemonRedBright,
            onSecondaryContainer = CustomColor.white,
            // Tertiary: Pokemon Yellow — used for contrasting accents, spinners, loading indicators, "Resume Game"
            tertiary = CustomColor.pokemonYellow,
            onTertiary = CustomColor.black,
            tertiaryContainer = CustomColor.pokemonYellowBright,
            onTertiaryContainer = CustomColor.black,
            // Surfaces
            background = CustomColor.pokemonBlueLight,
            onBackground = CustomColor.whiteDim,
            surface = CustomColor.white,
            onSurface = CustomColor.charcoal,
            surfaceVariant = CustomColor.whiteShadow,
            onSurfaceVariant = CustomColor.gray,
            inverseSurface = CustomColor.blackFade,
            inverseOnSurface = CustomColor.white,
            surfaceTint = CustomColor.pokemonBlue,
            // Error
            error = CustomColor.redDark,
            onError = CustomColor.white,
            errorContainer = CustomColor.redLight,
            onErrorContainer = CustomColor.white,
            // Borders
            outline = CustomColor.whiteShadow,
            outlineVariant = Color(0xFFEEEEEE),
            scrim = CustomColor.black
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
