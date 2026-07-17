package tw.edu.nchu.viveeagle.assistivereader.eagle;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;

import tw.edu.nchu.viveeagle.assistivereader.R;

public class MockEagleSdkAdapter implements EagleSdkAdapter {
    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] demoStreetImages = new int[] {
            R.drawable.demo_street_safe,
            R.drawable.demo_street_scooter,
            R.drawable.demo_street_construction,
            R.drawable.demo_street_drop
    };

    private TextToSpeech textToSpeech;
    private EagleEventListener listener;
    private boolean connected;
    private boolean textToSpeechReady;
    private String pendingSpeech;
    private Locale pendingLocale;
    private int demoImageIndex;

    public MockEagleSdkAdapter(Context context) {
        this.context = context.getApplicationContext();
        textToSpeech = new TextToSpeech(this.context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int languageResult = textToSpeech.setLanguage(Locale.TAIWAN);
                textToSpeechReady = languageResult != TextToSpeech.LANG_MISSING_DATA
                        && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
                if (textToSpeechReady && pendingSpeech != null) {
                    speakText(pendingSpeech, pendingLocale == null ? Locale.TAIWAN : pendingLocale);
                    pendingSpeech = null;
                    pendingLocale = null;
                } else if (!textToSpeechReady && listener != null) {
                    listener.onError("Text-to-Speech 不支援繁體中文，請檢查手機語音服務。", null);
                }
            } else if (listener != null) {
                listener.onError("Text-to-Speech 初始化失敗，無法播放提醒。", null);
            }
        });
    }

    @Override
    public void connect(EagleEventListener listener) {
        this.listener = listener;
        listener.onConnectionStateChanged(ConnectionState.CONNECTING);
        handler.postDelayed(() -> {
            connected = true;
            listener.onConnectionStateChanged(ConnectionState.CONNECTED);
        }, 700);
    }

    @Override
    public void disconnect() {
        connected = false;
        if (listener != null) {
            listener.onConnectionStateChanged(ConnectionState.DISCONNECTED);
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void captureImage(CaptureQuality quality) {
        if (!connected || listener == null) {
            return;
        }
        handler.postDelayed(() -> {
            try {
                int imageResource = demoStreetImages[demoImageIndex];
                demoImageIndex = (demoImageIndex + 1) % demoStreetImages.length;
                listener.onImageCaptured(readResourceBytes(imageResource));
            } catch (Exception exception) {
                listener.onError("讀取內建街景照片失敗：" + exception.getMessage(), exception);
            }
        }, 500);
    }

    private byte[] readResourceBytes(int resourceId) throws Exception {
        Resources resources = context.getResources();
        try (InputStream input = resources.openRawResource(resourceId);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    @Override
    public void speakText(String text, Locale locale) {
        if (!textToSpeechReady) {
            pendingSpeech = text;
            pendingLocale = locale;
            if (listener != null) {
                listener.onTextSpoken("TTS_PENDING_INIT");
            }
            return;
        }
        if (textToSpeech != null) {
            textToSpeech.setLanguage(locale);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scene-reader-alert");
        }
        if (listener != null) {
            listener.onTextSpoken("TTS_DONE");
        }
    }

    @Override
    public void release() {
        disconnect();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
    }
}
