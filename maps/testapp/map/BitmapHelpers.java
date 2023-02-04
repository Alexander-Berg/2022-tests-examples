package com.yandex.maps.testapp.map;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.DisplayMetrics;

public class BitmapHelpers {
    public static float getDensity(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics.density;
    }

    public static Bitmap createFilledCircle(int color, int size, float density) {
        size = (int)(size * density + 0.5);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        float margin = 2f * density;
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        p.setAntiAlias(true);
        Canvas c = new Canvas(bitmap);
        //MARGIN is needed otherwise circle with antialiasing bigger then bitmap.
        c.drawCircle(size / 2, size / 2 , (size - margin) / 2, p);
        return bitmap;
    }

    public static Bitmap createFilledRect(int color, RectF rect, int backgroundColor, int size,
                                          float density) {
        size = (int)(size * density + 0.5);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(bitmap);
        c.drawColor(backgroundColor);

        Paint p = new Paint();
        RectF scaledRect = new RectF(
                rect.left * density, rect.top * density,
                rect.right * density, rect.bottom * density);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        c.drawRect(scaledRect, p);
        return bitmap;
    }
}
