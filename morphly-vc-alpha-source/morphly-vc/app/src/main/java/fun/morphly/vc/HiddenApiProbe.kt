package fun.morphly.vc

/** A read-only feasibility check for Android hidden-API reflection restrictions.
 * Does not install hooks, change the camera service, or access another process.
 * Reflection success does not imply third-party camera interception will work.
 */
object HiddenApiProbe {
    fun camera2ReflectionStatus(): String {
        return try {
            val type = Class.forName("android.hardware.camera2.CameraManager\$CameraManagerGlobal")
            val method = type.getDeclaredMethod("get")
            method.isAccessible = true
            val singleton = method.invoke(null)
            if (singleton != null) {
                "Camera2 hidden-API reflection: accessible in this app (not a replacement camera)"
            } else {
                "Camera2 hidden-API reflection: returned null"
            }
        } catch (e: Throwable) {
            val simple = e.javaClass.simpleName
            "Camera2 hidden-API reflection: blocked or unavailable ($simple)"
        }
    }
}
