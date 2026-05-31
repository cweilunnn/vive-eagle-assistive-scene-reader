# VIVE AI Glasses SDK Integration Notes

Official Android setup says the app must register with the VIVE AI Glasses SDK, request Camera and Microphone permissions, create `ViveGlass`, assign `ViveGlassKit` as the adapter, then call `connect()` with `ViveGlassClientCallback`.

The official latest release page checked on 2026-05-31 lists VIVE AI Glasses SDK 0.5.0 with these important notes:

- Third-party apps connect directly to VIVE AI Glasses through Bluetooth.
- Third-party apps must request Camera and Microphone access before API invocation.

## Target Mapping

Map this app's adapter interface to the official SDK as follows:

| App interface | Official SDK concept |
| --- | --- |
| `EagleSdkAdapter.connect()` | `ViveGlass.connect(callback)` |
| `EagleEventListener.onConnectionStateChanged()` | `ViveGlassClientCallback.onConnectionStateChanged()` |
| `EagleEventListener.onKeyEvent()` | `ViveGlassClientCallback.onKeyEvent()` |
| `EagleSdkAdapter.captureImage()` | `ViveGlass.captureImage()` |
| `EagleEventListener.onImageCaptured()` | `ViveGlassClientCallback.onImageCaptured()` |
| `EagleSdkAdapter.speakText()` | `ViveGlass.speakText()` |
| `EagleEventListener.onTextSpoken()` | `ViveGlassClientCallback.onTextSpoken()` |

## Placeholder Replacement Sketch

After the official SDK is available, `ViveEagleSdkAdapter` should roughly become:

```java
public class ViveEagleSdkAdapter implements EagleSdkAdapter {
    private ViveGlass viveGlass;
    private EagleEventListener listener;

    public ViveEagleSdkAdapter(Context context) {
        ViveGlass.adapter = new ViveGlassKit(context.getApplicationContext());
        viveGlass = new ViveGlass();
    }

    @Override
    public void connect(EagleEventListener listener) {
        this.listener = listener;
        viveGlass.connect(new ViveGlassClientCallback() {
            @Override
            public void onConnectionStateChanged(ConnectionState state) {
                listener.onConnectionStateChanged(mapConnectionState(state));
            }

            @Override
            public void onImageCaptured(CaptureEvent event, ImagePayload payload) {
                listener.onImageCaptured(extractBytes(payload));
            }

            @Override
            public void onTextSpoken(SynthesisEvent event) {
                listener.onTextSpoken(event.name());
            }

            @Override
            public void onKeyEvent(KeyEvent event) {
                listener.onKeyEvent(event.name());
            }
        });
    }
}
```

Use the exact package names, method signatures, payload byte accessors, and enum names from the SDK version you download. Keep this app's `ConnectionState` enum separate so UI code does not depend directly on HTC SDK types.

