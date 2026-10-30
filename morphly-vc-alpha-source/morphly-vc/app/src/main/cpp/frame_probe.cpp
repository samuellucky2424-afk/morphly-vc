#include <jni.h>
#include <cstdint>
#include <cstddef>

// A testable native boundary for future NV21 frames from a provider SDK.
// This does not hook Android's camera service.
extern "C" JNIEXPORT jboolean JNICALL
Java_fun_morphly_vc_NativeFrameProbe_validateNv21(JNIEnv* env, jobject, jbyteArray bytes,
                                                    jint width, jint height) {
    if (bytes == nullptr || width <= 0 || height <= 0 || (width & 1) || (height & 1))
        return JNI_FALSE;
    const int64_t expected = static_cast<int64_t>(width) * height * 3 / 2;
    if (expected > INT32_MAX) return JNI_FALSE;
    return env->GetArrayLength(bytes) == expected ? JNI_TRUE : JNI_FALSE;
}
