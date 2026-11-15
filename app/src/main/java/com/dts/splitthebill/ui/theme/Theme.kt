package com.dts.splitthebill.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Immutable
internal data class BalanceCardColors(
    val container: Color,
    val content: Color,
    val supporting: Color,
    val positive: Color = PaleSeaGlass,
    val negative: Color = PaleApricot,
)

internal val LocalBalanceCardColors = staticCompositionLocalOf {
    BalanceCardColors(Ink, Paper, Paper.copy(alpha = 0.75f))
}

private val DarkColorScheme = darkColorScheme(
    primary = PaleSeaGlass,
    onPrimary = Ink,
    primaryContainer = NightElevated,
    onPrimaryContainer = PaleSeaGlass,
    secondary = PaleApricot,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF402B26),
    onSecondaryContainer = PaleApricot,
    tertiary = Color(0xFF91D3DC),
    onTertiary = Color(0xFF00363D),
    tertiaryContainer = Color(0xFF163A40),
    onTertiaryContainer = Color(0xFFBCEBF0),
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
    inverseSurface = NightText,
    inverseOnSurface = Night,
    inversePrimary = SeaGlass,
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
    tertiary = Color(0xFF27636A),
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFD4F5F5),
    onTertiaryContainer = Color(0xFF16474D),
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
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = PaleSeaGlass,
    error = Negative,
    onError = Paper,
    errorContainer = NegativeSoft,
    onErrorContainer = Color(0xFF6D2317),
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

    // The balance hero stays dark in both themes; its foregrounds must use the same pair.
    val balanceColors = BalanceCardColors(
        container = if (darkTheme) colorScheme.surfaceContainerHigh else colorScheme.inverseSurface,
        content = if (darkTheme) colorScheme.onSurface else colorScheme.inverseOnSurface,
        supporting = if (darkTheme) colorScheme.onSurfaceVariant else colorScheme.inverseOnSurface.copy(alpha = 0.75f),
    )
    CompositionLocalProvider(LocalBalanceCardColors provides balanceColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                contentColor = colorScheme.onBackground,
                content = content,
            )
        }
    }
}
