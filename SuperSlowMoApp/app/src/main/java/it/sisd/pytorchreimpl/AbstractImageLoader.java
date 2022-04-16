package it.sisd.pytorchreimpl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractImageLoader implements IImageLoader {
    @Override
    public Bitmap loadImage(String filepath, int resizeX, int resizeY) throws IOException {
        Bitmap img = loadImage(filepath);

        if (resizeX >= 0 && resizeY >= 0) {
            img = Bitmap.createScaledBitmap(img, resizeX, resizeY, true);
        }

        return img;
    }

    @Override
    public abstract Bitmap loadImage(String filepath) throws IOException;
}
