package it.sisd.superslowmo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.util.function.Consumer;

import it.sisd.pytorchreimpl.VideoDataset;

public class SlowMo {
    private VideoDataset<Tensor> videoFrames;
    private Bitmap[] outFrames;
    private Module flowComp;
    private Module arbTimeFlowIntrp;
    private Module backWarp;

    private Consumer<String> logOut;

    public SlowMo(VideoDataset<Tensor> videoFrames, Module flowComp, Module arbTimeFlowIntrp, Module backWarp, Consumer<String> logOut) {
        this.videoFrames = videoFrames;
        this.outFrames = null;
        this.flowComp = flowComp;
        this.arbTimeFlowIntrp = arbTimeFlowIntrp;
        this.backWarp = backWarp;
        this.logOut = logOut;
    }

    public SlowMo(VideoDataset<Tensor> videoFrames, Module flowComp, Module arbTimeFlowIntrp, Module backWarp) {
        this(videoFrames, flowComp, arbTimeFlowIntrp, backWarp, null);
    }

    public static VideoDataset<Tensor> createDataset(Context context, String framesdir) throws IOException {
        return VideoDataset.withContext(context, framesdir, bitmap ->
            TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB)
        );
    }

    public void doEvaluation() {
        log("Starting SuperSloMo eval");

        for (Pair<Tensor, Tensor> sample : videoFrames) {
            log(sample.first.shape()[2] + " " + sample.first.getDataAsFloatArray()[0]);
        }

        log("Ended SuperSloMo eval");
    }

    private void log(String s) {
        if (logOut != null)
            logOut.accept(s);
        else
            Log.println(Log.INFO, "SlowMo", s);
    }
}
