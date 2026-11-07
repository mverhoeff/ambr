package dev.ambr.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ambr.ar.ArSessionManager
import dev.ambr.ar.ArStatus
import dev.ambr.ar.ArSurface
import dev.ambr.ui.theme.Ambr
import dev.ambr.ui.theme.AmbrSpace

/** Capture screen. For now: the live camera only, locked to portrait. */
@Composable
fun CaptureScreen() {
    val activity = LocalActivity.current ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val manager = remember(activity) {
        ArSessionManager(activity) { permissionLauncher.launch(Manifest.permission.CAMERA) }
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
    Box(Modifier.fillMaxSize().background(Ambr.colors.ground)) {
        ArSurface(manager, Modifier.fillMaxSize())
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
