package com.yandex.maps.testapp.recovery;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.yandex.maps.testapp.FileUtils;
import com.yandex.maps.testapp.settings.SharedPreferencesConsts;
import com.yandex.runtime.recovery.CrashesTest;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.map.MapBaseActivity;

import java.io.File;

public class RecoveryActivity extends MapBaseActivity {

    private static final String RecoveryKey = "RECOVERY_FLAG";

    private static Boolean didRecoveryCrashOnPreviousExecution = null;

    private enum TypeOfCrash {NATIVE, ANDROID}

    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recovery);
        info = findViewById(R.id.info);

        didRecoveryCrashOnPreviousExecution(this);

        File file = new File(getRecoveryPath(this));
        if (!file.exists())
            file.mkdirs();

        Button createCrash = findViewById(R.id.make_crash);
        createCrash.setOnClickListener(view -> {
            if (createTestFile(TypeOfCrash.ANDROID.name())) {
                makeCrash();
            }
        });

        Button createNativeCrash = findViewById(R.id.make_native_crash);
        createNativeCrash.setOnClickListener(view -> {
            if (createTestFile(TypeOfCrash.NATIVE.name())) {
                makeNativeCrash();
            }
        });

        findViewById(R.id.make_assert_fail).setOnClickListener(view -> {
            RecoveryActivity.writeRecoveryFlag(this, true);
            CrashesTest.causeAssertFail();
        });

        findViewById(R.id.make_require_fail).setOnClickListener(view -> {
            RecoveryActivity.writeRecoveryFlag(this, true);
            CrashesTest.causeRequireFail();
        });

        findViewById(R.id.make_segfault).setOnClickListener(view -> {
            RecoveryActivity.writeRecoveryFlag(this, true);
            CrashesTest.causeSegfault();
        });

        findViewById(R.id.make_swallowed_exception).setOnClickListener(view -> {
            RecoveryActivity.writeRecoveryFlag(this, true);
            CrashesTest.causeSwallowedException();
        });

        findViewById(R.id.crash_with_crash_report).setOnClickListener(view -> {
            CrashesTest.causeAssertFail();
        });

        startRecoveryTesting();
    }

    public static String getRecoveryFile(Context context) {
        return getRecoveryPath(context) + getRecoveryFileName();
    }

    private void startRecoveryTesting() {
        info.setText("Check file after 5 seconds");
        new Thread(() -> {
            try {
                // runtime recovery actions is async
                // it need time for request
                Thread.sleep(5000);
                String recoveryFile = getRecoveryFile(RecoveryActivity.this);
                File file = FileUtils.testFile(recoveryFile);
                if (file == null) {
                    runOnUiThread(() -> info.setText("All clear!"));
                    return;
                }

                String typeOfCrash = FileUtils.readFromFile(file);
                if (typeOfCrash == null)
                    return;

                switch (TypeOfCrash.valueOf(typeOfCrash)) {
                    case ANDROID:
                        makeCrash();
                        break;
                    case NATIVE:
                        makeNativeCrash();
                        break;
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }).start();
    }

    public static boolean didRecoveryCrashOnPreviousExecution(Context context) {
        if (didRecoveryCrashOnPreviousExecution == null) {
            SharedPreferences sPref = getEnvironmentPrefs(context);
            didRecoveryCrashOnPreviousExecution = sPref.getBoolean(RecoveryKey, false);
            RecoveryActivity.writeRecoveryFlag(context, false);
        }

        return didRecoveryCrashOnPreviousExecution;
    }

    private static void writeRecoveryFlag(Context context, boolean enabled) {
        SharedPreferences sPref = getEnvironmentPrefs(context);
        SharedPreferences.Editor editor = sPref.edit();
        editor.putBoolean(RecoveryKey, enabled);
        editor.commit();
    }

    private static SharedPreferences getEnvironmentPrefs(final Context context) {
        return context.getSharedPreferences(SharedPreferencesConsts.RECOVERY_PREFS, Context.MODE_PRIVATE);
    }

    private void makeCrash() {
        RecoveryActivity.writeRecoveryFlag(this, true);
        throw new RuntimeException("This is a crash");
    }

    private void makeNativeCrash() {
        RecoveryActivity.writeRecoveryFlag(this, true);
        CrashesTest.causeRequireFail();
    }

    private static String getRecoveryPath(Context context) {
        return context.getExternalFilesDir(null).getAbsolutePath() + "/recovery_test";
    }

    private static String getRecoveryFileName() {
        return "/tiles.sqlite";
    }

    private boolean createTestFile(String typeOfCrash) {
        String recoveryFile = getRecoveryFile(this);
        File file = FileUtils.testFile(recoveryFile);
        return file != null || FileUtils.writeToFile(this, recoveryFile, typeOfCrash);
    }

    @Override
    protected void onStartImpl(){}
    @Override
    protected void onStopImpl(){}
}
