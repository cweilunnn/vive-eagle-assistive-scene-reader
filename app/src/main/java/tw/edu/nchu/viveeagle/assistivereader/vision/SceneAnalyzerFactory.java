package tw.edu.nchu.viveeagle.assistivereader.vision;

import tw.edu.nchu.viveeagle.assistivereader.BuildConfig;

public final class SceneAnalyzerFactory {
    private SceneAnalyzerFactory() {
    }

    public static SceneAnalyzer create() {
        if (BuildConfig.GEMINI_API_KEY == null || BuildConfig.GEMINI_API_KEY.trim().isEmpty()) {
            return new MockSceneAnalyzer();
        }
        return new GeminiSceneAnalyzer(BuildConfig.GEMINI_API_KEY, BuildConfig.GEMINI_MODEL);
    }
}
