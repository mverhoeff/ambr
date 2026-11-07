package dev.ambr.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ambr.R

private const val SYMBOL_WEIGHT = 500

@OptIn(ExperimentalTextApi::class)
private fun symbolsFamily(fill: Float) = FontFamily(
    Font(
        R.font.material_symbols_rounded,
        weight = FontWeight(SYMBOL_WEIGHT),
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("FILL", fill),
            FontVariation.weight(SYMBOL_WEIGHT),
            FontVariation.Setting("opsz", 24f),
        ),
    ),
)

private val OutlineSymbols = symbolsFamily(0f)
private val FilledSymbols = symbolsFamily(1f)

/**
 * A Material Symbols Rounded glyph, drawn from its ligature [name] (e.g. `deployed_code`).
 * Decorative: controls that show an icon carry the accessible label themselves.
 */
@Composable
fun AmbrIcon(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    filled: Boolean = false,
    tint: Color = LocalContentColor.current,
) {
    val fontSize = with(LocalDensity.current) { size.toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
    ) {
        BasicText(
            text = name,
            style = TextStyle(
                color = tint,
                fontFamily = if (filled) FilledSymbols else OutlineSymbols,
                fontWeight = FontWeight(SYMBOL_WEIGHT),
                fontSize = fontSize,
            ),
            maxLines = 1,
            softWrap = false,
            // Symbols fill the em square directly above the baseline, so put the baseline one
            // icon-size below the top instead of centring on the line box.
            modifier = Modifier.layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                val box = size.roundToPx()
                layout(box, box) {
                    placeable.place((box - placeable.width) / 2, box - placeable[FirstBaseline])
                }
            },
        )
    }
}
