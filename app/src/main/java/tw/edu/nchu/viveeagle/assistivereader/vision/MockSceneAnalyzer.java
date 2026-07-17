package tw.edu.nchu.viveeagle.assistivereader.vision;

public class MockSceneAnalyzer implements SceneAnalyzer {
    private int resultIndex;

    @Override
    public void analyze(byte[] imageBytes, String mimeType, Callback callback) {
        callback.onSuccess(nextResult());
    }

    private SceneAnalysisResult nextResult() {
        int current = resultIndex;
        resultIndex = (resultIndex + 1) % 4;

        if (current == 0) {
            return new SceneAnalysisResult(
                    SceneRiskLevel.SAFE,
                    "安全：可通行",
                    "前方人行道清楚，沒有立即障礙。請維持慢速，靠右前進。",
                    "安全，可通行，請靠右慢行。"
            );
        }
        if (current == 1) {
            return new SceneAnalysisResult(
                    SceneRiskLevel.CAUTION,
                    "注意：前方障礙",
                    "機車占用部分人行道，左側仍可通過。請放慢並留意車身邊緣。",
                    "注意前方障礙，請靠左慢行。"
            );
        }
        if (current == 2) {
            return new SceneAnalysisResult(
                    SceneRiskLevel.CAUTION,
                    "注意：路徑變窄",
                    "施工錐與圍欄讓通道變窄。請減速，確認左側空間再通過。",
                    "注意路徑變窄，請減速。"
            );
        }
        return new SceneAnalysisResult(
                SceneRiskLevel.DANGER,
                "危險：請停下",
                "前方有階梯或落差，可能絆倒。請先停下，改由旁人協助確認路線。",
                "危險，前方落差，請停下。"
        );
    }

    @Override
    public String displayName() {
        return "離線備援";
    }

    @Override
    public boolean isRealAi() {
        return false;
    }

    @Override
    public void release() {
    }
}
