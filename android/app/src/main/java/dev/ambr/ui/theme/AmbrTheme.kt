package dev.ambr.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AmbrThemeMode { DARK, OUTDOOR }

val LocalAmbrColors = staticCompositionLocalOf { DarkColors }
val LocalAmbrType = staticCompositionLocalOf { AmbrTypeScale }

@Composable
fun AmbrTheme(
    mode: AmbrThemeMode = AmbrThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val colors = when (mode) {
        AmbrThemeMode.DARK -> DarkColors
        AmbrThemeMode.OUTDOOR -> OutdoorColors
    }
    CompositionLocalProvider(
        LocalAmbrColors provides colors,
        LocalAmbrType provides AmbrTypeScale,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme(colors),
            typography = materialTypography(AmbrTypeScale),
            content = content,
        )
    }
}

/** Access point for the current ambr theme values: `Ambr.colors.accent`, `Ambr.type.body`. */
object Ambr {
    val colors: AmbrColors
        @Composable @ReadOnlyComposable get() = LocalAmbrColors.current

    val type: AmbrTypography
        @Composable @ReadOnlyComposable get() = LocalAmbrType.current
}

/** Maps ambr colours onto Material roles so stock Material widgets match the app. */
private fun materialColorScheme(c: AmbrColors): ColorScheme = darkColorScheme(
    primary = c.accent,
    onPrimary = c.onAccent,
    primaryContainer = c.surface2,
    onPrimaryContainer = c.ink,
    inversePrimary = c.accentPressed,
    secondary = c.accent,
    onSecondary = c.onAccent,
    secondaryContainer = c.surface2,
    onSecondaryContainer = c.ink,
    tertiary = c.ok,
    onTertiary = c.ground,
    tertiaryContainer = c.surface2,
    onTertiaryContainer = c.ink,
    background = c.ground,
    onBackground = c.ink,
    surface = c.surface,
    onSurface = c.ink,
    surfaceVariant = c.surface2,
    onSurfaceVariant = c.inkSecondary,
    surfaceTint = c.accent,
    inverseSurface = c.ink,
    inverseOnSurface = c.ground,
    error = c.err,
    onError = c.ground,
    errorContainer = c.surface2,
    onErrorContainer = c.err,
    outline = c.lineStrong,
    outlineVariant = c.line,
    scrim = c.fog,
    surfaceBright = c.surface2,
    surfaceDim = c.ground,
    surfaceContainerLowest = c.ground,
    surfaceContainerLow = c.surface,
    surfaceContainer = c.surface,
    surfaceContainerHigh = c.surface2,
    surfaceContainerHighest = c.surface2,
)

private fun materialTypography(t: AmbrTypography): Typography = Typography(
    displayLarge = t.display,
    displayMedium = t.display,
    displaySmall = t.display,
    headlineLarge = t.title,
    headlineMedium = t.title,
    headlineSmall = t.title,
    titleLarge = t.title,
    titleMedium = t.heading,
    titleSmall = t.label,
    bodyLarge = t.body,
    bodyMedium = t.body,
    bodySmall = t.caption,
    labelLarge = t.label,
    labelMedium = t.label,
    labelSmall = t.caption,
)
