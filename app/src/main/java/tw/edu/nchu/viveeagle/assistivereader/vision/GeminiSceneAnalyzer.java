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
            "你是低視能使用者的保守型場景提醒助手。只根據照片中清楚可見的內容判斷，"
                    + "不可宣稱路線絕對安全，也不可取代白手杖、同行者或使用者判斷。"
                    + "優先檢查：階梯或高低差、車輛或機車、被占用的行走空間、路口、施工與近距離障礙。"
                    + "若不確定，選 CAUTION 並明確說不確定。"
                    + "只回傳 JSON，不要 Markdown。欄位固定為："
                    + "riskLevel（SAFE、CAUTION、DANGER 三選一）、"
                    + "headline（繁體中文，30 字內）、detail（繁體中文，100 字內）、"
                    + "spokenText（繁體中文，60 字內，適合立即朗讀）。"
                    + "SAFE 只代表未看見明顯立即危險，仍要提醒慢行確認。";

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
                connection.setConnectTimeout(20_000);
                connection.setReadTimeout(45_000);
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

                callback.onSuccess(parseResult(response));
            } catch (Exception exception) {
                callback.onError("AI 影像分析失敗：" + exception.getMessage(), exception);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
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
                limit(result.optString("headline", "AI 無法確認場景，請先停下。"), 40),
                "Gemini AI：" + limit(result.optString("detail", "照片資訊不足，請重新拍攝或請同行者確認。"), 140),
                limit(result.optString("spokenText", "無法確認前方狀況，請先停下。"), 80)
        );
    }

    private byte[] prepareAsJpeg(byte[] imageBytes) throws Exception {
        Bitmap decoded = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (decoded == null) {
            throw new IllegalArgumentException("照片格式無法讀取");
        }

        int width = decoded.getWidth();
        int height = decoded.getHeight();
        int maxDimension = Math.max(width, height);
        Bitmap prepared = decoded;
        if (maxDimension > 1280) {
            float scale = 1280f / maxDimension;
            prepared = Bitmap.createScaledBitmap(
                    decoded,
                    Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)),
                    true
            );
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        prepared.compress(Bitmap.CompressFormat.JPEG, 82, output);
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

    private String limit(String value, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    @Override
    public String displayName() {
        return "Gemini AI 真實影像分析";
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
