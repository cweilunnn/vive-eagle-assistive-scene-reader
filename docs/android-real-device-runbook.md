# Android Real-Device Runbook

Use this checklist for ASUS Android 10 + VIVE Eagle field testing.

## Phase 1: Toolchain

1. Install Android Studio.
2. Install SDK Platform Tools.
3. Connect ASUS by USB.
4. Confirm `adb devices` shows `device`, not `unauthorized`.
5. In Android Studio, select the ASUS phone as the run target.

## Phase 2: Official SDK Sample

1. Download the official Android SDK package.
2. Open the official sample project.
3. Register/configure the app exactly as the SDK requires.
4. Run on the ASUS phone.
5. Grant Camera, Microphone, and Location.
6. Keep Bluetooth and Location enabled.
7. Verify connection and callback logs.

Acceptance checks:

- `connect()` starts without permission denial.
- `onConnectionStateChanged()` reaches connected.
- Pressing the glasses key produces `onKeyEvent()`.
- `captureImage()` returns `onImageCaptured()`.
- `speakText()` produces audio on the glasses.

## Phase 3: Assistive Scene Reader Mock App

1. Open this repository in Android Studio.
2. Run the `app` module on the ASUS phone.
3. Grant permissions.
4. Tap `連線眼鏡`.
5. Confirm mock connection state changes to connected.
6. Wait for the mock key event or tap `拍照辨識`.
7. Confirm the app displays a large high-contrast safety result.
8. Confirm phone TTS speaks the mock alert.

## Phase 4: Real SDK Adapter

1. Copy official SDK artifacts into `app/libs`.
2. Add the dependency in `app/build.gradle`.
3. Replace `ViveEagleSdkAdapter` placeholder code with real SDK calls.
4. Switch `EagleAdapterFactory` from `MockEagleSdkAdapter` to `ViveEagleSdkAdapter`.
5. Re-run on ASUS.
6. Compare logs with the official sample.

## Field Test Safety

Do the first outdoor test with a sighted assistant. Keep the first version conservative: one photo per key press, short spoken warnings, and no continuous video stream.

