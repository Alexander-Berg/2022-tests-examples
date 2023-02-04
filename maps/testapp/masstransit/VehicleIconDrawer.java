package com.yandex.maps.testapp.masstransit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import androidx.collection.LruCache;
import com.yandex.mapkit.transport.masstransit.VehicleData;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.image.ImageProvider;

import java.util.HashMap;
import java.util.Map;

public class VehicleIconDrawer {

    private Context context;
    private Paint namePaint;

    /**
     * Colors
     */
    private int backgroundColor;
    private final Map<String, Integer> pressedBackgroundColors = new HashMap<>();

    /**
     * Bitmap cache
     */
    private LruCache<String, ImageProvider> iconCache = new LruCache<>(150);

    public VehicleIconDrawer(Context context) {
        this.context = context;

        backgroundColor = context.getResources().getColor(R.color.background_general_color);

        pressedBackgroundColors.put("bus", context.getResources().getColor(R.color.background_bus_color));
        pressedBackgroundColors.put("trolleybus", context.getResources().getColor(R.color.background_trolleybus_color));
        pressedBackgroundColors.put("tramway", context.getResources().getColor(R.color.background_tram_color));
        pressedBackgroundColors.put("minibus", context.getResources().getColor(R.color.background_minibus_color));
    }

    public synchronized ImageProvider getDrawableWithBackground(VehicleData vehicle,
                                                                int vehicleIconWidth,
                                                                boolean isGoingLeft,
                                                                boolean checked) {
        final String name = vehicle.getLine().getName();

        String type = "bus";
        for (String t : vehicle.getLine().getVehicleTypes())
            if (pressedBackgroundColors.containsKey(t)) {
                type = t;
                break;
            }

        final String bitmapKey = type + "_" + name + "_" +
                (isGoingLeft ? "left" : "right") + "_" +
                (checked ? "checked" : "not_checked");

        ImageProvider cachedProvider = iconCache.get(bitmapKey);
        if (cachedProvider != null) {
            return cachedProvider;
        }

        final Rect nameBounds = initAndCalculateMetrics(context, name, checked);

        final int textMargin = context.getResources().getDimensionPixelSize(R.dimen.vehicle_baloon_margin);

        Bitmap substrateBitmap = Bitmap.createBitmap(
            nameBounds.width() + textMargin * 2 + vehicleIconWidth / 2,
            nameBounds.height() + textMargin * 2,
            Bitmap.Config.ARGB_8888);

        Canvas substrateCanvas = new Canvas(substrateBitmap);
        drawBackgroundWithSubstrate(substrateCanvas, checked, type);

        int textX = textMargin;
        if (isGoingLeft)
            // Text is to the right of the vehicle's icon
            textX = textMargin + vehicleIconWidth / 2;

        substrateCanvas.drawText(name, textX, textMargin + substrateCanvas.getHeight() / 2,
            namePaint);

        ImageProvider provider = ImageProvider.fromBitmap(substrateBitmap);
        iconCache.put(bitmapKey, provider);

        return provider;
    }

    private Rect initAndCalculateMetrics(Context context, String name, boolean checked) {

        // init

        if (namePaint == null) {
            namePaint = new Paint();
            namePaint.setAntiAlias(true);
            namePaint.setStyle(Paint.Style.FILL);
            namePaint.setTextSize(context.getResources().getDimensionPixelSize(R.dimen.vehicle_balloon_text_size));
        }

        if (checked) {
            namePaint.setColor(context.getResources().getColor(R.color.vehicle_pressed_balloon_text_color));
        } else {
            namePaint.setColor(context.getResources().getColor(R.color.vehicle_balloon_text_color));
        }

        Rect nameBounds = new Rect();
        namePaint.getTextBounds(name, 0, name.length(), nameBounds);

        return nameBounds;
    }

    private void drawBackgroundWithSubstrate(Canvas substrateCanvas,
                                             boolean isPressed,
                                             String type) {
        Paint transparentPaint = new Paint();
        transparentPaint.setAlpha(0);

        Paint rectanglePaint = new Paint();
        rectanglePaint.setStyle(Paint.Style.FILL);

        if (isPressed) {
            rectanglePaint.setColor(pressedBackgroundColors.get(type));
        } else {
            rectanglePaint.setColor(backgroundColor);
        }

        final int rectangleCorners = context.getResources().getDimensionPixelSize(R.dimen.vehicle_rect_corners);
        final RectF rectangle = new RectF(0.f, 0.f, substrateCanvas.getWidth(), substrateCanvas.getHeight());
        substrateCanvas.drawRect(rectangle, transparentPaint);
        substrateCanvas.drawRoundRect(rectangle, rectangleCorners, rectangleCorners, rectanglePaint);
    }

}
