package tw.edu.nchu.viveeagle.assistivereader.eagle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class MockEagleSdkAdapter implements EagleSdkAdapter {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech textToSpeech;
    private EagleEventListener listener;
    private boolean connected;
    private boolean textToSpeechReady;
    private String pendingSpeech;
    private Locale pendingLocale;
    private int mockSceneIndex;

    public MockEagleSdkAdapter(Context context) {
        textToSpeech = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int languageResult = textToSpeech.setLanguage(Locale.TAIWAN);
                textToSpeechReady = languageResult != TextToSpeech.LANG_MISSING_DATA
                        && languageResult != TextToSpeech.LANG_NOT_SUPPORTED;
                if (textToSpeechReady && pendingSpeech != null) {
                    speakText(pendingSpeech, pendingLocale == null ? Locale.TAIWAN : pendingLocale);
                    pendingSpeech = null;
                    pendingLocale = null;
                } else if (!textToSpeechReady && listener != null) {
                    listener.onError("手機 Text-to-Speech 不支援繁體中文或缺少語音資料。", null);
                }
            } else if (listener != null) {
                listener.onError("手機 Text-to-Speech 初始化失敗。請檢查系統語音服務。", null);
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
            handler.postDelayed(() -> {
                if (connected && this.listener != null) {
                    this.listener.onKeyEvent("MOCK_CAMERA_KEY");
                }
            }, 1800);
        }, 900);
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
            byte[] mockImage = createMockStreetImage(mockSceneIndex);
            mockSceneIndex = (mockSceneIndex + 1) % 4;
            listener.onImageCaptured(mockImage);
        }, 900);
    }

    private byte[] createMockStreetImage(int sceneIndex) {
        int width = 960;
        int height = 1280;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        canvas.drawColor(Color.rgb(32, 36, 40));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(74, 82, 90));
        canvas.drawRect(0, 0, width, height, paint);

        paint.setColor(Color.rgb(105, 112, 118));
        canvas.drawRect(0, 560, width, height, paint);

        paint.setColor(Color.rgb(42, 52, 48));
        canvas.drawRect(0, 560, 250, height, paint);

        paint.setColor(Color.rgb(225, 225, 210));
        for (int i = 0; i < 6; i++) {
            int top = 640 + i * 95;
            canvas.drawRect(410, top, 550, top + 42, paint);
        }

        if (sceneIndex == 0) {
            paint.setColor(Color.rgb(40, 190, 120));
            canvas.drawRect(640, 700, 820, 900, paint);
            drawSceneLabel(canvas, paint, "SAFE", Color.rgb(0, 208, 132));
        } else if (sceneIndex == 1) {
            paint.setColor(Color.rgb(230, 70, 60));
            canvas.drawRect(120, 720, 300, 910, paint);
            paint.setColor(Color.rgb(30, 30, 30));
            canvas.drawCircle(155, 930, 38, paint);
            canvas.drawCircle(270, 930, 38, paint);
            drawSceneLabel(canvas, paint, "CAUTION", Color.rgb(255, 212, 0));
        } else if (sceneIndex == 2) {
            paint.setColor(Color.rgb(255, 80, 80));
            canvas.drawRect(350, 760, 650, 1040, paint);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(14);
            canvas.drawLine(390, 800, 610, 1000, paint);
            canvas.drawLine(610, 800, 390, 1000, paint);
            drawSceneLabel(canvas, paint, "DANGER", Color.rgb(255, 77, 77));
        } else {
            paint.setColor(Color.rgb(150, 150, 150));
            for (int i = 0; i < 7; i++) {
                int top = 720 + i * 58;
                canvas.drawRect(220, top, 760, top + 28, paint);
            }
            drawSceneLabel(canvas, paint, "STAIRS", Color.rgb(255, 77, 77));
        }

        paint.setColor(Color.rgb(245, 200, 40));
        paint.setTextSize(48);
        paint.setFakeBoldText(true);
        canvas.drawText("MOCK CAPTURE", 48, 86, paint);

        paint.setColor(Color.WHITE);
        paint.setTextSize(34);
        paint.setFakeBoldText(false);
        canvas.drawText("Sidewalk + obstacle test scene", 48, 136, paint);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output);
        bitmap.recycle();
        return output.toByteArray();
    }

    private void drawSceneLabel(Canvas canvas, Paint paint, String label, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(48, 190, 430, 280, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(52);
        paint.setFakeBoldText(true);
        canvas.drawText(label, 74, 252, paint);
        paint.setFakeBoldText(false);
    }

    @Override
    public void speakText(String text, Locale locale) {
        if (!textToSpeechReady) {
            pendingSpeech = text;
            pendingLocale = locale;
            if (listener != null) {
                listener.onTextSpoken("MOCK_SPEAK_PENDING_TTS_INIT");
            }
            return;
        }
        if (textToSpeech != null) {
            textToSpeech.setLanguage(locale);
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "scene-reader-alert");
        }
        if (listener != null) {
            listener.onTextSpoken("MOCK_SPEAK_DONE");
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
