package fun.morphly.vc

class NativeFrameProbe {
    external fun validateNv21(bytes: ByteArray, width: Int, height: Int): Boolean

    companion object {
        val loaded: Boolean = try {
            System.loadLibrary("morphlyframe")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    fun runSelfTest(): String {
        if (!loaded) return "C++ frame library not available"
        val good = validateNv21(ByteArray(640 * 480 * 3 / 2), 640, 480)
        val bad = validateNv21(ByteArray(100), 640, 480)
        return if (good && !bad) "Native NV21 validator: PASS" else "Native NV21 validator: FAIL"
    }
}
