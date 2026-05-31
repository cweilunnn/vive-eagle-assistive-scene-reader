package tw.edu.nchu.viveeagle.assistivereader.eagle;

import android.content.Context;

import java.util.Locale;

public class ViveEagleSdkAdapter implements EagleSdkAdapter {
    public ViveEagleSdkAdapter(Context context) {
        // Add the official VIVE AI Glasses SDK to app/libs, then replace this placeholder
        // with ViveGlass, ViveGlassKit, and ViveGlassClientCallback wiring.
    }

    @Override
    public void connect(EagleEventListener listener) {
        listener.onError("ViveEagleSdkAdapter 尚未接上官方 SDK。請先跑通官方 Android sample，再依 docs/sdk-integration-notes.md 接線。", null);
    }

    @Override
    public void disconnect() {
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void captureImage(CaptureQuality quality) {
    }

    @Override
    public void speakText(String text, Locale locale) {
    }

    @Override
    public void release() {
    }
}

