package com.yandex.maps.testapp.common_routing;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.runtime.image.ImageProvider;

/**
 * Created by mbulva on 22.02.2018.
 */

public class IconHelper {
    public static ImageProvider createIconWithLetter(char letter, int color) {
        final int size = 40;
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
        p.setTextSize(18);

        Rect textBounds = new Rect();
        p.getTextBounds(String.valueOf(letter), 0, 1, textBounds);

        c.drawText(
                String.valueOf(letter),
                (size - textBounds.right) / 2,
                (size - textBounds.top) / 2,
                p);

        return ImageProvider.fromBitmap(bitmap);
    }

    public static PlacemarkMapObject addPlacemark(
            MapObjectCollection collection,
            Point point,
            ImageProvider icon,
            int zIndex
    ) {
        if (point == null)
            return null;
        PlacemarkMapObject placemark = collection.addPlacemark(point);
        placemark.setIcon(icon);
        placemark.setZIndex(zIndex);
        return placemark;
    }
}
