package tw.edu.nchu.viveeagle.assistivereader;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tw.edu.nchu.viveeagle.assistivereader.eagle.CaptureQuality;
import tw.edu.nchu.viveeagle.assistivereader.eagle.ConnectionState;
import tw.edu.nchu.viveeagle.assistivereader.eagle.EagleAdapterFactory;
import tw.edu.nchu.viveeagle.assistivereader.eagle.EagleEventListener;
import tw.edu.nchu.viveeagle.assistivereader.eagle.EagleSdkAdapter;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneAnalysisResult;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneAnalyzer;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneAnalyzerFactory;
import tw.edu.nchu.viveeagle.assistivereader.vision.SceneRiskLevel;

public class MainActivity extends Activity implements EagleEventListener {
    private static final int REQUEST_PERMISSIONS = 1101;
    private static final int REQUEST_PICK_IMAGE = 1102;

    private final int[] demoStreetImages = new int[] {
            R.drawable.demo_street_safe,
            R.drawable.demo_street_scooter,
            R.drawable.demo_street_construction,
            R.drawable.demo_street_drop
    };

    private EagleSdkAdapter eagleAdapter;
    private SceneAnalyzer sceneAnalyzer;

    private TextView connectionStatus;
    private TextView permissionStatus;
    private TextView analysisModeStatus;
    private TextView primaryResult;
    private TextView detailResult;
    private TextView fullResponseResult;
    private TextView eventLog;
    private ImageView capturedImage;
    private Button latestGalleryButton;
    private Button demoButton;
    private Button captureButton;
    private Button selectPhotoButton;
    private String lastSpokenText = "尚未有分析結果";
    private int demoImageIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eagleAdapter = EagleAdapterFactory.create(this);
        sceneAnalyzer = SceneAnalyzerFactory.create();
        buildUi();
        refreshPermissionStatus();
    }

    @Override
    protected void onDestroy() {
        eagleAdapter.release();
        sceneAnalyzer.release();
        super.onDestroy();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.BLACK);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(28));
        scrollView.addView(root);

        TextView title = label("智慧眼鏡路況提醒 Demo", 28, Color.WHITE);
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView oneStepHint = label("主流程只要一鍵：眼鏡拍照並存到手機相簿後，按第一顆按鈕分析最新照片。", 18, Color.LTGRAY);
        root.addView(oneStepHint);

        latestGalleryButton = primaryButton("一鍵分析最新相簿照片");
        latestGalleryButton.setOnClickListener(view -> analyzeLatestGalleryPhoto());
        root.addView(latestGalleryButton);

        demoButton = secondaryButton("一鍵示範內建街景照片");
        demoButton.setOnClickListener(view -> analyzeNextDemoPhoto());
        root.addView(demoButton);

        connectionStatus = label("眼鏡連線：未連線", 20, Color.YELLOW);
        root.addView(connectionStatus);

        permissionStatus = label("", 16, Color.WHITE);
        root.addView(permissionStatus);

        analysisModeStatus = label(analysisModeText(), 18, sceneAnalyzer.isRealAi() ? Color.GREEN : Color.YELLOW);
        root.addView(analysisModeStatus);

        Button connectButton = compactButton("連線眼鏡");
        connectButton.setOnClickListener(view -> connectToGlasses());
        root.addView(connectButton);

        captureButton = compactButton("從眼鏡取照分析");
        captureButton.setOnClickListener(view -> captureFromGlasses("眼鏡即時取照"));
        root.addView(captureButton);

        selectPhotoButton = compactButton("手動選照片");
        selectPhotoButton.setOnClickListener(view -> selectPhotoForAi());
        root.addView(selectPhotoButton);

        Button speakButton = compactButton("重念提醒");
        speakButton.setOnClickListener(view -> speakCurrentResult());
        root.addView(speakButton);

        capturedImage = new ImageView(this);
        capturedImage.setBackgroundColor(Color.rgb(24, 24, 24));
        capturedImage.setContentDescription("目前分析照片");
        capturedImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)
        );
        imageParams.setMargins(0, dp(14), 0, dp(10));
        capturedImage.setLayoutParams(imageParams);
        root.addView(capturedImage);

        primaryResult = label("等待照片", 36, Color.WHITE);
        primaryResult.setGravity(Gravity.START);
        root.addView(primaryResult);

        detailResult = label("建議 Demo 用法：先用眼鏡官方拍照功能讓照片進手機相簿，再回到本 App 按「一鍵分析最新相簿照片」。", 20, Color.LTGRAY);
        root.addView(detailResult);

        fullResponseResult = label("Gemini 完整回覆：\n尚未分析", 16, Color.rgb(180, 180, 180));
        root.addView(fullResponseResult);

        eventLog = label("事件紀錄：\n", 15, Color.GRAY);
        root.addView(eventLog);

        setContentView(scrollView);
    }

    private TextView label(String text, int sp, int color) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        textView.setLineSpacing(0, 1.12f);
        textView.setPadding(0, dp(7), 0, dp(7));
        return textView;
    }

    private Button primaryButton(String text) {
        Button button = baseButton(text, 24, Color.BLACK, Color.rgb(255, 235, 59), dp(76));
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = baseButton(text, 22, Color.WHITE, Color.rgb(45, 118, 255), dp(68));
        return button;
    }

    private Button compactButton(String text) {
        return baseButton(text, 18, Color.BLACK, Color.rgb(220, 220, 220), dp(52));
    }

    private Button baseButton(String text, int sp, int textColor, int backgroundColor, int minHeight) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(sp);
        button.setTextColor(textColor);
        button.setBackgroundColor(backgroundColor);
        button.setAllCaps(false);
        button.setMinHeight(minHeight);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(7), 0, dp(7));
        button.setLayoutParams(params);
        return button;
    }

    private void connectToGlasses() {
        if (!hasRequiredPermissions()) {
            requestNeededPermissions();
            return;
        }
        if (!isBluetoothEnabled()) {
            appendLog("請先開啟 Bluetooth");
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
            return;
        }
        if (!isLocationEnabled()) {
            appendLog("Android 10 掃描藍牙裝置需要開啟位置服務");
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        appendLog("connect()");
        eagleAdapter.connect(this);
    }

    private void captureFromGlasses(String trigger) {
        if (!eagleAdapter.isConnected()) {
            appendLog("眼鏡尚未連線，先嘗試連線");
            connectToGlasses();
            return;
        }
        primaryResult.setText("取照中...");
        primaryResult.setTextColor(Color.YELLOW);
        detailResult.setText("照片來源：" + trigger + "\n正在等待眼鏡照片。");
        fullResponseResult.setText("Gemini 完整回覆：\n等待分析中...");
        appendLog("captureImage() - " + trigger);
        eagleAdapter.captureImage(CaptureQuality.MEDIUM);
    }

    private void speakCurrentResult() {
        appendLog("speakText()");
        eagleAdapter.speakText(lastSpokenText, Locale.TAIWAN);
    }

    private void selectPhotoForAi() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void analyzeLatestGalleryPhoto() {
        if (!hasGalleryPermission()) {
            requestGalleryPermission();
            return;
        }

        Uri latestImageUri = findLatestGalleryImage();
        if (latestImageUri == null) {
            onError("找不到手機相簿照片。請先用眼鏡或手機拍一張照片，確認照片有存進相簿。", null);
            return;
        }

        try {
            String mimeType = getContentResolver().getType(latestImageUri);
            byte[] imageBytes;
            try (InputStream input = getContentResolver().openInputStream(latestImageUri)) {
                imageBytes = readBytes(input);
            }
            appendLog("讀取最新相簿照片：" + latestImageUri);
            processImage(imageBytes, mimeType == null ? "image/jpeg" : mimeType, "手機相簿最新照片");
        } catch (Exception exception) {
            onError("讀取最新相簿照片失敗：" + exception.getMessage(), exception);
        }
    }

    private void analyzeNextDemoPhoto() {
        try {
            int resourceId = demoStreetImages[demoImageIndex];
            demoImageIndex = (demoImageIndex + 1) % demoStreetImages.length;
            byte[] imageBytes = readResourceBytes(resourceId);
            appendLog("讀取內建示範街景照片");
            processImage(imageBytes, "image/png", "內建街景示範照片");
        } catch (Exception exception) {
            onError("讀取內建示範照片失敗：" + exception.getMessage(), exception);
        }
    }

    private Uri findLatestGalleryImage() {
        String[] projection = new String[] {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
        };

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor == null || !cursor.moveToFirst()) {
                return null;
            }
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            long id = cursor.getLong(idColumn);
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        }
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
            appendLog("讀取手動選取照片：" + imageBytes.length + " bytes");
            processImage(imageBytes, mimeType == null ? "image/jpeg" : mimeType, "手動選取照片");
        } catch (Exception exception) {
            onError("讀取手動選取照片失敗：" + exception.getMessage(), exception);
        }
    }

    private byte[] readResourceBytes(int resourceId) throws Exception {
        try (InputStream input = getResources().openRawResource(resourceId)) {
            return readBytes(input);
        }
    }

    private byte[] readBytes(InputStream input) throws Exception {
        if (input == null) {
            throw new IllegalArgumentException("照片資料是空的");
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
        status.append("照片權限：").append(hasGalleryPermission() ? "已允許" : "需要允許");
        status.append("\nBluetooth：").append(isBluetoothEnabled() ? "已開啟" : "未開啟");
        status.append("\nLocation：").append(isLocationEnabled() ? "已開啟" : "未開啟");
        permissionStatus.setText(status.toString());

        if (!hasGalleryPermission()) {
            requestGalleryPermission();
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

    private boolean hasGalleryPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestGalleryPermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { permission }, REQUEST_PERMISSIONS);
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
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
            connectionStatus.setText("眼鏡連線：" + state.displayName());
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
            processImage(imageBytes, "image/png", "眼鏡即時照片");
        });
    }

    private void processImage(byte[] imageBytes, String mimeType, String sourceLabel) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap == null) {
            onError("照片無法顯示，請換一張圖片再試。", null);
            return;
        }
        capturedImage.setImageBitmap(bitmap);
        primaryResult.setText("AI 分析中...");
        primaryResult.setTextColor(Color.YELLOW);
        detailResult.setText("照片來源：" + sourceLabel + "\n正在送給 Gemini 判斷路況。");
        fullResponseResult.setText("Gemini 完整回覆：\n分析中...");
        setAnalysisBusy(true);
        appendLog("analyze() - " + sceneAnalyzer.displayName());

        sceneAnalyzer.analyze(imageBytes, mimeType, new SceneAnalyzer.Callback() {
            @Override
            public void onSuccess(SceneAnalysisResult result) {
                runOnUiThread(() -> {
                    setAnalysisBusy(false);
                    analysisModeStatus.setText(analysisModeText());
                    analysisModeStatus.setTextColor(sceneAnalyzer.isRealAi() ? Color.GREEN : Color.YELLOW);
                    applyAnalysisResult(result, sourceLabel, sceneAnalyzer.displayName());
                    appendLog("analysis success: " + result.riskLevel.name());
                });
            }

            @Override
            public void onError(String message, Throwable throwable) {
                runOnUiThread(() -> {
                    setAnalysisBusy(false);
                    appendLog(message);
                    onError(message, throwable);
                });
            }
        });
    }

    private void applyAnalysisResult(SceneAnalysisResult result, String sourceLabel, String analyzerName) {
        primaryResult.setText(result.headline);
        primaryResult.setTextColor(colorForRisk(result.riskLevel));
        detailResult.setText(result.detail + "\n\n來源：" + sourceLabel + " | 分析：" + analyzerName);
        fullResponseResult.setText("Gemini 完整回覆：\n" + nonEmpty(result.fullResponse, "沒有額外完整回覆"));
        lastSpokenText = result.spokenText;
        eagleAdapter.speakText(result.spokenText, Locale.TAIWAN);
    }

    private String analysisModeText() {
        return "AI 模式：" + sceneAnalyzer.displayName();
    }

    private void setAnalysisBusy(boolean busy) {
        latestGalleryButton.setEnabled(!busy);
        demoButton.setEnabled(!busy);
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
            primaryResult.setText("無法完成分析");
            primaryResult.setTextColor(Color.RED);
            detailResult.setText(message);
            fullResponseResult.setText("Gemini 完整回覆：\n請查看事件紀錄確認錯誤。");
            appendLog("Error: " + message);
        });
    }

    private void appendLog(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.TAIWAN).format(new Date());
        eventLog.append(time + "  " + message + "\n");
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int colorForRisk(SceneRiskLevel riskLevel) {
        if (riskLevel == SceneRiskLevel.SAFE) {
            return Color.GREEN;
        }
        if (riskLevel == SceneRiskLevel.DANGER) {
            return Color.RED;
        }
        return Color.YELLOW;
    }
}
