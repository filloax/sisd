package it.sisd.pytorchreimpl;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;

public class VideoDataset<T> extends Dataset<Pair<T, T>> {
    private IImageLoader imageLoader;
    private String root;
    private String[] framePaths;
    private Function<Bitmap, T> transform;
    private Size origDim;
    private Size dim;

    // Dimensions will be scaled to be multiples of this number
    public static final int DIM_UNIT = 32;

    protected VideoDataset(IImageLoader imageLoader, String root, String[] framePaths, Function<Bitmap, T> transform, Size origDim, Size dim) {
        this.imageLoader = imageLoader;
        this.root = root;
        this.framePaths = framePaths;
        this.transform = transform;
        this.origDim = origDim;
        this.dim = dim;
    }

    /**
     * Create a new VideoDataset loading from the files
     * @param root Root directory path.
     * @param transform callable, optional, but required
     *                 if you want the dataset to return a type
     *                 different than Bitmap
     *                 A function/transform that takes in
     *                 a sample and returns a transformed version.
     *
     * @return a new VideoDataset
     * @throws IOException if load fails
     */
    public static <T> VideoDataset<T> withRootPath(String root, Function<Bitmap, T> transform) throws IOException {
        File rootDir = new File(root);

        if (!rootDir.isDirectory()) {
            throw new IllegalArgumentException("root " + root + " is not a directory!");
        }

        String[] fileList = rootDir.list();

        if (fileList.length == 0) {
            throw new IllegalArgumentException(String.format("No files inside the root dir '%s'", root));
        }

        String[] framePaths = new String[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            framePaths[i] = Paths.get(root, fileList[i]).toAbsolutePath().toString();
        }

        IImageLoader imageLoader = new ImageLoader();
        Bitmap firstFrame = imageLoader.loadImage(framePaths[0]);
        Size origDim = new Size(firstFrame.getWidth(), firstFrame.getHeight());
        Size dim = new Size(Math.floorDiv(origDim.x, DIM_UNIT) * DIM_UNIT, Math.floorDiv(origDim.y, DIM_UNIT) * DIM_UNIT);

        return new VideoDataset<>(imageLoader, root, framePaths, transform, origDim, dim);
    }


    public static <T> VideoDataset<T> withRootPath(String root) throws IOException {
        return withRootPath(root, null);
    }
    /**
     * Create a new VideoDataset loading from the assets folder
     * @param context Android context, required for file loading.
     * @param root Root directory path.
     * @param transform callable, optional, but required
     *                 if you want the dataset to return a type
     *                 different than Bitmap
     *                 A function/transform that takes in
     *                 a sample and returns a transformed version.
     *
     * @return a new VideoDataset
     * @throws IOException if load fails
     */
    public static <T> VideoDataset<T> withContextAssets(Context context, String root, Function<Bitmap, T> transform) throws IOException {
        String[] fileList = context.getAssets().list(root);

        if (fileList.length == 0) {
            throw new IllegalArgumentException(String.format("No files inside the root dir '%s'", root));
        }

        String[] framePaths = new String[fileList.length];
        for (int i = 0; i < fileList.length; i++) {
            framePaths[i] = Paths.get(root, fileList[i]).toString();
        }

        IImageLoader imageLoader = new ContextAssetsImageLoader(context);
        Bitmap firstFrame = imageLoader.loadImage(framePaths[0]);
        Size origDim = new Size(firstFrame.getWidth(), firstFrame.getHeight());
        Size dim = new Size(Math.floorDiv(origDim.x, DIM_UNIT) * DIM_UNIT, Math.floorDiv(origDim.y, DIM_UNIT) * DIM_UNIT);

        return new VideoDataset<>(imageLoader, root, framePaths, transform, origDim, dim);
    }

    // For testing
    public static <T> VideoDataset<T> withImageLoader(IImageLoader imageLoader, String[] framePaths, String root, Function<Bitmap, T> transform) {
        Bitmap firstFrame;
        try {
            firstFrame = imageLoader.loadImage(framePaths[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Size origDim = new Size(firstFrame.getWidth(), firstFrame.getHeight());
        Size dim = new Size(Math.floorDiv(origDim.x, 32) * 32, Math.floorDiv(origDim.y, 32) * 32);

        return new VideoDataset<>(imageLoader, root, framePaths, transform, origDim, dim);
    }

    public static <T> VideoDataset<T> withContextAssets(Context context, String root) throws IOException {
        return withContextAssets(context, root, null);
    }

    /**
     * Returns the sample corresponding to `index` from dataset.
     * The sample consists of two reference frames - I0 and I1.
     * @param index index
     * @return sample is [I0, I1] where I0 is the frame with index
     *         `index` and I1 is the next frame.
     */
    @Override
    public Pair<T, T> get(int index) {
        ArrayList<T> sample = new ArrayList<>(2);

        // Loop over for all frames corresponding to the `index`.
        for (int idx = index; idx <= index +1; idx++) {
            String framePath = framePaths[idx];
            // Open image with android
            Bitmap image;
            T transformed;
            try {
                image = imageLoader.loadImage(framePath, dim.x, dim.y);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Apply transformation if specified.
            if (transform != null) {
                transformed = transform.apply(image);
            } else {
                try {
                    transformed = getTFromBitmap(image);
                } catch (ClassCastException e) {
                    throw new IllegalStateException("VideoDataset is set to non-Bitmap type but has no transform", e);
                }
            }
            sample.add(idx - index, transformed);
        }

        return Pair.create(sample.get(0), sample.get(1));
    }

    // Workaround for SuppressWarnings not working inside methods apparently
    @SuppressWarnings("unchecked")
    private T getTFromBitmap(Bitmap bitmap) throws ClassCastException {
        return (T) bitmap;
    }

    @Override
    public int len() {
        return framePaths.length - 1;
    }

    @Override
    public String toString() {
        return "Dataset VideoDataset\n" +
                "\tNumber of datapoints: " +
                len() +
                "\n\tRoot Location: " +
                root +
                "\n\tTransforms (if any): " +
                transform;
    }

    public IImageLoader getImageLoader() {
        return imageLoader;
    }

    public void setImageLoader(IImageLoader imageLoader) {
        this.imageLoader = imageLoader;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String[] getFramePaths() {
        return framePaths;
    }

    public void setFramePaths(String[] framePaths) {
        this.framePaths = framePaths;
    }

    public Function<Bitmap, T> getTransform() {
        return transform;
    }

    public void setTransform(Function<Bitmap, T> transform) {
        this.transform = transform;
    }

    public Size getOrigDim() {
        return origDim;
    }

    public void setOrigDim(Size origDim) {
        this.origDim = origDim;
    }

    public Size getDim() {
        return dim;
    }

    public void setDim(Size dim) {
        this.dim = dim;
    }


    public static class Size {
        public int x;
        public int y;

        public Size(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public static Size ofBitmap(Bitmap bitmap) {
            return new Size(bitmap.getWidth(), bitmap.getHeight());
        }

        @Override
        public String toString() {
            return String.format(Locale.getDefault(), "(%d, %d)", x, y);
        }
    }
}
