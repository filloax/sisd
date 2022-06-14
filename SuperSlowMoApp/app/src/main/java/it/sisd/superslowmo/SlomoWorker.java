package it.sisd.superslowmo;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import it.sisd.pytorchreimpl.VideoDataset;

public class SlomoWorker extends Worker {
    public static final String VIDEO_NAME = "VIDEO_NAME";
    public static final String PROGRESS_TAG = "PROGRESS_TAG";
    public static final String PROGRESS_MESSAGE_TAG = "PROGRESS_MESSAGE_TAG";

    private SlowMo slowMoEvaluator;
    private IProgressHandler progressHandler;

    public SlomoWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        progressHandler = new SlomoWorkerProgressHandler(context, this, PROGRESS_TAG, PROGRESS_MESSAGE_TAG);
    }

    @NonNull
    @Override
    public Result doWork() {
        String videoName = getInputData().getString(VIDEO_NAME);

        Module flowCompCat = loadPytorchModule("flowCompCat.ptl");
        int scaleFactor = 2;

        slowMoEvaluator = new SlowMo()
                .scaleFactor(scaleFactor)
                .flowCompCat(flowCompCat)
                .progressHandler(progressHandler);

        // Get frame directories (already created in SlomoActivity)
        File extractedFramesDir = Paths.get(
                getApplicationContext().getFilesDir().getAbsolutePath(),
                Constants.WORK_DIR,
                videoName + "_extracted"
        ).toAbsolutePath().toFile();
        File convertedFramesDir = Paths.get(
                getApplicationContext().getFilesDir().getAbsolutePath(),
                Constants.WORK_DIR,
                videoName + "_converted"
        ).toAbsolutePath().toFile();

        // Elab video
        // Imposta parametri di superslowo in base a risoluzione
        // e nome file (nome usato per percorso di destinazione
        VideoDataset<Tensor> videoFrames = null;
        try {
            videoFrames = VideoDataset.withRootPath(extractedFramesDir.getAbsolutePath(), bitmap ->
                    TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                            TensorImageUtils.TORCHVISION_NORM_STD_RGB
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
            progressHandler.publishProgress(0, "SuperSloMo eval failed: error while reading frames");
            return Result.failure();
        }

        Module frameInterp = loadPytorchModule(getFrameInterpFileForResolution(videoFrames.getOrigDim().x, videoFrames.getOrigDim().y));

        slowMoEvaluator
                .videoFrames(videoFrames)
                .frameInterp(frameInterp)
                .imageWriter(new ImageWriter(convertedFramesDir.getAbsolutePath()));

        slowMoEvaluator.doEvaluation();


        return Result.success();
    }

    /*
    @NonNull
    @Override
    public Result doWork() {
        String videoName = getInputData().getString(VIDEO_NAME);

        setProgressAsync(new Data.Builder().putFloat(PROGRESS, 0F).putString(PROGRESS_MESSAGE, "Starting SuperSloMo eval").build());
        setForegroundAsync(getForegroundInfo(0));

        // Get frame directories (already created in SlomoActivity)
        File extractedFramesDir = Paths.get(
                getApplicationContext().getFilesDir().getAbsolutePath(),
                Constants.WORK_DIR,
                videoName + "_extracted"
        ).toAbsolutePath().toFile();
        File convertedFramesDir = Paths.get(
                getApplicationContext().getFilesDir().getAbsolutePath(),
                Constants.WORK_DIR,
                videoName + "_converted"
        ).toAbsolutePath().toFile();

        // Elab video
        // Imposta parametri di superslowo in base a risoluzione
        // e nome file (nome usato per percorso di destinazione
        videoFrames = null;
        try {
            videoFrames = VideoDataset.withRootPath(extractedFramesDir.getAbsolutePath(), bitmap ->
                    TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                            TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                            TensorImageUtils.TORCHVISION_NORM_STD_RGB
                    )
            );
        } catch (IOException e) {
            e.printStackTrace();
            setProgressAsync(new Data.Builder().putFloat(PROGRESS, 0)
                    .putString(PROGRESS_MESSAGE, "SuperSloMo eval failed: error while reading frames")
                    .build());
            return Result.failure();
        }

        Module frameInterp = loadPytorchModule(getFrameInterpFileForResolution(videoFrames.getOrigDim().x, videoFrames.getOrigDim().y));
        imageWriter = new ImageWriter(convertedFramesDir.getAbsolutePath());

        int iter = 0;
        int frameCounter = 1;
//        int batch_size = 1;
        float progressIncrements = 1 / (float) videoFrames.len();
        progress = 0;

        for (Pair<Tensor, Tensor> sample : videoFrames) {
            IValue I0 = IValue.from(sample.first), I1 = IValue.from(sample.second);

            IValue[] flowOutTuple = flowCompCat.forward(I0, I1).toTuple();
            IValue I_F_0_1 = flowOutTuple[0], I_F_1_0 = flowOutTuple[1];
            Tensor F_0_1 = I_F_0_1.toTensor();
            Tensor F_1_0 = I_F_1_0.toTensor();

            // Save reference frames as image
            // Possibile ottimizzazione: usare direttamente il file originale senza crearne di nuovi
            resizeAndSaveFrame(frameCounter, sample.first);

            frameCounter++;

            for (int intermediateIndex = 1; intermediateIndex < scaleFactor; intermediateIndex++) {
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

                // Save interpolated frame as image
                resizeAndSaveFrame(frameCounter, Ft_p);

                frameCounter++;
            }

//            frameCounter += scaleFactor * (batch_size - 1);

            progress += Math.min(progressIncrements, 1f);
            // Send progress to activity
            setProgressAsync(new Data.Builder().putFloat(PROGRESS, progress).build());
            // Send progress to notification
            setForegroundAsync(getForegroundInfo(progress));
        }

        return Result.success();
    }
    */

    private static String getFrameInterpFileForResolution(int x, int y) {
        // Sostituire con classe a parte? Per ora check hardcoded con uniche
        // risoluzioni disponibili
        String prefix = "frameInterp_";
        if (x == 320 && y == 180) {
            return prefix + "320x160.ptl";
        } else if (x == 1280 && y == 720) {
            return prefix + "1280x704.ptl";
        }

        throw new IllegalArgumentException("No frame interp model available for resolution " + x + "x" + y);
    }

    private Module loadPytorchModule(String assetName) {
        return LiteModuleLoader.loadModuleFromAsset(getApplicationContext().getAssets(), assetName);
    }
}