package tw.edu.nchu.viveeagle.assistivereader.vision;

public class MockSceneAnalyzer {
    private int resultIndex;

    public SceneAnalysisResult analyze(byte[] imageBytes) {
        int current = resultIndex;
        resultIndex = (resultIndex + 1) % 4;

        if (current == 0) {
            return new SceneAnalysisResult(
                    SceneRiskLevel.SAFE,
                    "安全：前方可通行，請靠右慢行。",
                    "Mock AI 結果：前方人行道清楚，未偵測到立即障礙。請維持慢速，靠右前進。",
                    "安全，前方可通行，請靠右慢行。"
            );
        }
        if (current == 1) {
            return new SceneAnalysisResult(
                    SceneRiskLevel.CAUTION,
                    "注意：左前方疑似停放機車。",
                    "Mock AI 結果：左前方約 2 公尺有障礙物，右側仍有可通行空間。請放慢速度，稍微往右。",
                    "注意，左前方疑似停放機車。請放慢速度，稍微往右。"
            );
        }
        if (current == 2) {
            return new SceneAnalysisResult(
                    SceneRiskLevel.DANGER,
                    "危險：前方疑似路口，請先停下。",
                    "Mock AI 結果：前方道路開口較大，可能是路口或車道出入口。請先停下，確認車流聲或請同行者協助。",
                    "危險，前方疑似路口，請先停下確認。"
            );
        }
        return new SceneAnalysisResult(
                SceneRiskLevel.DANGER,
                "危險：前方疑似階梯或高低差。",
                "Mock AI 結果：前方地面出現連續水平邊緣，可能是階梯或斜坡。請停下，用手杖或同行者確認。",
                "危險，前方疑似階梯或高低差。請停下確認。"
        );
    }
}
