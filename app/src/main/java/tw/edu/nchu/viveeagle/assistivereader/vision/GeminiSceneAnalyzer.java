package tw.edu.nchu.viveeagle.assistivereader.vision;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class GeminiSceneAnalyzer implements SceneAnalyzer {
    private static final String PROMPT =
            "你是給視障行人使用的第一視角街景安全判斷助手。"
                    + "請只根據照片中接下來 2 到 5 公尺的行走路徑判斷，不要描述無關背景。"
                    + "輸出繁體中文 JSON，不要 Markdown，不要多餘文字。"
                    + "riskLevel 只能是 SAFE、CAUTION、DANGER。"
                    + "headline 必須 14 個中文字以內，格式如「安全：可通行」、「注意：前方障礙」、「危險：請停下」。"
                    + "detail 必須 80 個中文字以內，說明障礙、方向與動作建議。"
                    + "spokenText 必須 36 個中文字以內，適合直接朗讀。"
                    + "若不確定，選 CAUTION。"
                    + "JSON 欄位：riskLevel, headline, detail, spokenText。";

    private final String apiKey;
    private final String model;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public GeminiSceneAnalyzer(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model == null || model.trim().isEmpty() ? "gemini-2.5-flash" : model.trim();
    }

    @Override
    public void analyze(byte[] imageBytes, String mimeType, Callback callback) {
        executor.execute(() -> {
            Exception lastException = null;
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    callback.onSuccess(requestAnalysis(imageBytes));
                    return;
                } catch (Exception exception) {
                    lastException = exception;
                    if (attempt == 1) {
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            callback.onError("AI 分析中斷：" + interruptedException.getMessage(), interruptedException);
                            return;
                        }
                    }
                }
            }
            callback.onError("AI 分析失敗：" + (lastException == null ? "未知錯誤" : lastException.getMessage()), lastException);
        });
    }

    private SceneAnalysisResult requestAnalysis(byte[] imageBytes) throws Exception {
        HttpURLConnection connection = null;
        try {
            byte[] preparedImage = prepareAsJpeg(imageBytes);
            String encodedImage = Base64.encodeToString(preparedImage, Base64.NO_WRAP);

            JSONObject textPart = new JSONObject().put("text", PROMPT);
            JSONObject imagePart = new JSONObject().put(
                    "inline_data",
                    new JSONObject()
                            .put("mime_type", "image/jpeg")
                            .put("data", encodedImage)
            );
            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray().put(
                            new JSONObject().put("parts", new JSONArray().put(textPart).put(imagePart))
                    ))
                    .put("generationConfig", new JSONObject()
                            .put("temperature", 0.1)
                            .put("responseMimeType", "application/json"));

            URL url = new URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + model + ":generateContent?key=" + apiKey
            );
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(25_000);
            connection.setReadTimeout(60_000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String response = readFully(stream);
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("Gemini API HTTP " + status + ": " + compact(response));
            }

            return parseResult(response);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private SceneAnalysisResult parseResult(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        String text = root.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        }

        JSONObject result = new JSONObject(text);
        SceneRiskLevel riskLevel;
        try {
            riskLevel = SceneRiskLevel.valueOf(
                    result.optString("riskLevel", "CAUTION").toUpperCase(Locale.US)
            );
        } catch (IllegalArgumentException ignored) {
            riskLevel = SceneRiskLevel.CAUTION;
        }

        return new SceneAnalysisResult(
                riskLevel,
                limit(nonEmpty(result.optString("headline"), fallbackHeadline(riskLevel)), 18),
                limit(nonEmpty(result.optString("detail"), "請放慢速度，確認前方路況後再前進。"), 90),
                limit(nonEmpty(result.optString("spokenText"), fallbackHeadline(riskLevel)), 40),
                prettyJsonOrRaw(text)
        );
    }

    private byte[] prepareAsJpeg(byte[] imageBytes) throws Exception {
        Bitmap decoded = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (decoded == null) {
            throw new IllegalArgumentException("照片無法讀取");
        }

        int width = decoded.getWidth();
        int height = decoded.getHeight();
        int maxDimension = Math.max(width, height);
        Bitmap prepared = decoded;
        if (maxDimension > 768) {
            float scale = 768f / maxDimension;
            prepared = Bitmap.createScaledBitmap(
                    decoded,
                    Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)),
                    true
            );
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        prepared.compress(Bitmap.CompressFormat.JPEG, 72, output);
        if (prepared != decoded) {
            prepared.recycle();
        }
        decoded.recycle();
        return output.toByteArray();
    }

    private String readFully(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }
        return result.toString();
    }

    private String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String fallbackHeadline(SceneRiskLevel riskLevel) {
        if (riskLevel == SceneRiskLevel.SAFE) {
            return "安全：可通行";
        }
        if (riskLevel == SceneRiskLevel.DANGER) {
            return "危險：請停下";
        }
        return "注意：請慢行";
    }

    private String limit(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String prettyJsonOrRaw(String value) {
        try {
            return new JSONObject(value).toString(2);
        } catch (Exception ignored) {
            return value;
        }
    }

    @Override
    public String displayName() {
        return "Gemini AI";
    }

    @Override
    public boolean isRealAi() {
        return true;
    }

    @Override
    public void release() {
        executor.shutdownNow();
    }
}
