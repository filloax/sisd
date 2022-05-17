package it.sisd.superslowmo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import it.sisd.pytorchreimpl.VideoDataset;

public class SlomoActivity extends AppCompatActivity {
    SlowMo slowMoEvaluator;
    boolean runningEval = false;
    boolean loadedSlowMoEvaluator = false;
    Button startButton = null;
    String selectedFile = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slomo);

        startButton = findViewById(R.id.startElabButton);
        startButton.setEnabled(false);
        startButton.setOnClickListener(this::startElabOnClick);

        final Button chooseFileButton = findViewById(R.id.fileSelectButton);

        createSlowMoEvaluator();
    }

    private void createSlowMoEvaluator() {
        outString("Initializing SlowMo service...");

        new Thread( () -> {
            try {
                Module flowCompCat = loadPytorchModule("flowCompCat.ptl");
    //            Module arbTimeFlowIntrp = loadPytorchModule("ArbTimeFlowIntrp.ptl");

                VideoDataset<Tensor> videoFrames = SlowMo.createDataset(getApplicationContext(), Constants.IN_FRAMES_DIR);

                Module frameInterp = loadPytorchModule(getFrameInterpFileForResolution(videoFrames.getOrigDim().x, videoFrames.getOrigDim().y));

                final File outDir = Paths.get(this.getApplicationContext().getFilesDir().getAbsolutePath(), Constants.OUT_FRAMES_DIR)
                        .toAbsolutePath().toFile();

                if (!outDir.exists()) {
                    outDir.mkdir();
                }

                slowMoEvaluator = new SlowMo().videoFrames(videoFrames)
                        .flowCompCat(flowCompCat)
                        .frameInterp(frameInterp)
                        .logOut(this::outString)
                        .imageWriter((name, bitmap) -> {
                            String path = new File(outDir, name).toString();
                            try (FileOutputStream out = new FileOutputStream(path)) {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                                // PNG is a lossless format, the compression factor (100) is ignored
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                runOnUiThread(() -> startButton.setEnabled(true));
                loadedSlowMoEvaluator = true;
                outString("Loaded SlowMo service!");
            } catch (IOException e) {
                Log.e("SlowMo", "Error reading assets", e);
                outString("Error reading assets, cannot start");
            }
        }).start();
    }

    public void loadFileOnClick(View w) {
        // TODO
    }

    public void startElabOnClick(View v) {
        if (!loadedSlowMoEvaluator)
            outString("Premuto il pulsante prima che sia caricato il valutatore");

        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_TIME;
        String out = "Premuto alle " + f.format(time);
        outString(out);

        if (!runningEval) {
            runningEval = true;
            final File outDir = Paths.get(this.getApplicationContext().getFilesDir().getAbsolutePath(), Constants.OUT_FRAMES_DIR)
                    .toAbsolutePath().toFile();
            outString("Saving to dir " + outDir);
            new Thread() {
                public void run() {
                    slowMoEvaluator.doEvaluation();
                    runningEval = false;
                    outString("Saved to dir " + outDir);
                }
            }.start();
        } else {
            outString("Already running");
        }
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

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}