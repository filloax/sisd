package it.sisd.superslowmo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import java.io.File;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class SlomoActivity extends AppCompatActivity {
    //SlomoWorker slowMoEvaluator;
    private OneTimeWorkRequest slomoWorkRequest;
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

    private File extractedFramesDir;
    private File convertedFramesDir;
    private int videoWidth;
    private int videoHeight;

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

        loadedSlowMoEvaluator = true;
        chooseFileButton.setEnabled(true);
        //createSlowMoEvaluator();
    }

    private void createSlowMoEvaluator() {
        outString("Initializing SlowMo service...");

        new Thread( () -> {
            File workDir = new File(getApplicationContext().getFilesDir(), Constants.WORK_DIR);
            if (!workDir.isDirectory())
                workDir.mkdir();

            Module flowCompCat = loadPytorchModule("flowCompCat.ptl");

            /*
            slowMoEvaluator = new SlowMo()
                    .scaleFactor(scaleFactor)
                    .flowCompCat(flowCompCat)
                    .logOut(this::outString);
            */

            runOnUiThread(() -> chooseFileButton.setEnabled(true));
            loadedSlowMoEvaluator = true;
            outString("Loaded SlowMo service!");
        }).start();
    }

    public void loadFileOnClick(View w) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("video/mp4");

        loadFileActivityResultLauncher.launch(intent);

        outString("Loading file...");
    }

    @SuppressLint("RestrictedApi")
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

            File selectedFileObj = new File(selectedFile);
            String videoName = Utils.getFileNameWithoutExtension(selectedFileObj.getName());

            // Creates WorkRequest, passing videoName as input data
            slomoWorkRequest = new OneTimeWorkRequest.Builder(SlomoWorker.class).
                    setInputData(new Data.Builder().putString(SlomoWorker.VIDEO_NAME, videoName).build()).build();

            // Callback to handle both intermediate progress sent by SlomoWorker and end Result
            WorkManager.getInstance(getApplicationContext())
                    .getWorkInfoByIdLiveData(slomoWorkRequest.getId())
                    .observeForever(new Observer<WorkInfo>() {
                        @Override
                        public void onChanged(WorkInfo workInfo) {
                            if(workInfo != null){
                                float progress = workInfo.getProgress().getFloat(SlomoWorker.PROGRESS_TAG, 0);
                                String progress_message = workInfo.getProgress().getString(SlomoWorker.PROGRESS_MESSAGE_TAG);
                                progressBar.setProgress((int)(progress*100));
                                if(progress_message != null)
                                    outString(progress_message);

                                if(workInfo.getState() == WorkInfo.State.SUCCEEDED){
                                    new Thread(() -> {
                                        // Finally, convert and merge frames into video

                                        outString("SuperSlomo evaluation completed, merging frames in video");

                                        // Update notification
                                        Notification notification = new NotificationCompat.Builder(getApplicationContext(), getString(R.string.notification_channel_id))
                                                .setContentTitle(getString(R.string.notification_completed_title))
                                                .setTicker(getString(R.string.notification_completed_title))
                                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                                                .setProgress(100, (int)(progress * 100), false)
                                                .build();
                                        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
                                        notificationManager.notify(getResources().getInteger(R.integer.notification_id), notification);

                                        runOnUiThread(() -> {
                                            progressText.setVisibility(View.INVISIBLE);
                                            progressBar.setVisibility(View.INVISIBLE);
                                        });

                                        // Converti frame in video

                                        String outVideoName = "SloMo_" + videoName + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

                                        ContentValues contentValues = new ContentValues();
                                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, outVideoName);
                                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/SuperSloMo/");
                                        // Usa mp4 essendo in convertutils codec h264
                                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
                                        Uri outVideoUri = getContentResolver().insert(MediaStore.Video.Media.getContentUri("external"), contentValues);
                                        String outVideoPath = FFmpegKitConfig.getSafParameterForWrite(getApplicationContext(), outVideoUri);
//                                      String outVideoPath = new File(selectedFileObj.getParent(), outVideoName + ".mp4").toString();
//                                      String outVideoPath = new File(selectedFileObj.getParent(), outVideoName + "." + videoExt.get()).toString();
                                        float fps = Utils.getVideoFramerate(getApplicationContext(), fileUri);

                                        outString("=== " + outVideoName + " | " + videoName);

                                        outString("Saving to " + outVideoPath + " at " + fps + " fps");

                                        convertVideo.resetResize();
                                        if (videoHeight > videoWidth) {
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

                                    // Remove the observer when not needed anymore
                                    // 'this' = observer
                                    WorkManager.getInstance(getApplicationContext()).getWorkInfoByIdLiveData(slomoWorkRequest.getId())
                                            .removeObserver(this);
                                }
                                else if(workInfo.getState() == WorkInfo.State.FAILED){
                                    outString("SuperSlomo evaluation failed!");
                                    WorkManager.getInstance(getApplicationContext()).getWorkInfoByIdLiveData(slomoWorkRequest.getId())
                                            .removeObserver(this);
                                }
                            }
                        }
                    });

            new Thread(() -> {
//                Optional<String> videoExt = Utils.getExtensionByStringHandling(selectedFile);
//                if (!videoExt.isPresent()) {
//                    outString(selectedFile + " is not a video!");
//                    runningEval = false;
//                    return;
//                }

                // Prepare dirs
                extractedFramesDir = Paths.get(getApplicationContext()
                                .getFilesDir().getAbsolutePath(),
                                Constants.WORK_DIR,
                                videoName + "_extracted").toAbsolutePath().toFile();
                if (extractedFramesDir.exists()) {
                    for(File file : extractedFramesDir.listFiles()){
                        file.delete();
                    }
                }
                extractedFramesDir.mkdir();

                convertedFramesDir = Paths.get(getApplicationContext()
                                .getFilesDir().getAbsolutePath(),
                                Constants.WORK_DIR,
                                videoName + "_converted").toAbsolutePath().toFile();
                if (convertedFramesDir.exists()) {
                    for(File file : convertedFramesDir.listFiles()){
                        file.delete();
                    }
                }
                convertedFramesDir.mkdir();

                // Extract frames from video

                int[] widthHeight = Utils.getVideoSize(getApplicationContext(), fileUri);
                videoHeight = widthHeight[1];
                videoWidth = widthHeight[0];

                // Set to horizontal
                if (videoHeight > videoWidth) {
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

                outString("Frames extracted, base size: " + videoWidth + "x" + videoHeight);

//                Module frameInterp = loadPytorchModule(getFrameInterpFileForResolution(videoFrames.getOrigDim().x, videoFrames.getOrigDim().y));
//                slowMoEvaluator
//                        .videoFrames(videoFrames)
//                        .frameInterp(frameInterp)
//                        .imageWriter(new ImageWriter(convertedFramesDir.getAbsolutePath()));

                /*
                // Callback to handle SlomoWorker's result
                ListenableFuture<WorkInfo> future = WorkManager.getInstance(getApplicationContext())
                        .getWorkInfoById(slomoWorkRequest.getId());
                Futures.addCallback(
                        future,
                        new FutureCallback<WorkInfo>() {
                            @Override
                            public void onSuccess(@Nullable WorkInfo result) {

                            }

                            @Override
                            public void onFailure(Throwable t) {

                            }
                        },
                        // callback runs on a new thread
                        Executors.newSingleThreadExecutor()
                );
                */

                outString("Prepared frame dataset and slowmo evaluator, start conversion");

                runOnUiThread(() -> {
                    progressText.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                });

                // Inizia elaborazione
                //slowMoEvaluator.doEvaluation();
                // Enqueues slomoWorkRequest
                WorkManager.getInstance(this).enqueueUniqueWork(
                        "superSlomoEvaluation",
                        ExistingWorkPolicy.KEEP,
                        slomoWorkRequest);

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