package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ColorTokensSurfaceHigh = androidx.compose.ui.graphics.Color(0xFF332C24)

private val EstudihambreColorScheme = darkColorScheme(
    primary = HoneyBrothGold,
    onPrimary = CastIronDark,
    primaryContainer = WarmGoldContainer,
    onPrimaryContainer = HoneyBrothGold,
    secondary = HyruleSage,
    onSecondary = CastIronDark,
    secondaryContainer = ForestEmeraldContainer,
    onSecondaryContainer = HyruleSage,
    tertiary = CrystalWaterBlue,
    onTertiary = CastIronDark,
    tertiaryContainer = DeepCrystalContainer,
    onTertiaryContainer = CrystalWaterBlue,
    background = CastIronDark,
    onBackground = ParchmentCream,
    surface = HearthCharcoal,
    onSurface = ParchmentCream,
    surfaceVariant = SmokedOak,
    onSurfaceVariant = MutedParchment,
    surfaceContainerLow = HearthCharcoal,
    surfaceContainer = SmokedOak,
    surfaceContainerHigh = ColorTokensSurfaceHigh,
    outline = WarmOutline,
    outlineVariant = LeatherBorder,
    error = EmberCrimson,
    onError = CastIronDark,
    errorContainer = EmberContainer,
    onErrorContainer = EmberCrimson
)

private val EstudihambreShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun EstudihambreLifeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EstudihambreColorScheme,
        typography = AppTypography,
        shapes = EstudihambreShapes,
        content = content
    )
}
