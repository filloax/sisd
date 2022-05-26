package it.sisd.pytorchreimpl;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VideoDatasetTest {
    private IImageLoader imageLoader;

    // percorso di esecuzione da qua parte da ..\SuperSlowMoApp\app\
    public static final String TEST_ASSET_PATH = "src/test/assets/";

    @Before
    public void up() {
        imageLoader = new ContextImageLoader(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testBitmap() {
        String[] testFrames = {
                "inframes/000001.png",
                "inframes/000002.png",
                "inframes/000003.png",
        };

        VideoDataset<Bitmap> videoDataset = VideoDataset.withImageLoader(imageLoader, testFrames, "inframes", null);

        assertEquals(2, videoDataset.len());

        assertEquals(1280, videoDataset.getOrigDim().x);
        assertEquals(720, videoDataset.getOrigDim().y);
        assertEquals(1280, videoDataset.getDim().x);
        assertEquals(704, videoDataset.getDim().y); // floor(y/32) * 32

        int countIters = 0;

        for (Pair<Bitmap, Bitmap> sample : videoDataset) {
            assertEquals(1280, sample.first.getWidth());
            assertEquals(704, sample.first.getHeight());
            assertEquals(1280, sample.second.getWidth());
            assertEquals(704, sample.second.getHeight());
            countIters++;
        }

        assertEquals(2, countIters);
    }

    @Test
    public void testTensor() {
        String[] testFrames = {
                "inframes/000001.png",
                "inframes/000002.png",
                "inframes/000003.png",
        };

        VideoDataset<Tensor> videoDataset = VideoDataset.withImageLoader(imageLoader, testFrames, "inframes", bitmap ->
            TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB)
        );

        for (Pair<Tensor, Tensor> sample : videoDataset) {
            assertEquals(1280, sample.first.shape()[3]);
            assertEquals(704, sample.first.shape()[2]);
            assertEquals(1280, sample.second.shape()[3]);
            assertEquals(704, sample.second.shape()[2]);
        }
    }
}
