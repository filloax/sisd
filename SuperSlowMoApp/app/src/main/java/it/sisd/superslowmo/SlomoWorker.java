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