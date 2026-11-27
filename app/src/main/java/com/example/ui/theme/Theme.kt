package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ForgeDarkColorScheme = darkColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = CyanOnPrimaryDark,
    primaryContainer = CyanContainerDark,
    onPrimaryContainer = CyanOnContainerDark,
    secondary = EmeraldSecondaryDark,
    onSecondary = EmeraldOnSecondaryDark,
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = EmeraldOnContainerDark,
    tertiary = VioletTertiaryDark,
    onTertiary = VioletOnTertiaryDark,
    tertiaryContainer = VioletContainerDark,
    onTertiaryContainer = VioletOnContainerDark,
    background = ForgeObsidian,
    onBackground = TextPrimaryDark,
    surface = ForgeSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = ForgeSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ForgeSurfaceDark,
    surfaceContainer = ForgeSurfaceVariantDark,
    surfaceContainerHigh = ForgeSurfaceElevatedDark,
    outline = ForgeOutlineDark,
    outlineVariant = ForgeOutlineVariantDark,
    error = StatusError,
    onError = Color.White,
)

private val ForgeLightColorScheme = lightColorScheme(
    primary = CyanPrimaryLight,
    onPrimary = CyanOnPrimaryLight,
    primaryContainer = CyanContainerLight,
    onPrimaryContainer = CyanOnContainerLight,
    secondary = EmeraldSecondaryLight,
    onSecondary = EmeraldOnSecondaryLight,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = EmeraldOnContainerLight,
    tertiary = VioletTertiaryLight,
    onTertiary = VioletOnTertiaryLight,
    tertiaryContainer = VioletContainerLight,
    onTertiaryContainer = VioletOnContainerLight,
    background = ForgePorcelain,
    onBackground = TextPrimaryLight,
    surface = ForgeSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ForgeSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = ForgeSurfaceLight,
    surfaceContainer = ForgeSurfaceVariantLight,
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = ForgeOutlineLight,
    outlineVariant = ForgeOutlineVariantLight,
    error = StatusError,
    onError = Color.White,
)

val ForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

enum class ThemePreference {
    DARK,
    LIGHT,
    SYSTEM,
}

@Composable
fun RepoForgeTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) ForgeDarkColorScheme else ForgeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ForgeShapes,
        content = content,
    )
}
