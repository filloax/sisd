package it.sisd.superslowmo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

public class CatturaActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> provider;

    private static final int PERMISSION_REQUEST_CODE = 200;

    private Button toggle_record_bt;
    private PreviewView pview;
    private ImageView imview;
    private ImageCapture imageCapt;
    private VideoCapture videoCapt;
    private ImageAnalysis imageAn;
    //private Recorder recorder;
    //private Recording recording;
    private boolean isRecording;

    public static String TAG = "HelloAndroidCamera";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cattura);

        if (!checkPermission())
            requestPermission();

        toggle_record_bt = findViewById(R.id.toggle_record_bt);
        pview = findViewById(R.id.previewView);

        toggle_record_bt.setOnClickListener(this::onClick);
        isRecording = false;

        provider = ProcessCameraProvider.getInstance(this);
        provider.addListener(() ->
        {
            try {
                ProcessCameraProvider cameraProvider = provider.get();
                startCamera(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return true;
    }

    private void requestPermission() {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();

                    // main logic
                } else {
                    Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_SHORT).show();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("You need to allow access permissions",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermission();
                                            }
                                        }
                                    });
                        }
                    }
                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(CatturaActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @SuppressLint("RestrictedApi")
    private void startCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector camSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(pview.getSurfaceProvider());

        videoCapt = new VideoCapture.Builder().setVideoFrameRate(25).setAudioChannelCount(0).build();
        // recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD)).build();

        cameraProvider.bindToLifecycle((LifecycleOwner) this, camSelector, preview, videoCapt);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    public void onClick(View view) {
        if (isRecording) {
            videoCapt.stopRecording();
            isRecording = false;
            toggle_record_bt.setText("Start Recording");
        } else {
            startCapture();
            isRecording = true;
            toggle_record_bt.setText("Stop Recording");
        }
    }

    @SuppressLint({"MissingPermission", "RestrictedApi"})
    public void startCapture() {
        //Es. SISDIG_2021127_189230.mp4
        String videoName = "SISDIG_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); // + ".mp4";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, videoName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

        VideoCapture.OutputFileOptions options = new VideoCapture.OutputFileOptions.Builder(getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues).build();

        videoCapt.startRecording(options, getExecutor(), new VideoCapture.OnVideoSavedCallback() {
            @Override
            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                Log.e(TAG, "Video saved successfully in " + outputFileResults.getSavedUri());
                Toast.makeText(getApplicationContext(), "Video saved successfully in " + outputFileResults.getSavedUri(), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                Log.e(TAG, "Video capture failed with error " + videoCaptureError + ": " + message);
                Toast.makeText(getApplicationContext(), "Video capture failed with error " + videoCaptureError + ": " + message, Toast.LENGTH_SHORT).show();
            }
        });
    }

}