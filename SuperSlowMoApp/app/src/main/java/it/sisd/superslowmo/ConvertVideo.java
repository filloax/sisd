package it.sisd.superslowmo;

import static it.sisd.superslowmo.Constants.*;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Size;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ConvertVideo {
    private Consumer<String> log;

    /*
    0 = 90CounterCLockwise and Vertical Flip (default)
    1 = 90Clockwise
    2 = 90CounterClockwise
    3 = 90Clockwise and Vertical Flip
     */
    private int transpose = -1;
    private int resizeWidth = -1;
    private int resizeHeight = -1;

    public ConvertVideo(Consumer<String> log) {
        this.log = log;
    }

    public ConvertVideo() { this(null); }

    public void resetResize() {
        resizeWidth = -1;
        resizeHeight = -1;
    }

    public void setResize(int width, int height) {
        resizeWidth = width;
        resizeHeight = height;
    }

    public void resetRotate() {
        transpose = -1;
    }

    public void rotateClockwise() {
        transpose = 1;
    }

    public void rotateCounterClockwise() {
        transpose = 2;
    }

    public boolean extractFrames(String inVideoPath, String outFramesDir) {
        String src = formatString("-i %s", inVideoPath);
        String params = "-vsync 0";
        String dest = formatString("%s/%%06d.png", outFramesDir);

        String command = String.join(" ", src, params, getVFilters(), dest);
        FFmpegSession session = FFmpegKit.execute(command);
        if (log != null) {
            log.accept("Finished ffmpeg extractFrames session! Log: " + session.getOutput());
        }

        return stdCheckReturnCode(session, "extractFrames");
    }

    public boolean createVideo(String inFramesDir, String outVideoPath, float fps) {
        String src = formatString("-i %s/%%d.png", inFramesDir);
        String params = formatString("-y -r %.2f", fps);
        String dest = formatString("\"%s\"", outVideoPath);

        String command = String.join(" ", src, params, getVFilters(), dest);
        FFmpegSession session = FFmpegKit.execute(command);
        if (log != null) {
            log.accept("Finished ffmpeg createVideo session! Log: " + session.getOutput());
        }

        return stdCheckReturnCode(session, "createVideo");
    }

    private String getVFilters() {
        List<String> vfilter = new ArrayList<>();

        // Rotazione PRIMA di scalare, importante

        if (transpose != -1) {
            vfilter.add(formatString("transpose=%d", transpose));
        }

        if (resizeWidth != -1 || resizeHeight != -1) {
            // -1 is already "as existing" in ffmpeg
            vfilter.add(formatString("scale=%d:%d", resizeWidth, resizeHeight));
        }

        if (vfilter.size() > 0) {
            return "-vf \"" + String.join(",", vfilter) + "\"";
        }
        return "";
    }


    private boolean stdCheckReturnCode(FFmpegSession session, String functionName) {
        if (ReturnCode.isSuccess(session.getReturnCode())) {
            return true;

        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            Log.w(LOG_TAG, String.format(
                    "%s: conversion cancelled! state %s and rc %s.%s",
                    functionName,
                    session.getState(),
                    session.getReturnCode(),
                    session.getFailStackTrace()
            ));
            return false;

        } else {
            // FAILURE
            Log.e(LOG_TAG,
                    String.format("%s: Command failed with state %s and rc %s.%s",
                            functionName,
                            session.getState(),
                            session.getReturnCode(),
                            session.getFailStackTrace()
                    )
            );
            return false;
        }
    }

    // US Locale for decimal separator used by ffmpeg
    private String formatString(String format, Object... args) {
        return String.format(Locale.US, format, args);
    }
}
