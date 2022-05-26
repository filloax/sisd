package it.sisd.pytorchreimpl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import it.sisd.superslowmo.Constants;

public class ContextImageLoader extends AbstractImageLoader {
    private Context context;

    public ContextImageLoader(Context context) {
        this.context = context;
    }

    @Override
    public Bitmap loadImage(String filepath) {
        String absPath = Paths.get(context.getFilesDir().getAbsolutePath(), filepath).toAbsolutePath().toString();
        return BitmapFactory.decodeFile(absPath);
    }
}
