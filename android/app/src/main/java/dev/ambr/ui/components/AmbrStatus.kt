package dev.ambr.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrRadius
import dev.ambr.ui.theme.AmbrSpace

private const val METER_MS = 300

enum class ChipTone { OK, WARN, ERR, REC, ACCENT, NEUTRAL }

/**
 * A pill stating one fact, always with an icon (or the recording dot) and a word.
 * Sits on `glass` by default; pass [onSurface] on solid screens.
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: ChipTone = ChipTone.NEUTRAL,
    icon: String? = null,
    onSurface: Boolean = false,
) {
    val c = Ambr.colors
    val background = when {
        onSurface -> c.surface2
        tone == ChipTone.ACCENT -> c.accentSoft
        else -> c.glass
    }
    val textColor = if (tone == ChipTone.ACCENT) c.accent else c.ink
    val glyph = icon ?: when (tone) {
        ChipTone.OK -> "check_circle"
        ChipTone.WARN -> "warning"
        ChipTone.ERR -> "error"
        else -> null
    }
    val iconColor = when (tone) {
        ChipTone.OK -> c.ok
        ChipTone.WARN -> c.warn
        ChipTone.ERR -> c.err
        else -> textColor
    }
    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(AmbrRadius.pill))
            .background(background)
            .padding(start = AmbrSpace.s2, end = 10.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tone == ChipTone.REC) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.rec))
        } else if (glyph != null) {
            AmbrIcon(
                glyph,
                size = 16.dp,
                filled = tone != ChipTone.NEUTRAL && tone != ChipTone.ACCENT,
                tint = iconColor,
            )
        }
        Text(
            text,
            style = Ambr.type.caption.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            maxLines = 1,
            softWrap = false,
        )
    }
}

enum class TrackingQuality { OK, LIMITED, LOST }

/** The top glass strip on the capture screen. */
@Composable
fun StatusBar(
    preset: ScenePreset,
    time: String,
    size: String,
    recording: Boolean,
    tracking: TrackingQuality,
    modifier: Modifier = Modifier,
) {
    val c = Ambr.colors
    val num = Ambr.type.num.copy(color = c.ink)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmbrRadius.lg))
            .background(c.glass)
            .padding(horizontal = AmbrSpace.s3, vertical = 10.dp)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmbrIcon(preset.icon, size = 18.dp, tint = c.ink)
            Text(preset.label, style = Ambr.type.label, color = c.ink)
        }
        Box(Modifier.size(width = 1.dp, height = 14.dp).background(c.line))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (recording) Box(Modifier.size(8.dp).clip(CircleShape).background(c.rec))
            Text(time, style = num, maxLines = 1, softWrap = false)
        }
        Text(size, style = num, maxLines = 1, softWrap = false)
        Spacer(Modifier.weight(1f))
        when (tracking) {
            TrackingQuality.OK -> StatusChip("Tracking", tone = ChipTone.OK)
            TrackingQuality.LIMITED -> StatusChip("Limited", tone = ChipTone.WARN)
            TrackingQuality.LOST -> StatusChip("Lost", tone = ChipTone.ERR)
        }
    }
}

enum class CoachTone { GUIDE, WARN }

/** One short instruction during capture, centred under the StatusBar. */
@Composable
fun CoachPrompt(
    text: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    tone: CoachTone = CoachTone.GUIDE,
) {
    val c = Ambr.colors
    Row(
        modifier = modifier
            .widthIn(max = 320.dp)
            .clip(RoundedCornerShape(AmbrRadius.pill))
            .background(c.glassStrong)
            .padding(start = AmbrSpace.s2, top = AmbrSpace.s2, bottom = AmbrSpace.s2, end = 14.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (tone == CoachTone.WARN) c.warn else c.accent),
            contentAlignment = Alignment.Center,
        ) {
            AmbrIcon(
                icon ?: if (tone == CoachTone.WARN) "speed" else "arrow_forward",
                size = 18.dp,
                tint = c.onAccent,
            )
        }
        Text(
            text,
            style = Ambr.type.body.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp),
            color = c.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Coverage percentage (0 to 100) for the whole scan, or for one region when [label] is given. */
@Composable
fun CoverageMeter(
    value: Int,
    modifier: Modifier = Modifier,
    label: String = "Coverage",
) {
    val c = Ambr.colors
    val v = value.coerceIn(0, 100)
    val fill by animateFloatAsState(v / 100f, tween(METER_MS), label = "coverageFill")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(v / 100f, 0f..1f)
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = Ambr.type.label, color = c.inkSecondary, modifier = Modifier.alignByBaseline())
            Text("$v%", style = Ambr.type.num, color = c.ink, modifier = Modifier.alignByBaseline())
        }
        ProgressTrack(fill)
    }
}

/** 4dp `accent` bar on a `line` track, shared by CoverageMeter and PipelineStep. */
@Composable
internal fun ProgressTrack(fraction: Float, modifier: Modifier = Modifier) {
    val c = Ambr.colors
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(c.line),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(shape)
                .background(c.accent),
        )
    }
}
