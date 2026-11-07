package dev.ambr.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.ar.core.TrackingState
import dev.ambr.ar.ArSessionManager
import dev.ambr.ar.ArStatus
import dev.ambr.ar.ArSurface
import dev.ambr.ar.SessionSource
import dev.ambr.ui.components.ChipTone
import dev.ambr.ui.components.ShutterButton
import dev.ambr.ui.components.ShutterState
import dev.ambr.ui.components.StatusChip
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrSpace
import java.io.File

/**
 * Capture screen. For now: the camera view locked to portrait, a tracking chip, and (when
 * [recordTo] is set) a stop control for recording the session.
 */
@Composable
fun CaptureScreen(
    source: SessionSource = SessionSource.Live,
    recordTo: File? = null,
) {
    val activity = LocalActivity.current ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val manager = remember(activity, source) {
        ArSessionManager(activity, source) { permissionLauncher.launch(Manifest.permission.CAMERA) }
    }

    DisposableEffect(lifecycleOwner, manager) {
        lifecycleOwner.lifecycle.addObserver(manager)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(manager)
            manager.onPause(lifecycleOwner)
            manager.onDestroy(lifecycleOwner)
        }
    }
    LockPortrait(activity)

    val status by manager.status.collectAsStateWithLifecycle()
    val tracking by manager.tracking.collectAsStateWithLifecycle()
    val recording by manager.recording.collectAsStateWithLifecycle()
    val playbackFinished by manager.playbackFinished.collectAsStateWithLifecycle()
    var recordingStarted by remember { mutableStateOf(false) }

    val running = status is ArStatus.Running
    LaunchedEffect(running) {
        if (running && recordTo != null && !recordingStarted) {
            recordingStarted = manager.startSessionRecording(recordTo)
        }
    }

    Box(Modifier.fillMaxSize().background(Ambr.colors.ground)) {
        ArSurface(manager, Modifier.fillMaxSize())

        if (running) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(AmbrSpace.s4),
            ) {
                when {
                    playbackFinished -> StatusChip("Replay finished", Modifier.align(Alignment.TopCenter))
                    recordingStarted && !recording ->
                        StatusChip("Recording saved", Modifier.align(Alignment.TopCenter), tone = ChipTone.OK)
                    else -> TrackingChip(tracking.trackingState, Modifier.align(Alignment.TopCenter))
                }
                if (recordTo != null && recording) {
                    ShutterButton(
                        state = ShutterState.RECORDING,
                        onClick = { manager.stopSessionRecording() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = AmbrSpace.s10),
                    )
                }
            }
        }

        when (val s = status) {
            ArStatus.Starting -> Unit
            is ArStatus.Running ->
                if (!s.depthSupported) FullScreenMessage("This phone doesn't support ARCore depth. Coverage needs it.")
            ArStatus.CameraPermissionDenied -> FullScreenMessage("Allow camera access in settings to scan")
            is ArStatus.Unavailable -> FullScreenMessage(s.reason)
        }
    }
}

@Composable
private fun TrackingChip(state: TrackingState, modifier: Modifier = Modifier) {
    when (state) {
        TrackingState.TRACKING -> StatusChip("Tracking", modifier, tone = ChipTone.OK)
        TrackingState.PAUSED -> StatusChip("Limited", modifier, tone = ChipTone.WARN)
        TrackingState.STOPPED -> StatusChip("Lost", modifier, tone = ChipTone.ERR)
    }
}

@Composable
private fun LockPortrait(activity: Activity) {
    DisposableEffect(activity) {
        val previous = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose { activity.requestedOrientation = previous }
    }
}

@Composable
private fun FullScreenMessage(text: String) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Ambr.colors.ground)
            .padding(AmbrSpace.s6),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Ambr.type.body, color = Ambr.colors.ink, textAlign = TextAlign.Center)
    }
}
