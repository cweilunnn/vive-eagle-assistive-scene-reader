package tw.edu.nchu.viveeagle.assistivereader.eagle;

import android.content.Context;

public final class EagleAdapterFactory {
    private EagleAdapterFactory() {
    }

    public static EagleSdkAdapter create(Context context) {
        return new MockEagleSdkAdapter(context);
    }
}

