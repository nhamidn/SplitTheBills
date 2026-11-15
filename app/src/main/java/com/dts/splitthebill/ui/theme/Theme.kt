package com.dts.splitthebill.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = PaleSeaGlass,
    onPrimary = Ink,
    primaryContainer = NightElevated,
    onPrimaryContainer = PaleSeaGlass,
    secondary = PaleApricot,
    onSecondary = Ink,
    secondaryContainer = NightSoft,
    onSecondaryContainer = PaleApricot,
    tertiary = Apricot,
    onTertiary = Ink,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightElevated,
    onSurfaceVariant = NightMuted,
    surfaceContainerLowest = NightDeep,
    surfaceContainerLow = NightSoft,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightElevated,
    surfaceContainerHighest = Ink,
    outline = NightOutline,
    outlineVariant = NightElevated,
    error = NightError,
    onError = Ink,
    errorContainer = NightErrorContainer,
    onErrorContainer = NightText,
)

private val LightColorScheme = lightColorScheme(
    primary = Positive,
    onPrimary = Paper,
    primaryContainer = PositiveSoft,
    onPrimaryContainer = Positive,
    secondary = Negative,
    onSecondary = Paper,
    secondaryContainer = NegativeSoft,
    onSecondaryContainer = Negative,
    tertiary = Ink,
    onTertiary = Paper,
    background = Canvas,
    surface = Paper,
    onBackground = Ink,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = SupportText,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Paper,
    surfaceContainer = SurfaceMuted,
    surfaceContainerHigh = SurfaceMuted,
    surfaceContainerHighest = SurfaceMuted,
    outline = MutedText,
    outlineVariant = MintLine,
    error = Negative,
    errorContainer = NegativeSoft,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun SplitTheBillsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Keep the product palette by default; callers may explicitly opt into system colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
