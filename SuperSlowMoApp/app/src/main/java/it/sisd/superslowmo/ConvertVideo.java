package it.sisd.superslowmo;

import static it.sisd.superslowmo.Constants.*;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Size;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;

import java.util.Locale;
import java.util.function.Consumer;

public class ConvertVideo {
    private Consumer<String> log;

    public ConvertVideo(Consumer<String> log) {
        this.log = log;
    }

    public ConvertVideo() { this(null); }

    public boolean extractFrames(String inVideoPath, String outFramesDir) {
        // US Locale for decimal separator used by ffmpeg
        String command = String.format(Locale.US,
            "-i %s -vsync 0 %s/%%06d.png",
            inVideoPath, outFramesDir
        );
        FFmpegSession session = FFmpegKit.execute(command);

        if (log != null) {
            log.accept("Finished ffmpeg extractFrames session! Log: " + session.getOutput());
        }

        return stdCheckReturnCode(session, "extractFrames");
    }

    public boolean extractFramesAndResize(String inVideoPath, String outFramesDir, int newWidth, int newHeight) {
        // US Locale for decimal separator used by ffmpeg
        String command = String.format(Locale.US,
                "-i %s -vsync 0 -vf \"scale=%d:%d\" %s/%%06d.png",
                inVideoPath, newWidth, newHeight, outFramesDir
        );
        FFmpegSession session = FFmpegKit.execute(command);

        if (log != null) {
            log.accept("Finished ffmpeg extractFramesAndResize session! Log: " + session.getOutput());
        }

        return stdCheckReturnCode(session, "extractFramesAndResize");
    }

    public boolean createVideo(String inFramesDir, String outVideoPath, float fps) {
        // US Locale for decimal separator used by ffmpeg
        String command = String.format(Locale.US,
            "-y -r %.2f -i %s/%%d.png \"%s\"",
            fps, inFramesDir, outVideoPath
        );
        FFmpegSession session = FFmpegKit.execute(command);

        if (log != null) {
            log.accept("Finished ffmpeg createVideo session! Log: " + session.getOutput());
        }

        return stdCheckReturnCode(session, "createVideo");
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
}
