package com.yandex.mail.tools;

import android.app.ActivityManager;
import android.content.Context;
import android.os.StrictMode;

import com.yandex.mail.BaseMailApplication;
import com.yandex.mail.util.NonInstantiableException;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class CommonTestUtils {

    private CommonTestUtils() {
        throw new NonInstantiableException();
    }

    /**
     * @return null if process with the specified name does not exist
     */
    @Nullable
    public static Integer getProcessId(@NonNull Context context, @NonNull String processName) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (processName.equals(process.processName)) {
                return process.pid;
            }
        }
        return null;
    }

    /**
     * Sorry for this method, it is super ugly, but there seems to be no way of restarting the application
     * running under instrumentation tests (that is, forcing onCreate to be called)
     */
    public static void resetAppState(@NonNull BaseMailApplication application) {
        // reinject new dependencies
        application.reinitComponent();

        // kill data managing process for the cleaner run (also it forces it to refresh dependencies next time it starts)
        // this code will be used if we switch on two processes again
        /*Integer dataManagingProcessPid = getProcessId(application, getDataManagingProcessName(application));
        if (dataManagingProcessPid != null) {
            android.os.Process.killProcess(dataManagingProcessPid);
        }*/
    }

    /**
     * Runs the runnable with disabled strict mode.
     * Restores policy on finish
     */
    public static void bypassStrictMode(@NonNull Runnable runnable) {
        StrictMode.ThreadPolicy policy = StrictMode.getThreadPolicy();
        try {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().permitAll().build());
            runnable.run();
        } finally {
            StrictMode.setThreadPolicy(policy);
        }
    }
}
