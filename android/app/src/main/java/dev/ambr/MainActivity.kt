package dev.ambr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.ambr.ar.SessionSource
import dev.ambr.ui.screens.CaptureScreen
import dev.ambr.ui.screens.GalleryScreen
import dev.ambr.ui.theme.AmbrTheme
import java.io.File

private enum class DebugScreen { GALLERY, CAPTURE, RECORD, REPLAY }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val devSession = File(filesDir, "dev/dev_session.mp4")
        setContent {
            // Debug navigation until the real screens exist.
            var screen by rememberSaveable { mutableStateOf(DebugScreen.GALLERY) }
            if (screen != DebugScreen.GALLERY) BackHandler { screen = DebugScreen.GALLERY }
            when (screen) {
                DebugScreen.GALLERY -> GalleryScreen(
                    onOpenCapture = { screen = DebugScreen.CAPTURE },
                    onRecordDevSession = { screen = DebugScreen.RECORD },
                    onReplayDevSession = { screen = DebugScreen.REPLAY },
                )
                DebugScreen.CAPTURE -> AmbrTheme { CaptureScreen() }
                DebugScreen.RECORD -> AmbrTheme { CaptureScreen(SessionSource.Live, recordTo = devSession) }
                DebugScreen.REPLAY -> AmbrTheme { CaptureScreen(SessionSource.Playback(devSession)) }
            }
        }
    }
}
