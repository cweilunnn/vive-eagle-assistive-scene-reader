package tw.edu.nchu.viveeagle.assistivereader.eagle;

import java.util.Locale;

public interface EagleSdkAdapter {
    void connect(EagleEventListener listener);

    void disconnect();

    boolean isConnected();

    void captureImage(CaptureQuality quality);

    void speakText(String text, Locale locale);

    void release();
}

