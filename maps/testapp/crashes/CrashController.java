package com.yandex.maps.testapp.crashes;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.yandex.maps.testapp.Environment;
import com.yandex.maps.testapp.FileUtils;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.recovery.RecoveryActivity;

import java.io.File;
import java.util.UUID;

public class CrashController {
    private String previousRunUuid = null;
    private boolean initCrashlytics = false;
    private static final CrashController INSTANCE = new CrashController();

    public static CrashController getInstance() {
        return INSTANCE;
    }

    public boolean needSendCrashReport(Context context) {
        return FileUtils.testFile(getLastCrashRunUuidPath(context)) != null;
    }

    public boolean didCrashOnPreviousExecution() {
        return FirebaseCrashlytics.getInstance().didCrashOnPreviousExecution();
    }

    public void initializeCrashlytics(Context context) {
        if (initCrashlytics)
            return;

        previousRunUuid = getPreviousRunUuid(context);
        if (previousRunUuid != null
                && FirebaseCrashlytics.getInstance().didCrashOnPreviousExecution()
                && !RecoveryActivity.didRecoveryCrashOnPreviousExecution(context)) {
            FileUtils.writeToFile(context, getLastCrashRunUuidPath(context), previousRunUuid);
        }

        final String runUuid = UUID.randomUUID().toString();
        if (FileUtils.writeToFile(context, getRunUuidPath(context), runUuid)) {
            FirebaseCrashlytics.getInstance().setCustomKey(Environment.environmentKey(context), Environment.readEnvironmentFromPreferences(context));
            FirebaseCrashlytics.getInstance().setCustomKey("runUuid", runUuid);
        } else {
            Toast.makeText(context, "Cannot write run uuid to file " + getRunUuidPath(context), Toast.LENGTH_LONG).show();
        }

        initCrashlytics = true;
    }

    @Nullable
    protected String previousRunUuid() {
        return previousRunUuid;
    }

    @Nullable
    protected String lastCrashRunUuid(Context context) {
        File file = FileUtils.testFile(getLastCrashRunUuidPath(context));
        if (file == null)
            return null;

        return FileUtils.readFromFile(file);
    }

    protected void markLastCrashAsReported(Context context) {
        File file = FileUtils.testFile(getLastCrashRunUuidPath(context));
        if (file != null) {
            if (!file.delete()) {
                Toast.makeText(context, "Cannot delete file with previous crash: " + getLastCrashRunUuidPath(context), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Nullable
    private String getPreviousRunUuid(Context context) {
        File file = FileUtils.testFile(getRunUuidPath(context));
        if (file == null)
            return null;

        return FileUtils.readFromFile(file);
    }

    private String getRunUuidPath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/run_uuid.txt";
    }

    private String getLastCrashRunUuidPath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/last_crash_run_uuid.txt";
    }

    private CrashController() {
    }
}
