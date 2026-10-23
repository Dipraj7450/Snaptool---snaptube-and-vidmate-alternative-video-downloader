package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.local.AccentTheme
import com.example.data.local.ThemeMode

@Composable
fun SnapLoadTheme(
    themeMode: ThemeMode = ThemeMode.DARK_AMOLED,
    accentTheme: AccentTheme = AccentTheme.SNAP_AMBER,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK_AMOLED, ThemeMode.DARK_SLATE -> true
    }

    val primaryColor = Color(accentTheme.primaryHex)
    val secondaryColor = when (accentTheme) {
        AccentTheme.SNAP_AMBER -> AmberSecondary
        AccentTheme.ELECTRIC_BLUE -> BlueSecondary
        AccentTheme.NEON_EMERALD -> EmeraldSecondary
        AccentTheme.CYBER_PURPLE -> PurpleSecondary
        AccentTheme.SUNSET_CRIMSON -> CrimsonSecondary
    }

    val colorScheme = if (isDark) {
        val bg = if (themeMode == ThemeMode.DARK_AMOLED) AmoledBackground else SlateBackground
        val surf = if (themeMode == ThemeMode.DARK_AMOLED) AmoledSurface else SlateSurface
        val surfVar = if (themeMode == ThemeMode.DARK_AMOLED) AmoledSurfaceVariant else SlateSurfaceVariant

        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.Black,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            background = bg,
            surface = surf,
            surfaceVariant = surfVar,
            onBackground = Color(0xFFE5E6F0),
            onSurface = Color(0xFFE5E6F0),
            onSurfaceVariant = Color(0xFFA0A3BD),
            outline = Color(0xFF383A52)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.15f),
            onPrimaryContainer = primaryColor,
            secondary = secondaryColor,
            background = Color(0xFFF7F8FC),
            surface = Color.White,
            surfaceVariant = Color(0xFFEEF0F8),
            onBackground = Color(0xFF191A23),
            onSurface = Color(0xFF191A23),
            onSurfaceVariant = Color(0xFF5A5E73),
            outline = Color(0xFFD4D6E4)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
