package dev.ambr.ar

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.ambr.ar.samplerender.SampleRender
import dev.ambr.ar.samplerender.arcore.BackgroundRenderer

/** Full-screen live camera background for the session owned by [manager] (GLES 3.0). */
@Composable
fun ArSurface(manager: ArSessionManager, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            GLSurfaceView(context).also { view ->
                SampleRender(view, CameraBackgroundRenderer(view, manager), context.assets)
                manager.attachSurface(view)
            }
        },
        onRelease = { manager.attachSurface(null) },
    )
}

private class CameraBackgroundRenderer(
    private val view: GLSurfaceView,
    private val manager: ArSessionManager,
) : SampleRender.Renderer {

    private lateinit var background: BackgroundRenderer

    override fun onSurfaceCreated(render: SampleRender) {
        background = BackgroundRenderer(render)
        try {
            background.setUseDepthVisualization(render, false)
        } catch (e: java.io.IOException) {
            Log.e("Ambr", "Failed to load background shaders", e)
        }
        manager.setCameraTextureName(background.cameraColorTexture.textureId)
    }

    override fun onSurfaceChanged(render: SampleRender, width: Int, height: Int) {
        manager.setDisplayGeometry(view.display.rotation, width, height)
    }

    override fun onDrawFrame(render: SampleRender) {
        val frame = manager.onDrawFrame() ?: return
        background.updateDisplayGeometry(frame)
        if (frame.timestamp != 0L) background.drawBackground(render)
    }
}
