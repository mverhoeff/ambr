package dev.ambr.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrRadius
import dev.ambr.ui.theme.AmbrSize
import dev.ambr.ui.theme.AmbrSpace

private const val PRESS_MS = 120
private const val TOGGLE_MS = 160
private const val SHUTTER_MORPH_MS = 200

/** Draws the 2dp `focus` ring, offset 2dp outside [shape], while the control has focus. */
internal fun Modifier.focusRing(interaction: InteractionSource, shape: Shape): Modifier = composed {
    val focused by interaction.collectIsFocusedAsState()
    val color = Ambr.colors.focus
    drawWithContent {
        drawContent()
        if (focused) {
            val stroke = 2.dp.toPx()
            val inset = 2.dp.toPx() + stroke / 2
            val outline = shape.createOutline(
                Size(size.width + 2 * inset, size.height + 2 * inset), layoutDirection, this,
            )
            translate(-inset, -inset) { drawOutline(outline, color, style = Stroke(stroke)) }
        }
    }
}

enum class ButtonVariant { PRIMARY, SECONDARY, GHOST }

/** Text action for solid screens. Verb first, sentence case, 1 to 3 words. */
@Composable
fun AmbrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.SECONDARY,
    large: Boolean = false,
    icon: String? = null,
    block: Boolean = false,
    textColor: Color? = null,
) {
    val c = Ambr.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(if (large) AmbrRadius.lg else AmbrRadius.md)
    val background by animateColorAsState(
        targetValue = when (variant) {
            ButtonVariant.PRIMARY -> if (pressed) c.accentPressed else c.accent
            ButtonVariant.SECONDARY -> c.surface2
            ButtonVariant.GHOST -> if (pressed) c.surface2 else Color.Transparent
        },
        animationSpec = tween(PRESS_MS),
        label = "buttonBackground",
    )
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(PRESS_MS), label = "buttonScale")
    val content = textColor ?: if (variant == ButtonVariant.PRIMARY) c.onAccent else c.ink
    val labelStyle = if (large) Ambr.type.label.copy(fontSize = 15.sp) else Ambr.type.label

    Row(
        modifier = modifier
            .then(if (block) Modifier.fillMaxWidth() else Modifier)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .focusRing(interaction, shape)
            .clip(shape)
            .background(background)
            .border(1.dp, if (variant == ButtonVariant.SECONDARY) c.lineStrong else Color.Transparent, shape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .heightIn(min = if (large) AmbrSize.targetCapture else AmbrSize.targetMin)
            .padding(horizontal = if (large) AmbrSpace.s6 else AmbrSpace.s4),
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) AmbrIcon(icon, tint = content)
        Text(text, style = labelStyle, color = content)
    }
}

/**
 * Single-icon control. [glass] makes it the 56dp camera-screen variant on a `glass` disc.
 * Pass [active] for toggles (overlay, torch); leave it null for plain actions.
 */
@Composable
fun AmbrIconButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glass: Boolean = false,
    active: Boolean? = null,
) {
    val c = Ambr.colors
    val interaction = remember { MutableInteractionSource() }
    val on = active == true
    val background = when {
        on -> c.accentSoft
        glass -> c.glass
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(if (glass) AmbrSize.targetCapture else AmbrSize.targetMin)
            .focusRing(interaction, CircleShape)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, if (on) c.accent else Color.Transparent, CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = if (active != null) Role.Switch else Role.Button,
                onClick = onClick,
            )
            .semantics {
                contentDescription = label
                if (active != null) stateDescription = if (active) "On" else "Off"
            },
        contentAlignment = Alignment.Center,
    ) {
        AmbrIcon(icon, size = if (glass) 24.dp else 20.dp, filled = on, tint = if (on) c.accent else c.ink)
    }
}

enum class ShutterState { IDLE, RECORDING, PAUSED }

/** The 72dp start/stop control in the CaptureDock. */
@Composable
fun ShutterButton(
    state: ShutterState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ambr.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val recording = state == ShutterState.RECORDING
    val disc by animateColorAsState(
        targetValue = when {
            recording -> c.ink
            pressed -> c.accentPressed
            else -> c.accent
        },
        animationSpec = tween(TOGGLE_MS),
        label = "shutterDisc",
    )
    val innerSize by animateDpAsState(if (recording) 24.dp else 26.dp, tween(SHUTTER_MORPH_MS), label = "shutterInner")
    val innerCorner by animateDpAsState(if (recording) 6.dp else 13.dp, tween(SHUTTER_MORPH_MS), label = "shutterCorner")
    val innerColor by animateColorAsState(if (recording) c.rec else c.onAccent, tween(SHUTTER_MORPH_MS), label = "shutterInnerColor")
    val glass = c.glass
    val ring = c.ink

    Box(
        modifier = modifier
            .size(AmbrSize.shutter)
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(ring, radius = r + 5.dp.toPx())
                drawCircle(glass, radius = r + 3.dp.toPx())
            }
            .focusRing(interaction, CircleShape)
            .clip(CircleShape)
            .background(disc)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = when (state) {
                    ShutterState.IDLE -> "Start scan"
                    ShutterState.RECORDING -> "Stop scan"
                    ShutterState.PAUSED -> "Resume scan"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state == ShutterState.PAUSED) {
            AmbrIcon("play_arrow", size = 32.dp, filled = true, tint = c.onAccent)
        } else {
            Box(
                Modifier
                    .size(innerSize)
                    .clip(RoundedCornerShape(innerCorner))
                    .background(innerColor),
            )
        }
    }
}

/** On/off switch, 52x32 inside a 48dp tap area. Changes apply immediately. */
@Composable
fun AmbrToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val c = Ambr.colors
    val interaction = remember { MutableInteractionSource() }
    val track by animateColorAsState(if (checked) c.accent else c.surface2, tween(TOGGLE_MS), label = "toggleTrack")
    val edge by animateColorAsState(if (checked) c.accent else c.lineStrong, tween(TOGGLE_MS), label = "toggleEdge")
    val knob by animateColorAsState(if (checked) c.onAccent else c.inkSecondary, tween(TOGGLE_MS), label = "toggleKnob")
    val knobX by animateDpAsState(if (checked) 20.dp else 0.dp, tween(TOGGLE_MS), label = "toggleKnobX")
    val shape = RoundedCornerShape(AmbrRadius.pill)

    Box(
        modifier = modifier
            .size(width = 52.dp, height = AmbrSize.targetMin)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 32.dp)
                .focusRing(interaction, shape)
                .clip(shape)
                .background(track)
                .border(2.dp, edge, shape),
        ) {
            Box(
                Modifier
                    .padding(6.dp)
                    .offset(x = knobX)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(knob),
            )
        }
    }
}
