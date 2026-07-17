package tw.edu.nchu.viveeagle.assistivereader.vision;

public class SceneAnalysisResult {
    public final SceneRiskLevel riskLevel;
    public final String headline;
    public final String detail;
    public final String spokenText;
    public final String fullResponse;

    public SceneAnalysisResult(
            SceneRiskLevel riskLevel,
            String headline,
            String detail,
            String spokenText
    ) {
        this(riskLevel, headline, detail, spokenText, "");
    }

    public SceneAnalysisResult(
            SceneRiskLevel riskLevel,
            String headline,
            String detail,
            String spokenText,
            String fullResponse
    ) {
        this.riskLevel = riskLevel;
        this.headline = headline;
        this.detail = detail;
        this.spokenText = spokenText;
        this.fullResponse = fullResponse == null ? "" : fullResponse;
    }
}
