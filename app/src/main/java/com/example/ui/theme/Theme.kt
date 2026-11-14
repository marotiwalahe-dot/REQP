package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SecureStudyDarkColorScheme = darkColorScheme(
    primary = LiquidYellow,
    onPrimary = DeepBlack,
    primaryContainer = WarmGoldSurface,
    onPrimaryContainer = LiquidYellow,
    secondary = GoldenYellow,
    onSecondary = DeepBlack,
    secondaryContainer = CharcoalElevated,
    onSecondaryContainer = PureWhite,
    tertiary = AmberGlow,
    onTertiary = DeepBlack,
    background = DeepBlack,
    onBackground = PureWhite,
    surface = ObsidianSurface,
    onSurface = PureWhite,
    surfaceVariant = CharcoalElevated,
    onSurfaceVariant = MutedZinc,
    outline = CharcoalBorder,
    outlineVariant = CharcoalElevated,
    error = CrimsonError,
    onError = PureWhite,
)

val SecureStudyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SecureStudyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SecureStudyDarkColorScheme,
        typography = SecureStudyTypography,
        shapes = SecureStudyShapes,
        content = content
    )
}
