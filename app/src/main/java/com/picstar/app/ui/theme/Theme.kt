package com.picstar.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.picstar.app.model.ThemeMode

data class PicStarColors(
    val ink: Color,
    val navy: Color,
    val panel: Color,
    val panel2: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val coral: Color,
    val coral2: Color,
    val cream: Color,
    val lime: Color,
    val isLight: Boolean
)

val MidnightPalette = PicStarColors(
    ink = Color(0xFF071423),
    navy = Color(0xFF0B1C30),
    panel = Color(0xFF10243A),
    panel2 = Color(0xFF142B44),
    line = Color(0x1AFFFFFF),
    text = Color(0xFFF7F1E8),
    muted = Color(0xFF9BABBC),
    coral = Color(0xFFFF795F),
    coral2 = Color(0xFFFF9A83),
    cream = Color(0xFFFFF9EE),
    lime = Color(0xFFD8FF72),
    isLight = false
)

val DaylightPalette = PicStarColors(
    ink = Color(0xFFF4F1EB),
    navy = Color(0xFFFFFAF2),
    panel = Color(0xFFFFFFFF),
    panel2 = Color(0xFFE6EDF2),
    line = Color(0x1C071423),
    text = Color(0xFF102033),
    muted = Color(0xFF6C7B8A),
    coral = Color(0xFFFF795F),
    coral2 = Color(0xFFE05A40),
    cream = Color(0xFF102033),
    lime = Color(0xFFB5E533),
    isLight = true
)

val LocalPicStarColors = staticCompositionLocalOf { MidnightPalette }

@Composable
fun PicStarTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useLight = when (themeMode) {
        ThemeMode.MIDNIGHT -> false
        ThemeMode.DAYLIGHT -> true
        ThemeMode.SYSTEM -> !systemDark
    }
    val palette = if (useLight) DaylightPalette else MidnightPalette

    val colorScheme = if (useLight) {
        lightColorScheme(
            background = palette.ink,
            surface = palette.panel,
            primary = palette.coral,
            secondary = palette.lime,
            onBackground = palette.text,
            onSurface = palette.text
        )
    } else {
        darkColorScheme(
            background = palette.ink,
            surface = palette.panel,
            primary = palette.coral,
            secondary = palette.lime,
            onBackground = palette.text,
            onSurface = palette.text
        )
    }

    CompositionLocalProvider(LocalPicStarColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
