package fun.morphly.vc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat

/** Reports camera visibility and attempts a real Camera2 open/close on the foreground activity.
 * This is NOT a system-camera-hook test. Such hooks require separate app-sandbox integration.
 */
class CameraCapabilityProbe(private val context: Context) {
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun reportHardware(): String {
        val ids = try { manager.cameraIdList } catch (e: Exception) {
            return "Cannot enumerate cameras: ${e.javaClass.simpleName}: ${e.message}"
        }
        val lines = mutableListOf(
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Device ${Build.MANUFACTURER} / ${Build.MODEL}",
            "Camera permission: ${if (permissionGranted()) "GRANTED" else "NOT GRANTED"}",
            "Camera2 IDs: ${if (ids.isEmpty()) "none" else ids.joinToString()}",
            "External camera registration: not available to ordinary third-party apps"
        )
        ids.forEach { id ->
            try {
                val lens = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                val name = when (lens) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
                    else -> "unknown"
                }
                lines.add("Camera $id: $name")
            } catch (e: CameraAccessException) {
                lines.add("Camera $id: query failed (${e.reason})")
            }
        }
        lines.add(HiddenApiProbe.camera2ReflectionStatus())
        lines.add(NativeFrameProbe().runSelfTest())
        return lines.joinToString("\n")
    }

    fun permissionGranted(): Boolean = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun tryOpenCamera(onResult: (String) -> Unit) {
        if (!permissionGranted()) {
            onResult("Open test: CAMERA permission denied")
            return
        }
        val id = try {
            manager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            onResult("Open test: enumeration failed: ${e.message}")
            return
        }
        if (id == null) {
            onResult("Open test: no physical camera listed")
            return
        }
        val thread = HandlerThread("MorphlyCameraProbe").apply { start() }
        val handler = Handler(thread.looper)
        var finished = false
        fun complete(message: String) {
            if (finished) return
            finished = true
            handler.post { thread.quitSafely() }
            onResult(message)
        }
        try {
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    camera.close()
                    complete("Camera2 open/close: PASS (camera ID $id). This does NOT prove virtual-camera injection.")
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    complete("Camera2 open: disconnected")
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    complete("Camera2 open: ERROR $error")
                }
            }, handler)
        } catch (e: SecurityException) {
            complete("Camera2 open: blocked by permissions (${e.message})")
        } catch (e: CameraAccessException) {
            complete("Camera2 open: CameraAccessException ${e.reason}")
        } catch (e: Exception) {
            complete("Camera2 open: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}
