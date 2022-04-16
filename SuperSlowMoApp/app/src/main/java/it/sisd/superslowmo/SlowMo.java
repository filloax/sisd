package it.sisd.superslowmo;

import android.graphics.Bitmap;
import android.util.Log;

import org.pytorch.Module;
import org.pytorch.torchvision.TensorImageUtils;

import java.util.function.Consumer;

public class SlowMo {
    private Bitmap[] inFrames;
    private Bitmap[] outFrames;
    private Module flowComp;
    private Module arbTimeFlowIntrp;
    private Module backWarp;
    private int frameResolutionX;
    private int frameResolutionY;

    private Consumer<String> logOut;

    public SlowMo(Bitmap[] inFrames, Module flowComp, Module arbTimeFlowIntrp, Module backWarp, Consumer<String> logOut) {
        this.inFrames = inFrames;
        this.outFrames = null;
        this.flowComp = flowComp;
        this.arbTimeFlowIntrp = arbTimeFlowIntrp;
        this.backWarp = backWarp;
        this.logOut = logOut;

        frameResolutionX = this.inFrames[0].getWidth();
        frameResolutionY = this.inFrames[0].getHeight();
    }

    public SlowMo(Bitmap[] inFrames, Module flowComp, Module arbTimeFlowIntrp, Module backWarp) {
        this(inFrames, flowComp, arbTimeFlowIntrp, backWarp, null);
    }

    public void doEvaluation() {
        log("Starting SuperSloMo eval");



        log("Ended SuperSloMo eval");
    }

    private void log(String s) {
        if (logOut != null)
            logOut.accept(s);
        else
            Log.println(0, "SlowMo", s);
    }
}
