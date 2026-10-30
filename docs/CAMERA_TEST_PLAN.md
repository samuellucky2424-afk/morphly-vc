# Non-root Android camera compatibility test — alpha 0.1

## What the test actually demonstrates

1. Android Storage Access Framework can select and persist a local video URI.
2. Android's native VideoView/MediaPlayer can decode and render it.
3. A user-granted `SYSTEM_ALERT_WINDOW` permission can permit a movable `TYPE_APPLICATION_OVERLAY` mini-video player.
4. Camera2 enumerates physical cameras and can open/close one with normal CAMERA permission.
5. A read-only probe reports whether hidden Android Camera2 reflection is blocked by platform restrictions. This is not a hook.
6. JNI loads a C++ frame validator and detects malformed NV21 buffers.

**None of these by itself creates or intercepts a camera device.** A non-root, unprivileged Android app cannot generally register a system-wide Camera2 camera visible to arbitrary WhatsApp or Instagram processes. A compatibility layer inside an app-virtualization sandbox is a separate, experimental engineering effort.

## Test matrix

| Test | Android 13 | Android 14 | Android 15 | Android 16 |
|---|---|---|---|---|
| SAF video upload | to test | to test | to test | to test |
| Local video preview | to test | to test | to test | to test |
| Overlay permission + mini-window | to test | to test | to test | to test |
| Camera2 permission granted | to test | to test | to test | to test |
| Camera2 physical open/close | to test | to test | to test | to test |
| Camera2 virtual-app interception | not implemented | not implemented | not implemented | not implemented |
| Camera permission round trip inside cloned app | not implemented | not implemented | not implemented | not implemented |
| Frames visible to cloned call | not implemented | not implemented | not implemented | not implemented |

## How to run

- Install the debug APK on a *non-rooted* Android 14 or Android 15 device.
- Tap **Upload video**, choose an H.264 MP4, verify it plays in the dark preview.
- Tap **Pop-out window**, grant Display over other apps, return and tap again. Verify it appears, drags, closes, and continues when you leave the app.
- Tap **Camera test**, grant the camera permission, press Test camera. Record the physical camera open/close result. This is not a virtual-camera test.
- Capture logs with `adb logcat -d -v threadtime | grep -E 'Morphly|CameraService|CameraManager'` or Windows `adb logcat -d -v threadtime > camera-probe.txt`.
- Check battery optimization and foreground-service restrictions if the overlay is stopped in the background.

## Real injection experiment that follows

The previous Morphlysm2 project contains a BlackBox core with `VirtualCameraServiceHook`, `VirtualCameraSurfaceWorker`, `HookMain`, `XmaxRealtimeBridge`, and `ViduRealtimeBridge`. Do **not** assume these work on Android 14/15 just because the code exists. In an isolated branch or module:

1. First use a harmless app we control (a Camera1/Camera2 capture test consumer) *inside the same virtualized process*.
2. Measure where permission requests originate and terminate, including host UID, guest UID, AppOps, package visibility, and virtual UID. Never fake successful permissions when the actual host permission is denied.
3. Hook only the virtualized test process; validate surfaces and frame formats before moving to third-party apps.
4. Test old and new capture APIs plus frame pacing, orientation and lifecycle. Avoid hooking the system-wide camera service from an ordinary app.
5. Consider third-party integrations only where the host application permits them, user consent is present, and test evidence shows compatibility.

**Stop condition:** if the virtualized test consumer cannot obtain actual camera frames and repeatably pass permission checks, don't integrate WhatsApp/Instagram yet.
