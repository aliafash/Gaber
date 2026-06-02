package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SlateColorScheme = darkColorScheme(
    primary = SlatePrimary,
    secondary = SlateSecondary,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val GoldColorScheme = darkColorScheme(
    primary = GoldPrimary,
    secondary = GoldSecondary,
    background = GoldBackground,
    surface = GoldSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldSecondary,
    background = EmeraldBackground,
    surface = EmeraldSurface,
    onPrimary = Color(0xFF022C22),
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun PortalTheme(
    themeName: String = "GOLD",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.uppercase()) {
        "SLATE" -> SlateColorScheme
        "EMERALD" -> EmeraldColorScheme
        else -> GoldColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

fun getActiveFontColor(name: String): Color {
    return when (name.uppercase()) {
        "WHITE" -> FontWhite
        "GOLD" -> FontGold
        "SILVER" -> FontSilver
        else -> FontGold
    }
}
