package tw.edu.nchu.viveeagle.assistivereader.eagle;

public interface EagleEventListener {
    void onConnectionStateChanged(ConnectionState state);

    void onKeyEvent(String keyName);

    void onImageCaptured(byte[] imageBytes);

    void onTextSpoken(String eventName);

    void onError(String message, Throwable throwable);
}

