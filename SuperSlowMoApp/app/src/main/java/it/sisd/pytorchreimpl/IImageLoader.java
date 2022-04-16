package it.sisd.pytorchreimpl;

import android.graphics.Bitmap;

import java.io.IOException;

public interface IImageLoader {
    Bitmap loadImage(String filepath, int resizeX, int resizeY) throws IOException;
    Bitmap loadImage(String filepath) throws IOException;
}
