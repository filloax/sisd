package it.sisd.superslowmo;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.media.MediaMetadataRetriever;
import android.opengl.Visibility;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.arthenica.ffmpegkit.FFmpegKitConfig;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import it.sisd.pytorchreimpl.VideoDataset;

public class SlomoActivity extends AppCompatActivity {
    SlowMo slowMoEvaluator;
    ConvertVideo convertVideo;
    boolean runningEval = false;
    boolean loadedSlowMoEvaluator = false;
    String selectedFile = "";
    private int scaleFactor = 2; // scaleFactor 2 for now, to add selection

    Button startButton = null;
    Button chooseFileButton = null;
    ImageButton cancelButton = null;
    TextView progressText = null;
    ProgressBar progressBar = null;
    boolean loadedVideoFile = false;
    private ActivityResultLauncher<Intent> loadFileActivityResultLauncher;
    private Uri fileUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slomo);

        startButton = findViewById(R.id.startElabButton);
        startButton.setEnabled(false);
        startButton.setOnClickListener(this::startElabOnClick);

        cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setEnabled(false);
        cancelButton.setOnClickListener(this::cancelOnClick);

        chooseFileButton = findViewById(R.id.fileSelectButton);
        chooseFileButton.setEnabled(false);
        chooseFileButton.setOnClickListener(this::loadFileOnClick);

        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.slowmoProgressBar);
        progressText.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        convertVideo = new ConvertVideo(s -> Log.d(Constants.LOG_TAG, s));

        loadFileActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == Activity.RESULT_OK){
                        if(result != null){
                            fileUri = result.getData().getData();
                            selectedFile = FFmpegKitConfig.getSafParameterForRead(getApplicationContext(), fileUri);
                            startButton.setEnabled(true);
                            outString("Loaded file!");
                        }
                    }
                }
        );

        createSlowMoEvaluator();
    }

    private void createSlowMoEvaluator() {
        outString("Initializing SlowMo service...");

        new Thread( () -> {
            File workDir = new File(getApplicationContext().getFilesDir(), Constants.WORK_DIR);
            if (!workDir.isDirectory())
                workDir.mkdir();

            Module flowCompCat = loadPytorchModule("flowCompCat.ptl");

            slowMoEvaluator = new SlowMo()
                    .scaleFactor(scaleFactor)
                    .flowCompCat(flowCompCat)
                    .logOut(this::outString);

            runOnUiThread(() -> chooseFileButton.setEnabled(true));
            loadedSlowMoEvaluator = true;
            outString("Loaded SlowMo service!");
        }).start();
    }

    public void loadFileOnClick(View w) {
//        try {
//            selectedFile = Utils.assetFilePath(getApplicationContext(), "elefante.mp4");
//        } catch (IOException e) {
//            e.printStackTrace();
//            return;
//        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/mp4");

        loadFileActivityResultLauncher.launch(intent);

        outString("Loading file...");
        /*
        if(fileUri == null)
            outString("SelectFile not successful, using asset file elefante.mp4");
        else
            selectedFile = fileUri.getPath();

        if (!"".equals(selectedFile)) {
            startButton.setEnabled(true);
        }
        */

    }

    public void startElabOnClick(View v) {
        if (!loadedSlowMoEvaluator)
            outString("Premuto il pulsante prima che sia caricato il valutatore");

        if (fileUri == null)
            outString("Premuto il pulsante prima che sia caricato il video");
        else
            outString("Video caricato: " + fileUri);

        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_TIME;
        String out = "Premuto alle " + f.format(time);
        outString(out);

        if (!runningEval) {
            runningEval = true;
            cancelButton.setEnabled(true);

            new Thread(() -> {
                File selectedFileObj = new File(selectedFile);
                String videoName = Utils.getFileNameWithoutExtension(selectedFileObj.getName());

//                Optional<String> videoExt = Utils.getExtensionByStringHandling(selectedFile);
//                if (!videoExt.isPresent()) {
//                    outString(selectedFile + " is not a video!");
//                    runningEval = false;
//                    return;
//                }

                // Prepare dirs
                File extractedFramesDir = Paths.get(
                        getApplicationContext().getFilesDir().getAbsolutePath(),
                    Constants.WORK_DIR,
                    videoName + "_extracted"
                ).toAbsolutePath().toFile();
                if (extractedFramesDir.exists()) {
                    for(File file : extractedFramesDir.listFiles()){
                        file.delete();
                    }
                }
                extractedFramesDir.mkdir();
                File convertedFramesDir = Paths.get(
                    getApplicationContext().getFilesDir().getAbsolutePath(),
                    Constants.WORK_DIR,
                    videoName + "_converted"
                ).toAbsolutePath().toFile();
                if (convertedFramesDir.exists()) {
                    for(File file : convertedFramesDir.listFiles()){
                        file.delete();
                    }
                }
                convertedFramesDir.mkdir();

                // Extract frames from video

                int[] widthHeight = Utils.getVideoSize(getApplicationContext(), fileUri);
                int height = widthHeight[1];
                int width = widthHeight[0];

                // Set to horizontal
                if (height > width) {
                    outString("Vertical video, rotating");
                    convertVideo.rotateClockwise();
                } else {
                    convertVideo.resetRotate();
                }
                convertVideo.setResize(320, 180);
                boolean convertSuccess = convertVideo.extractFrames(
                        selectedFile,
                        extractedFramesDir.getAbsolutePath()
                );
                if (!convertSuccess) {
                    outString("Failed frame extraction!");
                    runningEval = false;
                    return;
                }

                outString("Frames extracted, base size: " + width + "x" + height);

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
                    outString(e.getMessage());
                    runningEval = false;
                    return;
                }
                Module frameInterp = loadPytorchModule(getFrameInterpFileForResolution(videoFrames.getOrigDim().x, videoFrames.getOrigDim().y));
                slowMoEvaluator
                        .videoFrames(videoFrames)
                        .frameInterp(frameInterp)
                        .imageWriter(new ImageWriter(convertedFramesDir.getAbsolutePath()));

                outString("Prepared frame dataset and slowmo evaluator, start conversion");

                runOnUiThread(() -> {
                    progressText.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                });

                // Inizia elaborazione
                slowMoEvaluator.doEvaluation();

                runOnUiThread(() -> {
                    progressText.setVisibility(View.INVISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                });

                // Converti frame in video
                outString("Conversion done, merge frames in video");
                String outVideoName = "SloMo_" + videoName + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outVideoName);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SuperSloMo/");
                // Usa mp4 essendo in convertutils codec h264
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                Uri outVideoUri = getContentResolver().insert(MediaStore.Video.Media.getContentUri("external"), contentValues);
                String outVideoPath = FFmpegKitConfig.getSafParameterForWrite(getApplicationContext(), outVideoUri);
//                  String outVideoPath = new File(selectedFileObj.getParent(), outVideoName + ".mp4").toString();
//                  String outVideoPath = new File(selectedFileObj.getParent(), outVideoName + "." + videoExt.get()).toString();
                float fps = Utils.getVideoFramerate(getApplicationContext(), fileUri);

                outString("=== " + outVideoName + " | " + videoName);

                outString("Saving to " + outVideoPath + " at " + fps + " fps");

                convertVideo.resetResize();
                if (height > width) {
                    convertVideo.rotateCounterClockwise();
                } else {
                    convertVideo.resetRotate();
                }
                boolean mergeSuccess = convertVideo.createVideo(convertedFramesDir.getAbsolutePath(), outVideoPath, fps);
                if (!mergeSuccess) {
                    outString("Failed convert frames to video!");
                    runningEval = false;
                    return;
                }

                outString("Conversion done, removing frames...");

                runningEval = false;
                outString("Saved to " + outVideoPath);
            }).start();

            new Thread(() -> {
                while (runningEval) {
                    if (progressBar.isEnabled()) {
                        progressBar.setProgress((int)(slowMoEvaluator.getProgress() * 100));
                    }
                }
            }).start();

            cancelButton.setEnabled(false);
        } else {
            outString("Already running");
        }
    }

    public void cancelOnClick(View view) {

    }

    private Module loadPytorchModule(String assetName) {
        return LiteModuleLoader.loadModuleFromAsset(getAssets(), assetName);
    }

    @SuppressLint("SetTextI18n")
    private void outString(String s) {
        TextView tv = (TextView) findViewById(R.id.textView);
        runOnUiThread(() -> tv.setText(s + "\n" + tv.getText()));
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
}