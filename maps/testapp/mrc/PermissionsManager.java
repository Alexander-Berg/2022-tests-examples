package com.yandex.maps.testapp.mrc;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class PermissionsManager {
    private static final int PERMISSION_REQUEST_CODE_CAMERA = 1;
    private static final int PERMISSION_REQUEST_CODE_LOCATION = 2;

    public interface Listener {
        void onPermissionGranted(String permission);
        void onPermissionDenied(String permission);
    }

    private Activity activity;
    private Listener listener;

    public PermissionsManager(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public boolean haveCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public boolean haveLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    @TargetApi(23)
    public void requestCameraPermission() {
        doRequestPermissions(PERMISSION_REQUEST_CODE_CAMERA);
    }

    @TargetApi(23)
    public void requestLocationPermission() {
        doRequestPermissions(PERMISSION_REQUEST_CODE_LOCATION);
    }

    @TargetApi(23)
    public void onRequestPermissionsResult(
            int permsRequestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults){

        if (grantResults.length == 0) {
            // System sends onRequestPermissionsResult callback with empty length upon restart
            // e.g. on orientation change. We just ignore such callbacks
            return;
        }

        switch (permsRequestCode) {
            case PERMISSION_REQUEST_CODE_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listener.onPermissionGranted(Manifest.permission.CAMERA);
                    return;
                } else {
                    listener.onPermissionDenied(Manifest.permission.CAMERA);
                }
                break;
            case PERMISSION_REQUEST_CODE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    listener.onPermissionGranted(Manifest.permission.CAMERA);
                    return;
                } else {
                    listener.onPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION);
                }
                break;
        }
    }

    @TargetApi(23)
    private void doRequestPermissions(int permissionsRequestCode) {
        switch (permissionsRequestCode) {
            case PERMISSION_REQUEST_CODE_CAMERA:
                activity.requestPermissions(new String[]{Manifest.permission.CAMERA},
                        permissionsRequestCode);
                break;
            case PERMISSION_REQUEST_CODE_LOCATION:
                activity.requestPermissions(new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION},
                        permissionsRequestCode);
                break;
        }
    }
}
