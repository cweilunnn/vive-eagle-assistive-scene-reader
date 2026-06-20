# VIVE Eagle Assistive Scene Reader

Android real-device demo for VIVE Eagle / VIVE AI Glasses SDK accessibility testing.

The first milestone is a stable photo-based flow, not video streaming:

1. Android app connects to VIVE Eagle.
2. Glasses key event triggers image capture.
3. App receives the captured image.
4. App sends the image to AI vision, or uses a mock result.
5. App calls `speakText()` with a short safety message.
6. Phone screen shows the same result with high-contrast, large text.

This repository currently contains a native Android Studio project with a mock adapter that runs without the official SDK. The real SDK adapter is intentionally isolated so the UI and accessibility flow can be tested before wiring the hardware APIs.

The app now also includes an optional Gemini AI vision analyzer. With a local API key, a user can select a real phone photo and receive an actual model-generated risk assessment. Without a key, or when the API fails, the UI clearly identifies the Mock fallback. See `docs/gemini-ai-setup.md`.

## Current Workspace Note

The Codex workspace checked in this session is:

`C:\Users\USER\OneDrive - mail.nchu.edu.tw\文件\VIVE EAGLE`

It was empty when inspected. The originally mentioned folder:

`C:\Users\shao_\Documents\VIVE Eagle`

does not exist in this sandboxed Windows environment, so the Android project was created in the active workspace.

## Windows + Android Studio Setup

1. Install Android Studio on Windows.
2. In Android Studio, install:
   - Android SDK Platform for Android 10/API 29 or newer.
   - Android SDK Build-Tools.
   - Android SDK Platform-Tools.
   - Android USB Driver if Android Studio offers it.
3. On the ASUS phone:
   - Enable Developer options.
   - Enable USB debugging.
   - Keep Bluetooth enabled.
   - Keep Location enabled for Android 10 Bluetooth scanning behavior.
   - Confirm the VIVE Eagle glasses are already paired/usable with the phone.
4. Connect the ASUS phone by USB.
5. In Android Studio, open this folder and wait for Gradle sync.
6. Select the physical ASUS device as the run target.
7. Run the `app` configuration.

This project uses Android Gradle Plugin 8.5.2 and `compileSdk 35`. Android Studio's bundled JDK 17 is expected.

If the device does not appear, run Android Studio's Device Manager troubleshooting or check:

```powershell
adb devices
```

The current Codex shell did not have `adb`, `java`, `gradle`, Android Studio, or an Android SDK installed in PATH, so building/installing could not be verified inside this session.

## Official SDK Sample First

Before changing the demo app to the real SDK, validate the official Android sample on the ASUS phone:

1. Download the Android SDK from the official latest release page.
2. Extract it outside OneDrive if possible, for example:
   `C:\Dev\VIVE-AI-Glasses-SDK`
3. Open the official Android sample in Android Studio.
4. Confirm Gradle sync succeeds.
5. Confirm the sample package/application registration requirements from HTC are complete.
6. Run on the ASUS physical device, not an emulator.
7. Grant runtime permissions for Camera, Microphone, and Location.
8. Verify the SDK callbacks:
   - `connect()`
   - `onConnectionStateChanged()`
   - `onKeyEvent()`
   - `captureImage()`
   - `onImageCaptured()`
   - `speakText()`

Only after this sample works should the real adapter in this project be wired to the same API calls.

## Android 10 Permission Notes

For Android 10, Bluetooth discovery/scanning commonly requires location permission and Location services to be turned on. This app requests:

- Camera
- Microphone
- Fine location
- Bluetooth permissions for Android 10 and newer Android versions

The first screen shows a clear warning if Location services appear disabled.

## Adapter Architecture

The app depends on `EagleSdkAdapter`.

- `MockEagleSdkAdapter`: default implementation. Simulates connect, glasses key event, photo capture, image callback, and speech output.
- `ViveEagleSdkAdapter`: placeholder for the official VIVE AI Glasses SDK. It is isolated so the app still builds before the official SDK is copied into the project.

When the official SDK is available, update `EagleAdapterFactory` to return `ViveEagleSdkAdapter` after adding the SDK dependency.

Official references checked on 2026-05-31. Note: the official latest-release page currently lists SDK 0.5.0 with release date 2026-06-01, which is one calendar day after this session date in the provided environment:

- Android docs: https://developer.vive.com/resources/vive-ai-glasses-sdk/documentation/android/
- Android setup: https://developer.vive.com/resources/vive-ai-glasses-sdk/documentation/android/setup/
- Latest release: https://developer.vive.com/resources/vive-ai-glasses-sdk/download/latest-release/
