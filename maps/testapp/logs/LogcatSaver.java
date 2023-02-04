package com.yandex.maps.testapp.logs;

import android.content.Context;
import android.util.Log;

import com.yandex.maps.testapp.FileUtils;
import com.yandex.maps.testapp.crashes.CrashController;

import java.io.File;
import java.io.IOException;

public class LogcatSaver {
    private static LogcatSaver instance = new LogcatSaver();

    private LogcatSaver() {
    }

    public static LogcatSaver getInstance() {
        return instance;
    }

    private Boolean started = false;
    private final String currentLogcat = "currentLogcat";
    private final String lastCrash = "lastCrash";
    private final String logsFolderName = "logs";
    private Context context;

    public String getLastCrashLogsPath() {
        File logcatFilesFolder = FileUtils.createFolder(context, logsFolderName);
        File savedLogcat = new File(logcatFilesFolder, lastCrash);

        if (!savedLogcat.exists()) {
            return null;
        }

        return savedLogcat.getAbsolutePath();
    }

    public void startSaving(Context context) {
        this.context = context;

        if (started)
            return;

        try {
            if (CrashController.getInstance().didCrashOnPreviousExecution()) {
                savePreviousLogcat();
            }

            File logcatFilesFolder = FileUtils.createFolder(context, logsFolderName);
            final File savedLogcat = new File(logcatFilesFolder, currentLogcat);

            FileUtils.clearFile(savedLogcat.getAbsolutePath());

            java.lang.Runtime.getRuntime().exec("logcat -T 1 -f " + savedLogcat.getAbsolutePath());
            started = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePreviousLogcat() {
        File logsFolder = FileUtils.createFolder(context, logsFolderName);
        File currentLogcatFile = new File(logsFolder, currentLogcat);
        File lastCrashFile = new File(logsFolder, lastCrash);

        if (lastCrashFile.exists()) {
            lastCrashFile.delete();
        }

        if (!currentLogcatFile.renameTo(lastCrashFile)) {
            Log.e("LogcatSaver", "Couldn't dump logcat file");
        }
    }
}
