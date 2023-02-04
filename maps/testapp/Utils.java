package com.yandex.maps.testapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.yandex.mapkit.geometry.Point;
import com.yandex.runtime.Error;

import java.io.InputStream;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Utils {
    public static void showMessage(Context context, String message, Level level, Logger logger) {
        if (level != Level.OFF && logger != null)
            logger.log(level, message);
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showMessage(Context context, String message) {
        showMessage(context, message, Level.OFF, null);
    }

    public static void showError(Context context, Error error) {
        showMessage(context, "Error: " + error.getClass().getName());
    }

    public static void showError(Context context, Throwable error) {
        showMessage(context, "Error: " + error.getClass().getName());
    }

    public static void hideKeyboard(Activity instance) {
        View view = instance.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager)
                    instance.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(view.getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }

    public static Bitmap createPlacemarkBitmap(int color, int size, Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        size = (int)(size * metrics.density + 0.5);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        float strokeWidth = 10f * metrics.density;
        float margin = 2f * metrics.density;
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(strokeWidth);
        p.setAntiAlias(true);
        Canvas c = new Canvas(bitmap);
        //MARGIN is needed otherwise circle with antialiasing bigger then bitmap.
        c.drawCircle(size / 2, size / 2 , (size - (strokeWidth + margin)) / 2, p);
        return bitmap;
    }

    public static String readResourceAsString(Context context, int resourceId) {
        Scanner s = null;
        try {
            InputStream is = context.getResources().openRawResource(resourceId);
            s = new Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        } finally {
            if (s != null) {
                s.close();
            }
        }
    }

    public static float lerp(float t, float minValue, float maxValue) {
        return minValue * (1 - t) + maxValue * t;
    }

    public static float clamp(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(maxValue, value));
    }


    static final double WGS84RADIUS = 6378137.0;
    static double LAT_CONVERSION = WGS84RADIUS*Math.PI/180.;
    private static double metersToDlat(double len) {
        return len / LAT_CONVERSION;
    }

    private static double metersToDlon(double len, Point point) {
        return metersToDlat(len) / Math.cos(point.getLatitude() * Math.PI / 180.);
    }

    public static Point shiftPoint(Point point, double lenLat, double lenLon) {
        return new Point(point.getLatitude() + metersToDlat(lenLat),
                point.getLongitude() + metersToDlon(lenLon, point));
    }

    public static AlertDialog createRestartDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Restart the app")
                .setMessage("Application will be restarted for apply changes")
                .setNegativeButton("I understand and won't file a bug report",
                        (dialog, id) -> {
                            dialog.cancel();
                        });
        return builder.create();
    }

    public static boolean appInfo(Context context, String key) {
        try {
            Bundle metadata = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA).metaData;
            return metadata.getBoolean(key);
        } catch (Exception e) {
            return false;
        }
    }
}
