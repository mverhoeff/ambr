package dev.ambr.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.ambr.R

val Manrope = FontFamily(
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

@Immutable
data class AmbrTypography(
    val display: TextStyle,
    val title: TextStyle,
    val heading: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val numLg: TextStyle,
    val num: TextStyle,
    val numSm: TextStyle,
)

private const val TABULAR_FIGURES = "tnum"

val AmbrTypeScale = AmbrTypography(
    display = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em,
    ),
    title = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.01).em,
    ),
    heading = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 18.sp, lineHeight = 24.sp,
    ),
    body = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 22.sp,
    ),
    label = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 18.sp,
    ),
    caption = TextStyle(
        fontFamily = Manrope, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp,
    ),
    numLg = TextStyle(
        fontFamily = PlexMono, fontWeight = FontWeight.Medium,
        fontSize = 28.sp, lineHeight = 32.sp, fontFeatureSettings = TABULAR_FIGURES,
    ),
    num = TextStyle(
        fontFamily = PlexMono, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, fontFeatureSettings = TABULAR_FIGURES,
    ),
    numSm = TextStyle(
        fontFamily = PlexMono, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.02.em,
        fontFeatureSettings = TABULAR_FIGURES,
    ),
)
