package dev.ambr.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrRadius
import dev.ambr.ui.theme.AmbrSize
import dev.ambr.ui.theme.AmbrSpace
import dev.ambr.ui.theme.AmbrThemeMode

/** Display info for the three scene types. Preset numbers live elsewhere; this is only what the UI shows. */
enum class ScenePreset(val icon: String, val label: String, val hint: String) {
    OBJECT("deployed_code", "Object", "Orbit around it at arm's length. 2 cm detail."),
    ROOM("chair", "Room", "Walk the walls, then the middle. 5 cm detail."),
    OUTDOOR("landscape", "Outdoor", "Slow passes along the facade. 20 cm detail."),
}

/** In the Outdoor theme, cards, device cards and stat tiles get a `line` border. */
@Composable
private fun Modifier.outdoorBorder(shape: Shape): Modifier =
    if (Ambr.mode == AmbrThemeMode.OUTDOOR) border(1.dp, Ambr.colors.line, shape) else this

@Composable
private fun cardTitleStyle() = Ambr.type.body.copy(fontWeight = FontWeight.Bold, lineHeight = 20.sp)

enum class ScanStatus { CAPTURED, PROCESSING, READY, FAILED }

/** One row in the scan library. The whole card opens the viewer; long-press opens actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScanCard(
    title: String,
    preset: ScenePreset,
    meta: String,
    status: ScanStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Int? = null,
    onLongClick: (() -> Unit)? = null,
    thumbnail: Painter? = null,
) {
    val c = Ambr.colors
    val shape = RoundedCornerShape(AmbrRadius.lg)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusRing(interaction, shape)
            .clip(shape)
            .background(c.surface)
            .outdoorBorder(shape)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(AmbrSpace.s3),
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(AmbrSize.thumb)
                .clip(RoundedCornerShape(AmbrRadius.sm))
                .background(c.surface2),
        ) {
            if (thumbnail != null) {
                Image(thumbnail, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(AmbrSize.thumb))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AmbrSpace.s1)) {
            Text(title, style = cardTitleStyle(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${preset.label} · $meta", style = Ambr.type.caption, color = c.inkSecondary)
            Row(Modifier.padding(top = 2.dp)) {
                when (status) {
                    ScanStatus.CAPTURED ->
                        StatusChip("Captured", tone = ChipTone.NEUTRAL, icon = "photo_camera", onSurface = true)
                    ScanStatus.PROCESSING ->
                        StatusChip(
                            if (progress != null) "Processing $progress%" else "Processing",
                            tone = ChipTone.ACCENT,
                            icon = "progress_activity",
                            onSurface = true,
                        )
                    ScanStatus.READY -> StatusChip("Splat and mesh ready", tone = ChipTone.OK, onSurface = true)
                    ScanStatus.FAILED -> StatusChip("Failed", tone = ChipTone.ERR, onSurface = true)
                }
            }
        }
        AmbrIcon("chevron_right", size = 24.dp, tint = c.ink)
    }
}

/** One headline number on the review sheet. [value] is already rounded. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    hint: String? = null,
) {
    val c = Ambr.colors
    val shape = RoundedCornerShape(AmbrRadius.md)
    Column(
        modifier = modifier
            .clip(shape)
            .background(c.surface2)
            .outdoorBorder(shape)
            .padding(AmbrSpace.s3)
            .semantics(mergeDescendants = true) { },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = Ambr.type.caption.copy(fontWeight = FontWeight.Bold), color = c.inkSecondary)
        Row {
            Text(value, style = Ambr.type.numLg, color = c.ink, modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Spacer(Modifier.width(2.dp))
                Text(
                    unit,
                    style = Ambr.type.numLg.copy(fontSize = 13.sp),
                    color = c.inkSecondary,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        if (hint != null) Text(hint, style = Ambr.type.caption, color = c.inkTertiary)
    }
}

enum class StepState { DONE, RUNNING, QUEUED, FAILED }

/** One processing stage. Set [divider] false on the last step of a list. */
@Composable
fun PipelineStep(
    label: String,
    state: StepState,
    modifier: Modifier = Modifier,
    detail: String? = null,
    progress: Int? = null,
    right: String? = null,
    divider: Boolean = true,
) {
    val c = Ambr.colors
    val line = c.line
    val ring = when (state) {
        StepState.DONE -> c.ok
        StepState.RUNNING -> c.accent
        StepState.FAILED -> c.err
        StepState.QUEUED -> c.lineStrong
    }
    val glyphColor = if (state == StepState.QUEUED) c.inkTertiary else ring
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                if (divider) {
                    val y = size.height - 0.5.dp.toPx()
                    drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
            .padding(vertical = AmbrSpace.s3)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .border(1.5.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AmbrIcon(
                when (state) {
                    StepState.DONE -> "check"
                    StepState.RUNNING -> "progress_activity"
                    StepState.QUEUED -> "more_horiz"
                    StepState.FAILED -> "close"
                },
                size = 18.dp,
                tint = glyphColor,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = cardTitleStyle(), color = c.ink)
            if (detail != null) Text(detail, style = Ambr.type.caption, color = c.inkSecondary)
            if (state == StepState.RUNNING && progress != null) {
                ProgressTrack(progress / 100f, Modifier.padding(top = 6.dp))
            }
        }
        if (right != null) Text(right, style = Ambr.type.num, color = c.inkSecondary)
    }
}

enum class DeviceState { CONNECTED, SEARCHING, OFFLINE }

/** The paired PC used for full-quality processing. */
@Composable
fun DeviceCard(
    name: String,
    address: String,
    state: DeviceState,
    onSend: () -> Unit,
    onRetry: () -> Unit,
    onCopyManually: () -> Unit,
    modifier: Modifier = Modifier,
    gpu: String? = null,
) {
    val c = Ambr.colors
    val shape = RoundedCornerShape(AmbrRadius.lg)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .outdoorBorder(shape)
            .padding(AmbrSpace.s4),
        verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(AmbrRadius.md))
                    .background(c.surface2),
                contentAlignment = Alignment.Center,
            ) {
                AmbrIcon("computer", size = 24.dp, tint = c.ink)
            }
            Column(Modifier.weight(1f)) {
                Text(name, style = cardTitleStyle(), color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(address, style = Ambr.type.numSm, color = c.inkSecondary)
            }
            when (state) {
                DeviceState.CONNECTED -> StatusChip("Connected", tone = ChipTone.OK, onSurface = true)
                DeviceState.SEARCHING -> StatusChip("Searching", tone = ChipTone.WARN, onSurface = true)
                DeviceState.OFFLINE -> StatusChip("Offline", tone = ChipTone.ERR, onSurface = true)
            }
        }
        if (gpu != null) Text(gpu, style = Ambr.type.caption, color = c.inkSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s2)) {
            if (state == DeviceState.CONNECTED) {
                AmbrButton("Send scan", onClick = onSend, variant = ButtonVariant.PRIMARY, icon = "upload")
            } else {
                AmbrButton("Retry", onClick = onRetry, variant = ButtonVariant.SECONDARY, icon = "refresh")
            }
            AmbrButton("Copy manually", onClick = onCopyManually, variant = ButtonVariant.GHOST)
        }
    }
}

/** Rounded container for a stack of [SettingRow]s. */
@Composable
fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(AmbrRadius.lg)), content = content)
}

/** A settings row with a trailing control. Set [divider] on every row except the first in a group. */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    divider: Boolean = false,
    control: @Composable () -> Unit,
) {
    val c = Ambr.colors
    val line = c.line
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .drawBehind {
                if (divider) drawLine(line, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), 1.dp.toPx())
            }
            .heightIn(min = 56.dp)
            .padding(horizontal = AmbrSpace.s4, vertical = AmbrSpace.s3),
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = cardTitleStyle(), color = c.ink)
            if (description != null) {
                Text(description, style = Ambr.type.caption, color = c.inkSecondary, modifier = Modifier.padding(top = 2.dp))
            }
        }
        control()
    }
}

/** Scene-type choice for the new-scan sheet. */
@Composable
fun PresetPicker(
    value: ScenePreset,
    onChange: (ScenePreset) -> Unit,
    modifier: Modifier = Modifier,
    showHint: Boolean = true,
) {
    val c = Ambr.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AmbrRadius.lg))
                .background(c.surface2)
                .padding(AmbrSpace.s1)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s1),
        ) {
            ScenePreset.entries.forEach { preset ->
                val on = preset == value
                val shape = RoundedCornerShape(AmbrRadius.md)
                val interaction = remember { MutableInteractionSource() }
                val fg = if (on) c.accent else c.inkSecondary
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .focusRing(interaction, shape)
                        .clip(shape)
                        .background(if (on) c.accentSoft else Color.Transparent)
                        .border(1.dp, if (on) c.accent else Color.Transparent, shape)
                        .selectable(
                            selected = on,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = { onChange(preset) },
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AmbrIcon(preset.icon, size = 24.dp, filled = on, tint = fg)
                    Text(preset.label, style = Ambr.type.label, color = fg)
                }
            }
        }
        if (showHint) {
            Text(
                value.hint,
                style = Ambr.type.caption,
                color = c.inkSecondary,
                modifier = Modifier.padding(start = AmbrSpace.s1, end = AmbrSpace.s1, top = AmbrSpace.s2),
            )
        }
    }
}
