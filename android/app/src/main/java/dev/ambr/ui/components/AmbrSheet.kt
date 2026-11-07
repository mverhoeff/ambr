package dev.ambr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrRadius
import dev.ambr.ui.theme.AmbrSpace
import dev.ambr.ui.theme.AmbrThemeMode

/**
 * Bottom-sheet surface for solid screens: `radius-xl` top corners, a grip and a title.
 * The host (e.g. a modal bottom sheet) handles swipe-to-dismiss.
 */
@Composable
fun AmbrSheet(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ambr.colors
    val outdoor = Ambr.mode == AmbrThemeMode.OUTDOOR
    val radius = AmbrRadius.xl
    val shape = RoundedCornerShape(topStart = radius, topEnd = radius)
    val edge = if (outdoor) c.lineStrong else c.line
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (outdoor) Modifier
                else Modifier.dropShadow(
                    shape,
                    Shadow(radius = 32.dp, color = Color.Black, offset = DpOffset(0.dp, (-8).dp), alpha = 0.45f),
                ),
            )
            .clip(shape)
            .background(c.surface)
            .drawWithContent {
                drawContent()
                // 1dp top edge that follows the rounded corners.
                val r = radius.toPx()
                val half = 0.5.dp.toPx()
                val path = Path().apply {
                    moveTo(half, r)
                    arcTo(Rect(half, half, 2 * r - half, 2 * r - half), 180f, 90f, false)
                    lineTo(size.width - r, half)
                    arcTo(Rect(size.width - 2 * r + half, half, size.width - half, 2 * r - half), 270f, 90f, false)
                }
                drawPath(path, edge, style = Stroke(1.dp.toPx()))
            }
            .padding(start = AmbrSpace.s5, end = AmbrSpace.s5, top = AmbrSpace.s2, bottom = AmbrSpace.s5),
    ) {
        Box(
            Modifier
                .padding(bottom = AmbrSpace.s4)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(c.lineStrong)
                .align(Alignment.CenterHorizontally),
        )
        if (title != null) {
            Text(
                title,
                style = Ambr.type.title,
                color = c.ink,
                modifier = Modifier
                    .padding(bottom = AmbrSpace.s4)
                    .semantics { heading() },
            )
        }
        content()
    }
}

enum class ViewerMode { SPLAT, MESH }

/** Floating glass toolbar over the 3D viewer: Splat/Mesh switch, reset view and export. */
@Composable
fun ViewerToolbar(
    mode: ViewerMode,
    onMode: (ViewerMode) -> Unit,
    onResetView: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ambr.colors
    val pill = RoundedCornerShape(AmbrRadius.pill)
    Row(
        modifier = modifier
            .clip(pill)
            .background(c.glass)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeButton("Splat", "blur_on", mode == ViewerMode.SPLAT) { onMode(ViewerMode.SPLAT) }
        ModeButton("Mesh", "deployed_code", mode == ViewerMode.MESH) { onMode(ViewerMode.MESH) }
        Box(
            Modifier
                .padding(horizontal = 2.dp)
                .size(width = 1.dp, height = 24.dp)
                .background(c.line),
        )
        AmbrIconButton(icon = "3d_rotation", label = "Reset view", onClick = onResetView)
        AmbrIconButton(icon = "ios_share", label = "Export", onClick = onExport)
    }
}

@Composable
private fun ModeButton(label: String, icon: String, selected: Boolean, onClick: () -> Unit) {
    val c = Ambr.colors
    val shape = RoundedCornerShape(AmbrRadius.pill)
    val interaction = remember { MutableInteractionSource() }
    val fg = if (selected) c.onAccent else c.inkSecondary
    Row(
        Modifier
            .focusRing(interaction, shape)
            .clip(shape)
            .background(if (selected) c.accent else Color.Transparent)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .height(40.dp)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AmbrIcon(icon, size = 18.dp, tint = fg)
        Text(label, style = Ambr.type.label, color = fg)
    }
}
