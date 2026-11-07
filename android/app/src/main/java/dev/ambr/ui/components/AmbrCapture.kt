package dev.ambr.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrRadius
import dev.ambr.ui.theme.AmbrSpace

/**
 * The bottom glass panel of the capture screen: CoverageMeter, then discard, shutter and fog toggle.
 * [onDiscard] must not discard directly; the caller pauses and asks for confirmation first.
 */
@Composable
fun CaptureDock(
    coverage: Int,
    state: ShutterState,
    fogOn: Boolean,
    onShutter: () -> Unit,
    onDiscard: () -> Unit,
    onFogToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AmbrRadius.xl))
            .background(Ambr.colors.glass)
            .padding(start = AmbrSpace.s4, end = AmbrSpace.s4, top = AmbrSpace.s3, bottom = AmbrSpace.s4),
        verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3),
    ) {
        CoverageMeter(coverage)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmbrIconButton(icon = "close", label = "Discard scan", onClick = onDiscard, glass = true)
            ShutterButton(state = state, onClick = onShutter)
            AmbrIconButton(
                icon = "visibility",
                label = "Show fog overlay",
                onClick = onFogToggle,
                glass = true,
                active = fogOn,
            )
        }
    }
}
