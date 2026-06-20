package tw.edu.nchu.viveeagle.assistivereader;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tw.edu.nchu.viveeagle.assistivereader.eagle.CaptureQuality;
import tw.edu.nchu.viveeagle.assistivereader.eagle.ConnectionState;
import tw.edu.nchu.viveeagle.assistivereader.eagle.EagleAdapterFactory;
import tw.edu.nchu.viveeagle.assistivereader.eagle.EagleEventListener;
import tw.edu.nchu.viveeagle.assistivereader.eagle.EagleSdkAdapter;
import tw.edu.nchu.viveeagle.assistivereader.vision.MockSceneAnalyzer;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneAnalyzer;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneAnalyzerFactory;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneAnalysisResult;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneRiskLevel;

public class MainActivity extends Activity implements EagleEventListener {
    private static final int REQUEST_PERMISSIONS = 1101;
    private static final int REQUEST_PICK_IMAGE = 1102;

    private EagleSdkAdapter eagleAdapter;
    private SceneAnalyzer sceneAnalyzer;
    private final MockSceneAnalyzer mockFallbackAnalyzer = new MockSceneAnalyzer();
    private TextView connectionStatus;
    private TextView permissionStatus;
    private TextView analysisModeStatus;
    private TextView primaryResult;
    private TextView detailResult;
    private TextView eventLog;
    private ImageView capturedImage;
    private Button connectButton;
    private Button captureButton;
    private Button selectPhotoButton;
    private Button speakButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eagleAdapter = EagleAdapterFactory.create(this);
        sceneAnalyzer = SceneAnalyzerFactory.create();
        buildHighContrastUi();
        refreshPermissionStatus();
    }

    @Override
    protected void onDestroy() {
        eagleAdapter.release();
        sceneAnalyzer.release();
        mockFallbackAnalyzer.release();
        super.onDestroy();
    }

    private void buildHighContrastUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(28));
        scrollView.addView(root);

        TextView title = label("VIVE Eagle Assistive Scene Reader", 28, Color.WHITE);
        title.setGravity(Gravity.START);
        root.addView(title);

        connectionStatus = label("連線狀態：尚未連線", 24, Color.YELLOW);
        root.addView(connectionStatus);

        permissionStatus = label("", 20, Color.WHITE);
        root.addView(permissionStatus);

        analysisModeStatus = label(
                "分析模式：" + sceneAnalyzer.displayName(),
                20,
                sceneAnalyzer.isRealAi() ? Color.GREEN : Color.YELLOW
        );
        root.addView(analysisModeStatus);

        connectButton = actionButton("連線眼鏡");
        connectButton.setOnClickListener(view -> connectToGlasses());
        root.addView(connectButton);

        captureButton = actionButton("模擬眼鏡拍照");
        captureButton.setOnClickListener(view -> captureFromGlasses("手機按鈕"));
        root.addView(captureButton);

        selectPhotoButton = actionButton("選擇真實照片給 AI");
        selectPhotoButton.setOnClickListener(view -> selectPhotoForAi());
        root.addView(selectPhotoButton);

        speakButton = actionButton("重念提醒");
        speakButton.setOnClickListener(view -> speakCurrentResult());
        root.addView(speakButton);

        primaryResult = label("等待拍照。按眼鏡按鍵或手機按鈕開始。", 32, Color.WHITE);
        primaryResult.setGravity(Gravity.START);
        root.addView(primaryResult);

        detailResult = label(
                sceneAnalyzer.isRealAi()
                        ? "可選擇手機中的真實照片交給 Gemini AI 分析。AI 只提供輔助提醒，不可取代白手杖或人類判斷。"
                        : "尚未設定 Gemini API key，目前使用 Mock 預設情境。",
                22,
                Color.WHITE
        );
        root.addView(detailResult);

        capturedImage = new ImageView(this);
        capturedImage.setBackgroundColor(Color.DKGRAY);
        capturedImage.setContentDescription("最近一次眼鏡拍攝的照片");
        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        capturedImage.setAdjustViewBounds(true);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
        );
        imageParams.setMargins(0, dp(12), 0, dp(12));
        capturedImage.setLayoutParams(imageParams);
        root.addView(capturedImage);

        eventLog = label("事件紀錄：\n", 18, Color.LTGRAY);
        root.addView(eventLog);

        setContentView(scrollView);
    }

    private TextView label(String text, int sp, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setLineSpacing(0, 1.15f);
        textView.setPadding(0, dp(10), 0, dp(10));
        return textView;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(24);
        button.setTextColor(Color.BLACK);
        button.setBackgroundColor(Color.YELLOW);
        button.setAllCaps(false);
        button.setMinHeight(dp(72));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(10), 0, dp(10));
        button.setLayoutParams(params);
        return button;
    }

    private void connectToGlasses() {
        if (!hasRequiredPermissions()) {
            requestNeededPermissions();
            return;
        }
        if (!isBluetoothEnabled()) {
            appendLog("請先開啟手機 Bluetooth。");
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            return;
        }
        if (!isLocationEnabled()) {
            appendLog("Android 10 藍牙掃描通常需要開啟定位服務。");
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        appendLog("呼叫 connect()");
        eagleAdapter.connect(this);
    }

    private void captureFromGlasses(String trigger) {
        if (!eagleAdapter.isConnected()) {
            appendLog(trigger + "：尚未連線，先執行 connect()");
            connectToGlasses();
            return;
        }
        primaryResult.setText("正在拍照，請保持手機與眼鏡連線。");
        detailResult.setText("觸發來源：" + trigger + "\n等待 onImageCaptured()。");
        appendLog(trigger + "：呼叫 captureImage()");
        eagleAdapter.captureImage(CaptureQuality.MEDIUM);
    }

    private void speakCurrentResult() {
        String text = primaryResult.getText().toString();
        appendLog("呼叫 speakText()");
        eagleAdapter.speakText(text, Locale.TAIWAN);
    }

    private void selectPhotoForAi() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_IMAGE || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri imageUri = data.getData();
        if (imageUri == null) {
            return;
        }

        try {
            String mimeType = getContentResolver().getType(imageUri);
            byte[] imageBytes;
            try (InputStream input = getContentResolver().openInputStream(imageUri)) {
                imageBytes = readBytes(input);
            }
            appendLog("已選擇真實照片：" + imageBytes.length + " bytes");
            processImage(imageBytes, mimeType == null ? "image/jpeg" : mimeType);
        } catch (Exception exception) {
            onError("讀取照片失敗：" + exception.getMessage(), exception);
        }
    }

    private byte[] readBytes(InputStream input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("無法開啟照片");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private void refreshPermissionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("權限狀態：");
        status.append(hasRequiredPermissions() ? "已授權" : "需要授權");
        status.append("\nBluetooth：").append(isBluetoothEnabled() ? "已開啟" : "未開啟");
        status.append("\nLocation：").append(isLocationEnabled() ? "已開啟" : "未開啟");
        permissionStatus.setText(status.toString());

        if (!hasRequiredPermissions()) {
            requestNeededPermissions();
        }
    }

    private boolean hasRequiredPermissions() {
        for (String permission : requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestNeededPermissions() {
        List<String> missing = new ArrayList<>();
        for (String permission : requiredPermissions()) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private List<String> requiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        return permissions;
    }

    private boolean isBluetoothEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return adapter != null && adapter.isEnabled();
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            refreshPermissionStatus();
        }
    }

    @Override
    public void onConnectionStateChanged(ConnectionState state) {
        runOnUiThread(() -> {
            connectionStatus.setText("連線狀態：" + state.displayName());
            connectionStatus.setTextColor(state == ConnectionState.CONNECTED ? Color.GREEN : Color.YELLOW);
            appendLog("onConnectionStateChanged(" + state.name() + ")");
        });
    }

    @Override
    public void onKeyEvent(String keyName) {
        runOnUiThread(() -> {
            appendLog("onKeyEvent(" + keyName + ")");
            captureFromGlasses("眼鏡按鍵");
        });
    }

    @Override
    public void onImageCaptured(byte[] imageBytes) {
        runOnUiThread(() -> {
            appendLog("onImageCaptured(" + imageBytes.length + " bytes)");
            processImage(imageBytes, "image/jpeg");
        });
    }

    private void processImage(byte[] imageBytes, String mimeType) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap == null) {
            onError("照片格式無法顯示。", null);
            return;
        }
        capturedImage.setImageBitmap(bitmap);
        primaryResult.setText(sceneAnalyzer.isRealAi() ? "AI 正在分析照片…" : "Mock 正在產生情境…");
        primaryResult.setTextColor(Color.YELLOW);
        detailResult.setText("請稍候。分析模式：" + sceneAnalyzer.displayName());
        setAnalysisBusy(true);
        appendLog("開始分析：" + sceneAnalyzer.displayName());

        sceneAnalyzer.analyze(imageBytes, mimeType, new SceneAnalyzer.Callback() {
            @Override
            public void onSuccess(SceneAnalysisResult result) {
                runOnUiThread(() -> {
                    setAnalysisBusy(false);
                    analysisModeStatus.setText("分析模式：" + sceneAnalyzer.displayName());
                    analysisModeStatus.setTextColor(sceneAnalyzer.isRealAi() ? Color.GREEN : Color.YELLOW);
                    applyAnalysisResult(result);
                    appendLog("分析完成：" + result.riskLevel.name());
                });
            }

            @Override
            public void onError(String message, Throwable throwable) {
                runOnUiThread(() -> {
                    appendLog(message);
                    analysisModeStatus.setText("AI 失敗，本次改用 Mock 結果");
                    analysisModeStatus.setTextColor(Color.RED);
                    mockFallbackAnalyzer.analyze(imageBytes, mimeType, new SceneAnalyzer.Callback() {
                        @Override
                        public void onSuccess(SceneAnalysisResult result) {
                            runOnUiThread(() -> {
                                setAnalysisBusy(false);
                                SceneAnalysisResult fallback = new SceneAnalysisResult(
                                        result.riskLevel,
                                        result.headline,
                                        "注意：Gemini AI 呼叫失敗，本次為 Mock fallback。\n" + result.detail,
                                        result.spokenText
                                );
                                applyAnalysisResult(fallback);
                            });
                        }

                        @Override
                        public void onError(String ignored, Throwable ignoredThrowable) {
                            runOnUiThread(() -> {
                                setAnalysisBusy(false);
                                MainActivity.this.onError(message, throwable);
                            });
                        }
                    });
                });
            }
        });
    }

    private void applyAnalysisResult(SceneAnalysisResult result) {
        primaryResult.setText(result.headline);
        primaryResult.setTextColor(colorForRisk(result.riskLevel));
        detailResult.setText(result.detail);
        eagleAdapter.speakText(result.spokenText, Locale.TAIWAN);
    }

    private void setAnalysisBusy(boolean busy) {
        captureButton.setEnabled(!busy);
        selectPhotoButton.setEnabled(!busy);
    }

    @Override
    public void onTextSpoken(String eventName) {
        runOnUiThread(() -> appendLog("onTextSpoken(" + eventName + ")"));
    }

    @Override
    public void onError(String message, Throwable throwable) {
        runOnUiThread(() -> {
            primaryResult.setText("發生錯誤，請停在安全位置。");
            detailResult.setText(message);
            appendLog("Error: " + message);
        });
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.TAIWAN).format(new Date());
        eventLog.append(time + "  " + message + "\n");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int colorForRisk(SceneRiskLevel riskLevel) {
        if (riskLevel == SceneRiskLevel.SAFE) {
            return Color.GREEN;
        }
        if (riskLevel == SceneRiskLevel.CAUTION) {
            return Color.YELLOW;
        }
        return Color.RED;
    }
}
