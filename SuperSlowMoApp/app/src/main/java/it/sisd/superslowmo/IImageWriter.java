package it.sisd.superslowmo;

import android.graphics.Bitmap;

public interface IImageWriter {
    void writeImage(String name, Bitmap bitmap);
}
