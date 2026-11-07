package dev.ambr.ar

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.ArCoreApk
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.EnumSet

private const val TAG = "Ambr"

data class TrackingInfo(
    val trackingState: TrackingState,
    val trackingFailureReason: TrackingFailureReason,
)

sealed interface ArStatus {
    /** Waiting for permission, ARCore install or session start. */
    data object Starting : ArStatus

    data object CameraPermissionDenied : ArStatus

    /** Session running. */
    data class Running(val depthSupported: Boolean) : ArStatus

    data class Unavailable(val reason: String) : ArStatus
}

/**
 * Owns the ARCore [Session] for the capture screen and follows the activity lifecycle.
 *
 * Lifecycle callbacks run on the main thread; [onDrawFrame] and the GL setters run on the GL thread.
 */
class ArSessionManager(
    private val activity: Activity,
    private val requestCameraPermission: () -> Unit,
) : DefaultLifecycleObserver {

    private var session: Session? = null
    private var userRequestedInstall = true
    private var permissionRequested = false
    private var surfaceView: GLSurfaceView? = null

    // Written on the GL thread, applied to the session before the next update.
    @Volatile private var cameraTextureName: Int? = null
    private var cameraTextureApplied = false
    @Volatile private var pendingGeometry: IntArray? = null

    private val _status = MutableStateFlow<ArStatus>(ArStatus.Starting)
    val status: StateFlow<ArStatus> = _status.asStateFlow()

    private val _tracking = MutableStateFlow(TrackingInfo(TrackingState.PAUSED, TrackingFailureReason.NONE))
    val tracking: StateFlow<TrackingInfo> = _tracking.asStateFlow()

    /** The view rendering this session; paused before and resumed after the session. */
    fun attachSurface(view: GLSurfaceView?) {
        surfaceView = view
    }

    override fun onResume(owner: LifecycleOwner) {
        if (session == null && !createSession()) return
        try {
            session?.resume()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available", e)
            _status.value = ArStatus.Unavailable("Camera is in use by another app")
            return
        }
        surfaceView?.onResume()
    }

    override fun onPause(owner: LifecycleOwner) {
        surfaceView?.onPause()
        session?.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        session?.close()
        session = null
    }

    /** Returns true once a session exists; false while waiting for install or permission. */
    private fun createSession(): Boolean {
        try {
            when (ArCoreApk.getInstance().requestInstall(activity, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    userRequestedInstall = false
                    return false
                }
                ArCoreApk.InstallStatus.INSTALLED -> Unit
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            _status.value = ArStatus.Unavailable("Google Play Services for AR is needed to scan")
            return false
        } catch (e: UnavailableException) {
            Log.e(TAG, "ARCore unavailable", e)
            _status.value = ArStatus.Unavailable("ARCore isn't available on this phone")
            return false
        }

        val granted = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            if (permissionRequested) {
                _status.value = ArStatus.CameraPermissionDenied
            } else {
                permissionRequested = true
                requestCameraPermission()
            }
            return false
        }

        val newSession = try {
            Session(activity)
        } catch (e: UnavailableException) {
            Log.e(TAG, "Session creation failed", e)
            _status.value = ArStatus.Unavailable("ARCore isn't available on this phone")
            return false
        }

        val chosen = chooseCameraConfig(newSession)
        val depthSupported = newSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
        val config = Config(newSession).apply {
            depthMode = if (depthSupported) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
            focusMode = Config.FocusMode.AUTO
            updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
            planeFindingMode = Config.PlaneFindingMode.DISABLED
            lightEstimationMode = Config.LightEstimationMode.DISABLED
        }
        newSession.configure(config)

        Log.i(
            TAG,
            "capture setup: device ${Build.MODEL}, cpu image ${chosen.imageSize.width}x${chosen.imageSize.height}, " +
                "gpu texture ${chosen.textureSize.width}x${chosen.textureSize.height}, " +
                "depth supported: ${if (depthSupported) "yes" else "no"}, arcore ${arcoreVersion()}",
        )

        session = newSession
        cameraTextureApplied = false
        _status.value = ArStatus.Running(depthSupported)
        return true
    }

    /** Among the 30 fps configs, picks the one with the largest CPU image. Logs every candidate. */
    private fun chooseCameraConfig(session: Session): CameraConfig {
        val filter = CameraConfigFilter(session).setTargetFps(EnumSet.of(CameraConfig.TargetFps.TARGET_FPS_30))
        val configs = session.getSupportedCameraConfigs(filter)
        configs.forEachIndexed { i, c ->
            Log.i(
                TAG,
                "camera config $i: cpu ${c.imageSize.width}x${c.imageSize.height}, " +
                    "gpu ${c.textureSize.width}x${c.textureSize.height}, fps ${c.fpsRange}, " +
                    "depth sensor ${c.depthSensorUsage}, facing ${c.facingDirection}",
            )
        }
        val chosen = configs.maxByOrNull { it.imageSize.width.toLong() * it.imageSize.height } ?: session.cameraConfig
        session.cameraConfig = chosen
        return chosen
    }

    private fun arcoreVersion(): String = try {
        activity.packageManager.getPackageInfo("com.google.ar.core", 0).versionName ?: "unknown"
    } catch (e: PackageManager.NameNotFoundException) {
        "not installed"
    }

    /** GL thread: the external OES texture the camera image is streamed into. */
    fun setCameraTextureName(textureId: Int) {
        cameraTextureName = textureId
        cameraTextureApplied = false
    }

    /** GL thread: display rotation (`Surface.ROTATION_*`) and viewport size. */
    fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        pendingGeometry = intArrayOf(rotation, width, height)
    }

    /** GL thread: advances the session and returns the newest frame, or null if none is available. */
    fun onDrawFrame(): Frame? {
        val s = session ?: return null
        val texture = cameraTextureName ?: return null
        if (!cameraTextureApplied) {
            s.setCameraTextureName(texture)
            cameraTextureApplied = true
        }
        pendingGeometry?.let { (rotation, w, h) ->
            s.setDisplayGeometry(rotation, w, h)
            pendingGeometry = null
        }
        val frame = try {
            s.update()
        } catch (e: CameraNotAvailableException) {
            Log.e(TAG, "Camera not available during update", e)
            return null
        }
        val camera = frame.camera
        val info = TrackingInfo(camera.trackingState, camera.trackingFailureReason)
        if (info != _tracking.value) _tracking.value = info
        return frame
    }
}
