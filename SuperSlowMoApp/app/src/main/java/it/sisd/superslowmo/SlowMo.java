package it.sisd.superslowmo;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Consumer;

import it.sisd.pytorchreimpl.TensorUtils;
import it.sisd.pytorchreimpl.VideoDataset;

public class SlowMo {
    private VideoDataset<Tensor> videoFrames;
    private Bitmap[] outFrames;
    private Module flowCompCat;
//    private Module arbTimeFlowIntrp;
    private Module frameInterp;

    private Consumer<String> logOut;

    public SlowMo(VideoDataset<Tensor> videoFrames, Module flowCompCat, Module frameInterp, Consumer<String> logOut) {
        this.videoFrames = videoFrames;
        this.outFrames = null;
        this.flowCompCat = flowCompCat;
//        this.arbTimeFlowIntrp = arbTimeFlowIntrp;
        this.frameInterp = frameInterp;
        this.logOut = logOut;
    }

    public SlowMo(VideoDataset<Tensor> videoFrames, Module flowComp, Module frameInterp) {
        this(videoFrames, flowComp, frameInterp, null);
    }

    public static VideoDataset<Tensor> createDataset(Context context, String framesdir) throws IOException {
        return VideoDataset.withContext(context, framesdir, bitmap ->
            TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB)
        );
    }

    public void doEvaluation() {
        log("Starting SuperSloMo eval");

        int iter = 0;
        int frameCounter = 1;
        int scaleFactor = 2;
        int batch_size = 1;

        for (Pair<Tensor, Tensor> sample : videoFrames) {
            IValue I0 = IValue.from(sample.first), I1 = IValue.from(sample.second);

            log("=== Doing sample " + (iter++) + " ===");
            //Tensor catFrames = TensorUtils.cat(sample.first, sample.second, 1);
            float[] printData = sample.first.getDataAsFloatArray(); // DEBUG
            log(
                    String.format(Locale.getDefault(), "Pre flowout: shape %s, first 3 values (%.2f %.2f %.2f)",
                            Arrays.toString(sample.first.shape()),
                            printData[0], printData[1], printData[2]
                    )
            );
            IValue[] flowOutTuple = flowCompCat.forward(I0, I1).toTuple();
            IValue I_F_0_1 = flowOutTuple[0], I_F_1_0 = flowOutTuple[1];
            Tensor F_0_1 = I_F_0_1.toTensor();
            Tensor F_1_0 = I_F_1_0.toTensor();

            printData = F_0_1.getDataAsFloatArray(); // DEBUG
            log(
                    String.format(Locale.getDefault(), "Post flowout F_0_1: shape %s, first 3 values %.2f %.2f %.2f",
                    Arrays.toString(F_1_0.shape()), printData[0], printData[1], printData[2])
            );
            printData = F_1_0.getDataAsFloatArray(); // DEBUG
            log(
                    String.format(Locale.getDefault(), "Post flowout F_1_0: shape %s, first 3 values %.2f %.2f %.2f",
                    Arrays.toString(F_1_0.shape()), printData[0], printData[1], printData[2])
            );

            // Save reference frames as image
            // TODO

            frameCounter++;

            for (int intermediateIndex = 1; intermediateIndex < scaleFactor; intermediateIndex ++) {
                double t = intermediateIndex / (double) scaleFactor;
                IValue I_t = IValue.from(t);
                // Workaround: passare IValue creati da non tensori in ingresso a forward crasha
                // con ArrayIndexOutOfBoundsException: vector, probabilmente bug di Pytorch?
//                IValue I_t = IValue.from(Tensor.fromBlob(new double[]{t}, new long[]{1}));

                IValue I_Ft_p = frameInterp.forward(
                        I_t,
                        I0,
                        I1,
                        I_F_0_1,
                        I_F_1_0
                );
                Tensor Ft_p = I_Ft_p.toTensor();

                // Save interpolated frame as image
                // TODO

                printData = Ft_p.getDataAsFloatArray(); // DEBUG
                log(
                        String.format(Locale.getDefault(), "Interpolated frame %d|%d: shape %s, first 3 values %.2f %.2f %.2f",
                                intermediateIndex, frameCounter,
                                Arrays.toString(Ft_p.shape()),
                                printData[0], printData[1], printData[2]
                        )
                );

                frameCounter ++;
            }

            frameCounter += scaleFactor * (batch_size - 1);
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
