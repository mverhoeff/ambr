package dev.ambr.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.dp
import dev.ambr.ui.components.AmbrButton
import dev.ambr.ui.components.AmbrIcon
import dev.ambr.ui.components.AmbrIconButton
import dev.ambr.ui.components.AmbrSheet
import dev.ambr.ui.components.AmbrToggle
import dev.ambr.ui.components.ButtonVariant
import dev.ambr.ui.components.CaptureDock
import dev.ambr.ui.components.ChipTone
import dev.ambr.ui.components.CoachPrompt
import dev.ambr.ui.components.CoachTone
import dev.ambr.ui.components.CoverageMeter
import dev.ambr.ui.components.DeviceCard
import dev.ambr.ui.components.DeviceState
import dev.ambr.ui.components.PipelineStep
import dev.ambr.ui.components.PresetPicker
import dev.ambr.ui.components.ScanCard
import dev.ambr.ui.components.ScanStatus
import dev.ambr.ui.components.ScenePreset
import dev.ambr.ui.components.SettingRow
import dev.ambr.ui.components.SettingsGroup
import dev.ambr.ui.components.ShutterButton
import dev.ambr.ui.components.ShutterState
import dev.ambr.ui.components.StatTile
import dev.ambr.ui.components.StatusBar
import dev.ambr.ui.components.StatusChip
import dev.ambr.ui.components.StepState
import dev.ambr.ui.components.TrackingQuality
import dev.ambr.ui.components.ViewerMode
import dev.ambr.ui.components.ViewerToolbar
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrSpace
import dev.ambr.ui.theme.AmbrTheme
import dev.ambr.ui.theme.AmbrThemeMode

/** Stand-in for the live camera image behind capture components. */
private val CameraPlaceholder = Color(0xFF8D8379)

/** Debug screen showing every component in both themes, for side-by-side comparison. */
@Composable
fun GalleryScreen() {
    var mode by rememberSaveable { mutableStateOf(AmbrThemeMode.DARK) }
    AmbrTheme(mode) {
        val c = Ambr.colors
        Column(
            Modifier
                .fillMaxSize()
                .background(c.ground)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(AmbrSpace.s4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ambr", style = Ambr.type.display, color = c.accent, modifier = Modifier.weight(1f))
                Text("Outdoor", style = Ambr.type.label, color = c.inkSecondary)
                Spacer(Modifier.padding(AmbrSpace.s1))
                AmbrToggle(
                    checked = mode == AmbrThemeMode.OUTDOOR,
                    onCheckedChange = { mode = if (it) AmbrThemeMode.OUTDOOR else AmbrThemeMode.DARK },
                    label = "Outdoor theme",
                )
            }
            GalleryContent()
            Spacer(Modifier.height(AmbrSpace.s10))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryContent() {
    Section("Icon") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            listOf(
                "deployed_code", "chair", "landscape", "photo_camera", "blur_on",
                "upload", "computer", "check_circle", "warning", "error",
            ).forEach { AmbrIcon(it, size = 24.dp, tint = Ambr.colors.ink) }
        }
    }

    Section("Button") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3), verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            AmbrButton("Send to PC", onClick = {}, variant = ButtonVariant.PRIMARY, icon = "upload")
            AmbrButton("Preview on phone", onClick = {})
            AmbrButton("Discard", onClick = {}, variant = ButtonVariant.GHOST)
            AmbrButton("Start scanning", onClick = {}, variant = ButtonVariant.PRIMARY, large = true)
        }
    }

    Section("IconButton") {
        Camera(Modifier.heightIn(min = 96.dp).padding(20.dp)) {
            var fog by remember { mutableStateOf(true) }
            Row(horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
                AmbrIconButton(icon = "close", label = "Discard scan", onClick = {}, glass = true)
                AmbrIconButton(icon = "visibility", label = "Show fog overlay", onClick = { fog = !fog }, glass = true, active = fog)
                AmbrIconButton(icon = "flashlight_on", label = "Torch", onClick = {}, glass = true)
            }
        }
    }

    Section("ShutterButton") {
        Row(Modifier.padding(AmbrSpace.s6), horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s8)) {
            ShutterButton(ShutterState.IDLE, onClick = {})
            ShutterButton(ShutterState.RECORDING, onClick = {})
            ShutterButton(ShutterState.PAUSED, onClick = {})
        }
    }

    Section("Toggle") {
        var a by remember { mutableStateOf(false) }
        var b by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            AmbrToggle(a, { a = it }, label = "Off example")
            AmbrToggle(b, { b = it }, label = "On example")
        }
    }

    Section("StatusChip") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s3), verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            StatusChip("Tracking", tone = ChipTone.OK, onSurface = true)
            StatusChip("Too fast", tone = ChipTone.WARN, onSurface = true)
            StatusChip("Lost", tone = ChipTone.ERR, onSurface = true)
            StatusChip("REC", tone = ChipTone.REC, onSurface = true)
            StatusChip("Processing 42%", tone = ChipTone.ACCENT)
            StatusChip("12 GB free", icon = "sd_card", onSurface = true)
        }
    }

    Section("StatusBar") {
        Camera(Modifier.heightIn(min = 120.dp).padding(AmbrSpace.s4)) {
            Column(verticalArrangement = Arrangement.spacedBy(AmbrSpace.s2)) {
                StatusBar(ScenePreset.ROOM, "02:14", "1.4 GB", recording = true, tracking = TrackingQuality.OK)
                StatusBar(ScenePreset.OBJECT, "00:41", "310 MB", recording = true, tracking = TrackingQuality.LIMITED)
            }
        }
    }

    Section("CoachPrompt") {
        Camera(Modifier.heightIn(min = 136.dp).padding(AmbrSpace.s4)) {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CoachPrompt("Step right to fill in the shelf", icon = "arrow_forward")
                CoachPrompt("Slow down", tone = CoachTone.WARN)
            }
        }
    }

    Section("CoverageMeter") {
        Column(verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            CoverageMeter(64)
            CoverageMeter(92, label = "Shelf")
        }
    }

    Section("CaptureDock") {
        Camera(Modifier.heightIn(min = 200.dp).padding(start = AmbrSpace.s4, end = AmbrSpace.s4, top = 40.dp, bottom = AmbrSpace.s4)) {
            var fog by remember { mutableStateOf(true) }
            var state by remember { mutableStateOf(ShutterState.RECORDING) }
            CaptureDock(
                coverage = 64,
                state = state,
                fogOn = fog,
                onShutter = { state = if (state == ShutterState.RECORDING) ShutterState.IDLE else ShutterState.RECORDING },
                onDiscard = {},
                onFogToggle = { fog = !fog },
            )
        }
    }

    Section("PresetPicker") {
        var preset by remember { mutableStateOf(ScenePreset.ROOM) }
        PresetPicker(preset, onChange = { preset = it })
    }

    Section("ScanCard") {
        val thumb = remember { ColorPainter(CameraPlaceholder) }
        Column(verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            ScanCard("Living room", ScenePreset.ROOM, "6 Nov, 23:14 · 1.4 GB", ScanStatus.READY, onClick = {}, thumbnail = thumb)
            ScanCard("Ceramic vase", ScenePreset.OBJECT, "5 Nov · 310 MB", ScanStatus.PROCESSING, onClick = {}, progress = 42, thumbnail = thumb)
            ScanCard("Garden shed", ScenePreset.OUTDOOR, "2 Nov · 2.1 GB", ScanStatus.FAILED, onClick = {}, thumbnail = thumb)
        }
    }

    Section("StatTile") {
        Row(Modifier.height(IntrinsicSize.Max), horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s2)) {
            StatTile("Coverage", "91", Modifier.weight(1f).fillMaxHeight(), unit = "%", hint = "2 thin areas")
            StatTile("Keyframes", "214", Modifier.weight(1f).fillMaxHeight(), hint = "38 blurry dropped")
            StatTile("Size", "1.4", Modifier.weight(1f).fillMaxHeight(), unit = "GB")
        }
    }

    Section("PipelineStep") {
        Column {
            PipelineStep("Preview mesh", StepState.DONE, detail = "On phone · 12 s", right = "0:12")
            PipelineStep("Preview splat", StepState.RUNNING, detail = "On phone · Brush, 2,000 steps", progress = 58, right = "~1:40")
            PipelineStep("Full splat and textured mesh", StepState.QUEUED, detail = "On your PC · gsplat")
            PipelineStep("Upload", StepState.FAILED, detail = "PC went offline. Retry when it is back.", divider = false)
        }
    }

    Section("DeviceCard") {
        Column(verticalArrangement = Arrangement.spacedBy(AmbrSpace.s3)) {
            DeviceCard(
                "Workstation", "192.168.1.20:8765", DeviceState.CONNECTED,
                onSend = {}, onRetry = {}, onCopyManually = {},
                gpu = "NVIDIA RTX · WSL2 · 18 GB free VRAM",
            )
            DeviceCard("Workstation", "192.168.1.20:8765", DeviceState.OFFLINE, onSend = {}, onRetry = {}, onCopyManually = {})
        }
    }

    Section("SettingRow") {
        var outdoor by remember { mutableStateOf(false) }
        var haptics by remember { mutableStateOf(true) }
        var video by remember { mutableStateOf(true) }
        SettingsGroup {
            SettingRow("Outdoor contrast", description = "Black panels and brighter text for direct sun") {
                AmbrToggle(outdoor, { outdoor = it }, label = "Outdoor contrast")
            }
            SettingRow("Haptic coaching", description = "Buzz on guidance and warnings", divider = true) {
                AmbrToggle(haptics, { haptics = it }, label = "Haptic coaching")
            }
            SettingRow("Record video", description = "Keep the full MP4 next to keyframes", divider = true) {
                AmbrToggle(video, { video = it }, label = "Record video")
            }
        }
    }

    Section("Sheet", padded = false) {
        Spacer(Modifier.height(AmbrSpace.s6))
        AmbrSheet(title = "Living room") {
            Row(Modifier.padding(bottom = AmbrSpace.s4), horizontalArrangement = Arrangement.spacedBy(AmbrSpace.s2)) {
                StatTile("Coverage", "91", Modifier.weight(1f), unit = "%")
                StatTile("Keyframes", "214", Modifier.weight(1f))
                StatTile("Size", "1.4", Modifier.weight(1f), unit = "GB")
            }
            Column(verticalArrangement = Arrangement.spacedBy(AmbrSpace.s2)) {
                AmbrButton("Send to PC", onClick = {}, variant = ButtonVariant.PRIMARY, large = true, block = true, icon = "upload")
                AmbrButton("Preview on phone", onClick = {}, block = true)
                AmbrButton("Keep scanning", onClick = {}, variant = ButtonVariant.GHOST, block = true)
            }
        }
    }

    Section("ViewerToolbar") {
        Camera(Modifier.heightIn(min = 96.dp).padding(20.dp)) {
            var viewer by remember { mutableStateOf(ViewerMode.SPLAT) }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ViewerToolbar(viewer, onMode = { viewer = it }, onResetView = {}, onExport = {})
            }
        }
    }
}

@Composable
private fun Section(name: String, padded: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = AmbrSpace.s6)) {
        Text(
            name,
            style = Ambr.type.heading,
            color = Ambr.colors.ink,
            modifier = Modifier.padding(horizontal = AmbrSpace.s4, vertical = AmbrSpace.s2),
        )
        Column(if (padded) Modifier.padding(horizontal = AmbrSpace.s4) else Modifier, content = content)
    }
}

@Composable
private fun Camera(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(CameraPlaceholder)
            .then(modifier),
        content = content,
    )
}
