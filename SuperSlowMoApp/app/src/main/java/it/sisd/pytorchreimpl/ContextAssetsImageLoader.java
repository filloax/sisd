package it.sisd.pytorchreimpl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;

public class ContextAssetsImageLoader extends AbstractImageLoader {
    private Context context;

    public ContextAssetsImageLoader(Context context) {
        this.context = context;
    }

    @Override
    public Bitmap loadImage(String filepath) throws IOException {
        return BitmapFactory.decodeStream(context.getAssets().open(filepath));
    }
}
