package tw.edu.nchu.viveeagle.assistivereader.vision;

public final class MissingApiKeySceneAnalyzer implements SceneAnalyzer {
    @Override
    public void analyze(byte[] imageBytes, String mimeType, Callback callback) {
        callback.onError("尚未設定 GEMINI_API_KEY，無法使用正式線上 AI 分析。", null);
    }

    @Override
    public String displayName() {
        return "Gemini AI（未設定 API key）";
    }

    @Override
    public boolean isRealAi() {
        return false;
    }

    @Override
    public void release() {
    }
}
