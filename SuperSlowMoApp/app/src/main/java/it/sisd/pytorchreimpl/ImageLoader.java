package it.sisd.pytorchreimpl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class ImageLoader extends AbstractImageLoader {

    @Override
    public Bitmap loadImage(String filepath) throws IOException {
        return BitmapFactory.decodeFile(filepath);
    }
}
