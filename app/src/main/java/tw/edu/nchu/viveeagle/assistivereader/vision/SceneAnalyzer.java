package tw.edu.nchu.viveeagle.assistivereader.vision;

public interface SceneAnalyzer {
    void analyze(byte[] imageBytes, String mimeType, Callback callback);

    String displayName();

    boolean isRealAi();

    void release();

    interface Callback {
        void onSuccess(SceneAnalysisResult result);

        void onError(String message, Throwable throwable);
    }
}
