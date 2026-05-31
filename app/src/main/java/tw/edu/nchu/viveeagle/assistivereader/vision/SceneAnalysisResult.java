package tw.edu.nchu.viveeagle.assistivereader.vision;

public class SceneAnalysisResult {
    public final SceneRiskLevel riskLevel;
    public final String headline;
    public final String detail;
    public final String spokenText;

    public SceneAnalysisResult(SceneRiskLevel riskLevel, String headline, String detail, String spokenText) {
        this.riskLevel = riskLevel;
        this.headline = headline;
        this.detail = detail;
        this.spokenText = spokenText;
    }
}
