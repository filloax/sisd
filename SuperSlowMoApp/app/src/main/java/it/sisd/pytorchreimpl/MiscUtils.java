package it.sisd.pytorchreimpl;

import android.content.Context;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;

public class MiscUtils {
    private static Context context = null;

    public static void initContext(Context context1) {
        if (context == null)
            context = context1;
        else
            throw new RuntimeException("MiscUtils: Cannot init context twice");
    }

    // Separato per testing
    public static Module loadPytorchModule(String assetName) {
        assert context != null;

        return LiteModuleLoader.loadModuleFromAsset(context.getAssets(), assetName);
    }
}
