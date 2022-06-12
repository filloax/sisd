package it.sisd.superslowmo;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

public class Utils {

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    public static String getFileNameWithoutExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public static float getVideoFramerate(Context context, Uri videoPath) {
        MediaExtractor extractor = new MediaExtractor();
        float frameRate = 30;
        try {
            //Adjust data source as per the requirement if file, URI, etc.
            extractor.setDataSource(context, videoPath, null);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //Release stuff
            extractor.release();
        }

        return frameRate;
    }

    public static int[] getVideoSize(Context context, Uri videoPath) {
        Log.d(Constants.LOG_TAG, videoPath + " ARGH");
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(context, videoPath);

        int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        // video verticale
        if (Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)) % 180 == 90) {
            int tmp = width;
            width = height;
            height = tmp;
        }

//        Bitmap bmp = retriever.getFrameAtTime();
//        int width = bmp.getWidth();
//        int height = bmp.getHeight();

        retriever.release();

        return new int[] {width, height};
    }
}
