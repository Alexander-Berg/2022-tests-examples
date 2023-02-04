package com.yandex.maps.testapp.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.road_events_layer.RoadEventStyle;
import com.yandex.mapkit.road_events_layer.HighlightCircleStyle;
import com.yandex.mapkit.road_events_layer.HighlightMode;
import com.yandex.mapkit.road_events_layer.RoadEventStylingProperties;
import com.yandex.mapkit.road_events_layer.StyleProvider;
import com.yandex.mapkit.styling.roadevents.RoadEventsLayerDefaultStyleProvider;
import com.yandex.runtime.image.ImageProvider;

// TODO: simplify add / replace icon case in StyleProvider. MAPSROADEVENTS-228
public class RoadEventsLayerStyleProvider implements StyleProvider
{
    private static final float IMAGE_RESOURCES_SCALE_FACTOR = 4.0f;
    public static final String LOCAL_CHAT_IMAGE_ID = "local_chat";

    private static class ScaledImageProvider extends ImageProvider {
        private final Context context;
        private final int resourceId;
        private final float scale;
        private Bitmap bitmap;

        public ScaledImageProvider(Context context, int resourceId, float scale) {
            super(true);
            this.context = context;
            this.resourceId = resourceId;
            this.scale = scale;
        }

        @Override
        public String getId() { return "resource:" + resourceId + ",scale:" + scale; }

        @Override
        public Bitmap getImage() {
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
                bitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        Math.round(bitmap.getWidth() * scale),
                        Math.round(bitmap.getHeight() * scale),
                        true);
            }
            return bitmap;
        }
    }

    private ImageProvider bestImage(
            String imageId,
            boolean isSelected,
            boolean isNightMode,
            boolean isInFuture,
            float scaleFactor) {
        String fullName = (isSelected ? "event_pin_" : "event_poi_")
                + imageId
                + (isInFuture ? "_future" : "")
                + (isNightMode ? "_night" : "_day");

        int resourceId = context.getResources().getIdentifier(
                fullName, "drawable", context.getPackageName());

        if (resourceId == 0) {
            if (isNightMode) {
                // Try load 'day' image if 'night' version is missing, not the other way.
                return bestImage(
                        imageId,
                        isSelected,
                        false,
                        isInFuture,
                        scaleFactor);
            } else {
                return null;
            }
        }

        return new ScaledImageProvider(
                context,
                resourceId,
                scaleFactor / IMAGE_RESOURCES_SCALE_FACTOR);
    }

    private Context context;

    public RoadEventsLayerStyleProvider(Context context) {
        this.context = context;
        this.defaultStyleProvider = new RoadEventsLayerDefaultStyleProvider(context);
    }

    @Override
    public boolean provideStyle(
            @NonNull RoadEventStylingProperties roadEventStylingProperties,
            boolean isNightMode,
            float scaleFactor,
            @NonNull RoadEventStyle style) {
        boolean result = this.defaultStyleProvider.provideStyle(
                roadEventStylingProperties,
                isNightMode,
                scaleFactor,
                style
        );
        if (!result) {
            return false;
        }

        if (roadEventStylingProperties.getTags().size() == 1
                && roadEventStylingProperties.getTags().get(0) == EventTag.LOCAL_CHAT) {
            ImageProvider image = bestImage(
                    LOCAL_CHAT_IMAGE_ID,
                    roadEventStylingProperties.isIsSelected(),
                    isNightMode,
                    roadEventStylingProperties.isIsInFuture(),
                    scaleFactor);
            style.setIconImage(image);
        }
        return true;
    }

    @Override
    public HighlightCircleStyle provideHighlightCircleStyle(
            boolean isNightMode,
            HighlightMode highlightMode) {
        return this.defaultStyleProvider.provideHighlightCircleStyle(
            isNightMode,
            highlightMode
        );
    }

    private StyleProvider defaultStyleProvider;
}
