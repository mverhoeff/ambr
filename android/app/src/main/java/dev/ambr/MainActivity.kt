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
import dev.ambr.ui.screens.CaptureScreen
import dev.ambr.ui.screens.GalleryScreen
import dev.ambr.ui.theme.AmbrTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Debug navigation until the real screens exist: gallery, with a button to the capture screen.
            var showCapture by rememberSaveable { mutableStateOf(false) }
            if (showCapture) {
                BackHandler { showCapture = false }
                AmbrTheme { CaptureScreen() }
            } else {
                GalleryScreen(onOpenCapture = { showCapture = true })
            }
        }
    }
}
