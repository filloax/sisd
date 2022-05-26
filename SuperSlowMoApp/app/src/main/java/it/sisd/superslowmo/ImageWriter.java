package it.sisd.superslowmo;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;

public class ImageWriter implements IImageWriter {
    private String rootDir;

    public ImageWriter(String rootDir) {
        this.rootDir = rootDir;
    }

    @Override
    public void writeImage(String name, Bitmap bitmap) {
        String path = Paths.get(rootDir, name).toAbsolutePath().toString();
        try (FileOutputStream out = new FileOutputStream(path)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
