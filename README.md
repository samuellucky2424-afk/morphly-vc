# Morphly VC — native Android alpha 0.1

A **rootless, testable first-stage Android prototype** for Morphly's video source, floating preview, and future camera virtualization pipeline. It is a fresh codebase rather than a repackaged BlackBox fork.

> **Status (2026-10-30):** source-only alpha. Not installed or hardware-tested. It **does not yet intercept the camera of WhatsApp, Instagram, or other applications**. Provider SDKs are not connected. See [camera test plan](docs/CAMERA_TEST_PLAN.md).

## Included

- Morphly logo/loading splash and red/white dashboard inspired by the supplied phone references.
- Select local video with Android's native file picker; loop preview.
- Select reference face image, stored as an Android content URI.
- Choose XMax Plus / Vidu Pro and output-quality preference.
- Floating draggable mini video window using the normal Android overlay permission and foreground media-playback service. Close/expand controls.
- Normal Camera2 physical-camera capability probe and permission handling; a read-only hidden-API reflection feasibility check.
- Small C++ NDK JNI NV21 frame-validator self-test, ready to be connected to a source pipeline.
- GitHub Actions workflow to build `app-debug.apk` when pushed to a repository.

## Not included / not claimed

- No provider SDK / backend authentication or credit charges. **Connect** explicitly tells the user this.
- No realtime provider streams, image upload to provider, or output routed into third-party camera consumers.
- No APK included; build requires an Android SDK, NDK and Gradle and is not yet verified on a device.
- No successful non-root camera interception claim. `SYSTEM_ALERT_WINDOW` is an overlay permission, not a camera-override permission.

## Build

Requirements: JDK 17, Android SDK API 35, Android NDK `27.2.12479018`, CMake `3.22.1`, Gradle `8.10.2` (or open with Android Studio and install requested components).

```bash
gradle :app:assembleDebug
```

Result, if build succeeds: `app/build/outputs/apk/debug/app-debug.apk`.

For GitHub upload: create a new repo called `morphly-vc`, push these files to `main`; the Actions tab will run the debug APK build workflow. Never commit provider secrets. Use a secure backend issuing short-lived XMax/Vidu credentials. The previous `Morphlysm2` repository has bridge implementations to study and port after basic compatibility passes.

## Architecture

- `MainActivity.kt`: native Android UI + SAF picker + provider selection + honest integration status.
- `FloatingPreviewService.kt`: movable local-video preview overlay.
- `CameraCapabilityProbe.kt`: normal Camera2 physical-device diagnostic, not an interceptor.
- `NativeFrameProbe.kt` + `frame_probe.cpp`: C++ validator with JNI.
- `Engine.kt`: seam for secure provider SDK integration.
- `docs/CAMERA_TEST_PLAN.md`: reproducible unrooted-device test procedure.

### Recommended next implementation steps

1. Reuse short-lived backend auth and provider SDK integrations from `Morphlysm2` (XMax and Vidu; no hard-coded keys).
2. Send provider **output** frames into an application-owned `Surface` and benchmark correct fps/orientation.
3. Port the virtual-camera sandbox layer in isolation; validate with a controlled Camera1/Camera2 test app inside BlackBox before trying any third-party messenger.
4. Fix Android 14/15 virtual permissions and app lifecycle based on logs and real-device testing, not API bypass assumptions.

Media uploads are kept local via Android's ContentResolver and are not sent to providers in this alpha.
