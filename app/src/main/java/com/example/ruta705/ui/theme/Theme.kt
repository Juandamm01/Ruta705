package com.example.ruta705.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Bone,
    primaryContainer = TealSoft,
    onPrimaryContainer = TealDeep,
    secondary = GoldAccent,
    onSecondary = InkDark,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = InkDark,
    tertiary = GoldDark,
    onTertiary = Surface,
    background = Bone,
    onBackground = InkDark,
    surface = Surface,
    onSurface = InkDark,
    surfaceVariant = TealMist,
    onSurfaceVariant = Stone,
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Surface,
    surfaceContainer = TealMist,
    surfaceContainerHigh = BoneDim,
    outline = StoneLight,
    outlineVariant = BoneDim,
    error = DangerRed,
    onError = Surface,
    errorContainer = DangerSoft,
    onErrorContainer = DangerRed,
    inverseSurface = TealDeep,
    inverseOnSurface = Bone,
)

private val DarkColors = darkColorScheme(
    primary = GoldAccent,
    onPrimary = InkDark,
    primaryContainer = TealMid,
    onPrimaryContainer = Bone,
    secondary = GoldAccent,
    onSecondary = InkDark,
    secondaryContainer = TealMid,
    onSecondaryContainer = Bone,
    tertiary = GoldAccent,
    onTertiary = InkDark,
    background = NightBackground,
    onBackground = Bone,
    surface = NightSurface,
    onSurface = Bone,
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = StoneLight,
    surfaceContainerLowest = NightBackground,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurfaceHigh,
    surfaceContainerHigh = NightSurfaceHigh,
    outline = NightOutline,
    outlineVariant = NightOutline,
    error = DangerSoft,
    onError = DangerRed,
    errorContainer = DangerRed,
    onErrorContainer = DangerSoft,
    inverseSurface = Bone,
    inverseOnSurface = InkDark,
)

val RutaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * Tema de Ruta705. No usa color dinámico (Material You) para preservar la identidad de marca
 * en todos los teléfonos.
 */
@Composable
fun Ruta705Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = RutaShapes,
        content = content
    )
}
