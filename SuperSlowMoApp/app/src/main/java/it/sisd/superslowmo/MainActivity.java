package it.sisd.superslowmo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.pytorch.Module;
import org.pytorch.PyTorchAndroid;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class MainActivity extends AppCompatActivity {
    SlowMo slowMoEvaluator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Module flowComp, arbTimeFlowIntrp, backWarp;
        Bitmap[] frames;
        try {
            flowComp = PyTorchAndroid.loadModuleFromAsset(getAssets(), "flowComp.pt");
            arbTimeFlowIntrp = PyTorchAndroid.loadModuleFromAsset(getAssets(), "ArbTimeFlowIntrp.pt");
            frames = VideoConverter.getFrames(this);

            int resX = frames[0].getWidth(), resY = frames[0].getHeight();
            backWarp = PyTorchAndroid.loadModuleFromAsset(getAssets(), getBackWarpFileForResolution(resX, resY));
        } catch (IOException e) {
            Log.e("SlowMo", "Error reading assets", e);
            // Per evitare errore "variabile potrebbe essere non inizializzata"
            throw new RuntimeException("Error reading assets!", e);
        }

        slowMoEvaluator = new SlowMo(frames, flowComp, arbTimeFlowIntrp, backWarp, this::outString);

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(this::buttonOnClick);
    }

    public void buttonOnClick(View v) {
        LocalDateTime time = LocalDateTime.now();
        DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_TIME;
        String out = "Premuto alle " + f.format(time);
        outString(out);

        slowMoEvaluator.doEvaluation();
    }

    private void outString(String s) {
        TextView tv = (TextView) findViewById(R.id.textView);
        tv.setText(s + "\n" + tv.getText());
    }

    private static String getBackWarpFileForResolution(int x, int y) {
        // Sostituire con classe a parte? Per ora check hardcoded con unica
        // risoluzione disponibile
        if (x == 1280 && y == 720) {
            return "flowBackWarp_1280x720.pt";
        }

        throw new IllegalArgumentException("No backwarp model available for resolution " + x + "x" + y);
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