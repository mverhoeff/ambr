package dev.ambr.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

@Immutable
data class AmbrColors(
    val ground: Color,
    val surface: Color,
    val surface2: Color,
    val glass: Color,
    val glassStrong: Color,
    val line: Color,
    val lineStrong: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val fog: Color,
    val fogHatch: Color,
    val ok: Color,
    val warn: Color,
    val err: Color,
    val rec: Color,
    val focus: Color,
)

/** Converts a 0..1 alpha to the 0..255 channel value used by `Color(r, g, b, a)`. */
private fun alpha(a: Float): Int = (a * 255).roundToInt()

private val DarkAccent = Color(0xFFFF9A4D)
private val OutdoorAccent = Color(0xFFFFAD66)

val DarkColors = AmbrColors(
    ground = Color(0xFF0F0E12),
    surface = Color(0xFF1A1820),
    surface2 = Color(0xFF25222C),
    glass = Color(20, 19, 26, alpha(0.86f)),
    glassStrong = Color(20, 19, 26, alpha(0.94f)),
    line = Color(0xFF4A4654),
    lineStrong = Color(0xFF77727F),
    ink = Color(0xFFF4F1F8),
    inkSecondary = Color(0xFFBDB7C8),
    inkTertiary = Color(0xFFA39DB0),
    accent = DarkAccent,
    accentPressed = Color(0xFFF0863A),
    onAccent = Color(0xFF1F1006),
    accentSoft = Color(255, 154, 77, alpha(0.18f)),
    fog = Color(11, 10, 14, alpha(0.62f)),
    fogHatch = Color(244, 241, 248, alpha(0.16f)),
    ok = Color(0xFF5CCBE8),
    warn = Color(0xFFFFD166),
    err = Color(0xFFFF7A7A),
    rec = Color(0xFFFF4D4D),
    focus = DarkAccent,
)

val OutdoorColors = AmbrColors(
    ground = Color(0xFF000000),
    surface = Color(0xFF0D0D0D),
    surface2 = Color(0xFF1A1A1A),
    glass = Color(0, 0, 0, alpha(0.9f)),
    glassStrong = Color(0, 0, 0, alpha(0.96f)),
    line = Color(0xFF9A9A9A),
    lineStrong = Color(0xFFFFFFFF),
    ink = Color(0xFFFFFFFF),
    inkSecondary = Color(0xFFEBEBEB),
    inkTertiary = Color(0xFFCCCCCC),
    accent = OutdoorAccent,
    accentPressed = Color(0xFFFF9A47),
    onAccent = Color(0xFF000000),
    accentSoft = Color(255, 173, 102, alpha(0.28f)),
    fog = Color(0, 0, 0, alpha(0.72f)),
    fogHatch = Color(255, 255, 255, alpha(0.28f)),
    ok = Color(0xFF7FDCF5),
    warn = Color(0xFFFFE08A),
    err = Color(0xFFFF8F8F),
    rec = Color(0xFFFF6B6B),
    focus = OutdoorAccent,
)
