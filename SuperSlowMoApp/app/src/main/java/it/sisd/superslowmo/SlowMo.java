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
    private Module flowCompCat;
//    private Module arbTimeFlowIntrp;
    private Module frameInterp;
    private float progress = 0;

    private IImageWriter imageWriter;

    private Consumer<String> logOut;

    public SlowMo(VideoDataset<Tensor> videoFrames, Module flowCompCat, Module frameInterp, IImageWriter imageWriter, Consumer<String> logOut) {
        this.videoFrames = videoFrames;
        this.flowCompCat = flowCompCat;
//        this.arbTimeFlowIntrp = arbTimeFlowIntrp;
        this.frameInterp = frameInterp;
        this.imageWriter = imageWriter;
        this.logOut = logOut;
    }

    public SlowMo() {
    }

    public static VideoDataset<Tensor> createDataset(Context context, String framesdir) throws IOException {
        return VideoDataset.withContext(context, framesdir, bitmap ->
            TensorImageUtils.bitmapToFloat32Tensor(bitmap, TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB)
        );
    }

    public synchronized void doEvaluation() {
        log("Starting SuperSloMo eval");

        int iter = 0;
        int frameCounter = 1;
        int scaleFactor = 2;
//        int batch_size = 1;
        float progressIncrements = 1 / (float) videoFrames.len();
        progress = 0;

        for (Pair<Tensor, Tensor> sample : videoFrames) {
            IValue I0 = IValue.from(sample.first), I1 = IValue.from(sample.second);

            log("=== Doing sample " + (iter++) + " ===");

            IValue[] flowOutTuple = flowCompCat.forward(I0, I1).toTuple();
            IValue I_F_0_1 = flowOutTuple[0], I_F_1_0 = flowOutTuple[1];
            Tensor F_0_1 = I_F_0_1.toTensor();
            Tensor F_1_0 = I_F_1_0.toTensor();

            log("\t Did flowout");

            // Save reference frames as image
            // Possibile ottimizzazione: usare direttamente il file originale senza crearne di nuovi
            resizeAndSaveFrame(frameCounter, sample.first);

            log(String.format(Locale.getDefault(), "\tSaved reference frame %05d.png", frameCounter));

            frameCounter++;

            for (int intermediateIndex = 1; intermediateIndex < scaleFactor; intermediateIndex ++) {
                double t = intermediateIndex / (double) scaleFactor;
                IValue I_t = IValue.from(t);

                IValue I_Ft_p = frameInterp.forward(
                        I_t,
                        I0,
                        I1,
                        I_F_0_1,
                        I_F_1_0
                );
                Tensor Ft_p = I_Ft_p.toTensor();

                log(String.format(Locale.getDefault(), "\tInterpolated frame %d|%d", intermediateIndex, frameCounter));

                // Save interpolated frame as image
                resizeAndSaveFrame( frameCounter, Ft_p);

                log(String.format(Locale.getDefault(), "\tSaved frame %05d.png", frameCounter));

                frameCounter ++;
            }

//            frameCounter += scaleFactor * (batch_size - 1);

            progress += Math.max(progressIncrements, 1f);
        }

        log("Ended SuperSloMo eval");
    }

    private void resizeAndSaveFrame(int sequenceNum, Tensor frameTensor) {
        /*
        (TP(frame0[batchIndex].detach()))
          .resize(videoFrames.origDim, Image.BILINEAR)
          .save(os.path.join(outputPath, str(frameCounter + args.sf * batchIndex) + ".png"))
         */
        int dimy = (int) frameTensor.shape()[2];
        int dimx = (int) frameTensor.shape()[3];
        String name = String.format("%05d.png", sequenceNum);

        log(dimx + " " + dimy);

        // Possibile ottimizzazione: resize insieme a conversione a bitmap
        Bitmap bm1 = TensorUtils.bitmapFromRGBImageAsFloatArray(frameTensor.getDataAsFloatArray(), dimx, dimy);
        imageWriter.writeImage(name, Bitmap.createScaledBitmap(bm1, videoFrames.getOrigDim().x, videoFrames.getOrigDim().y, true));
    }

    private void log(String s) {
        if (logOut != null)
            logOut.accept(s);
        Log.println(Log.INFO, "SlowMo", s);
    }

    public SlowMo videoFrames(VideoDataset<Tensor> videoFrames) {
        this.videoFrames = videoFrames;
        return this;
    }

    public SlowMo flowCompCat(Module flowCompCat) {
        this.flowCompCat = flowCompCat;
        return this;
    }

    public SlowMo frameInterp(Module frameInterp) {
        this.frameInterp = frameInterp;
        return this;
    }

    public SlowMo imageWriter(IImageWriter imageWriter) {
        this.imageWriter = imageWriter;
        return this;
    }

    public SlowMo logOut(Consumer<String> logOut) {
        this.logOut = logOut;
        return this;
    }

    public float getProgress() {
        return progress;
    }
}
