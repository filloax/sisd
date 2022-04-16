package it.sisd.superslowmo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VideoConverter {
    private static Path frameDirPath = null;

    public static Bitmap[] getFrames(Context context) throws IOException {
        String[] filelist = context.getAssets().list(Constants.IN_FRAMES_DIR);
        Bitmap[] out = new Bitmap[filelist.length];

        File file;
        for (int i = 0; i < filelist.length; i++) {
            Path path = Paths.get(Constants.IN_FRAMES_DIR, filelist[i]);
            out[i] = BitmapFactory.decodeStream(context.getAssets().open(path.toString()));
        }

        return out;
    }
}
