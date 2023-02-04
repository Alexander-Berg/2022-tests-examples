package com.yandex.maps.testapp.driving;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.yandex.runtime.Runtime;
import com.yandex.runtime.image.ImageProvider;

public class IconWithText {
    static public ImageProvider iconWithText(String text, int color, int textSize) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(Runtime.getApplicationContext().getResources().getDimensionPixelSize(textSize));
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);

        float baseline = -paint.ascent();
        float width = paint.measureText(text) + 0.5f;
        float height = baseline + paint.descent() + 0.5f;

        Bitmap bitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawText(text, 0, baseline, paint);

        return ImageProvider.fromBitmap(bitmap);
    }
}
