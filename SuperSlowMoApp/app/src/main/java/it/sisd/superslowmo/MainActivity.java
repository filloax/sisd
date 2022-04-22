package it.sisd.superslowmo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import it.sisd.pytorchreimpl.VideoDataset;

public class MainActivity extends AppCompatActivity {
    SlowMo slowMoEvaluator;
    boolean runningEval;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createSlowMoEvaluator();

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(this::buttonOnClick);
    }

    private void createSlowMoEvaluator() {
        try {
            Module flowCompCat = loadPytorchModule("flowCompCat.ptl");
//            Module arbTimeFlowIntrp = loadPytorchModule("ArbTimeFlowIntrp.ptl");

            VideoDataset<Tensor> videoFrames = SlowMo.createDataset(getApplicationContext(), Constants.IN_FRAMES_DIR);

            Module frameInterp = loadPytorchModule(getFrameInterpFileForResolution(videoFrames.getOrigDim().x, videoFrames.getOrigDim().y));

            slowMoEvaluator = new SlowMo(videoFrames, flowCompCat, frameInterp, this::outString);
        } catch (IOException e) {
            Log.e("SlowMo", "Error reading assets", e);
            // Per evitare errore "variabile potrebbe essere non inizializzata"
            throw new RuntimeException("Error reading assets!", e);
        }
    }

    private Module loadPytorchModule(String assetName) {
        return LiteModuleLoader.loadModuleFromAsset(getAssets(), assetName);
    }

    public void buttonOnClick(View v) {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_TIME;
        String out = "Premuto alle " + f.format(time);
        outString(out);

        if (!runningEval) {
            runningEval = true;
            new Thread() {
                public void run() {
                    slowMoEvaluator.doEvaluation();
                    runningEval = false;
                }
            }.start();
        } else {
            outString("Already running");
        }
    }

    private void outString(String s) {
        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(s + "\n" + tv.getText());
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