package com.yandex.maps.testapp.driving;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.yandex.runtime.image.ImageProvider;

public class IconWithLetter {
    static public ImageProvider iconWithLetter(char letter, int color, int size) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);

        Paint p = new Paint();
        p.setColor(color);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(7f);
        p.setAntiAlias(true);

        c.drawCircle(size / 2, size / 2, (size - 10) / 2, p);

        p.setColor(Color.BLACK);
        p.setStrokeWidth(1.5f);
        p.setTextSize(size * 0.45f);

        Rect textBounds = new Rect();
        p.getTextBounds(String.valueOf(letter), 0, 1, textBounds);

        c.drawText(
                String.valueOf(letter),
                (size - textBounds.right) / 2,
                (size - textBounds.top) / 2,
                p);

        return ImageProvider.fromBitmap(bitmap);
    }

    static public ImageProvider iconWithLetter(char letter, int color) {
        return iconWithLetter(letter, color, 40);
    }
}
