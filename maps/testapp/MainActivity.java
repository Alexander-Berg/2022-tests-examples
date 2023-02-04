package com.yandex.maps.testapp;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.media.AudioManager;

import com.yandex.maps.testapp.crashes.CrashController;
import com.yandex.maps.testapp.crashes.CrashReportActivity;
import com.yandex.maps.testapp.logs.LogController;
import com.yandex.maps.testapp.logs.LogcatSaver;

public class MainActivity extends PreferenceActivity {
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private boolean shouldCheckLocationPermissions = true;

    @Override
    protected void onStart() {
        super.onStart();

        MapkitAdapter.onStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Here, thisActivity is the current activity
        if (shouldCheckLocationPermissions &&
                ContextCompat.checkSelfPermission(this,
                "android.permission.ACCESS_FINE_LOCATION")
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{"android.permission.ACCESS_FINE_LOCATION"},
                        PERMISSIONS_REQUEST_FINE_LOCATION);
        }

        MapkitAdapter.onResume(this);
    }

    @Override
    protected void onPause() {
        MapkitAdapter.onPause(this);

        super.onPause();
    }

    @Override
    protected void onStop() {
        MapkitAdapter.onStop(this);
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogcatSaver.getInstance().startSaving(getApplicationContext());

        MainApplication.initialize();
        addPreferencesFromResource(R.xml.main);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); //TTS

        CrashController.getInstance().initializeCrashlytics(this);
        final boolean forceSendCrashReport = Utils.appInfo(this, "yandex.maps.force_send_crash_report");
        if (CrashController.getInstance().needSendCrashReport(this) && forceSendCrashReport)
            CrashReportActivity.sendCrashReport(this, false);
    }

    @Override
    protected void onDestroy() {
        LogController.getInstance().hideNotification();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_FINE_LOCATION) {
            shouldCheckLocationPermissions = false;
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
