package com.yandex.maps.testapp.common_routing;

import android.os.Bundle;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.BoundingBox;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.map.MapBaseActivity;

public abstract class BaseRoutingActivity extends MapBaseActivity
    implements InputListener
{
    @Override
    public void onMapTap(Map map, Point point) {
        Utils.hideKeyboard(this);
        focusOnRoute();
    }

    protected abstract BoundingBox getRouteBoundingBox();

    protected void focusOnRoute() {
        final BoundingBox bbox = getRouteBoundingBox();
        if (bbox != null) {
            focusOnBoundingBox(bbox);
        }
    }

    protected void focusOnBoundingBox(BoundingBox bbox) {
        mapview.getMap().move(
            mapview.getMap().cameraPosition(inflatedBoundingBox(bbox, 1.3)),
            new Animation(Animation.Type.SMOOTH, 0.2f),
            null
        );
    }

    private static BoundingBox inflatedBoundingBox(BoundingBox bbox, double inflationFactor) {
        double minX = bbox.getSouthWest().getLatitude();
        double maxX = bbox.getNorthEast().getLatitude();
        double minY = bbox.getSouthWest().getLongitude();
        double maxY = bbox.getNorthEast().getLongitude();
        double dx = (maxX - minX) * (inflationFactor - 1) / 2;
        double dy = (maxY - minY) * (inflationFactor - 1) / 2;

        return new BoundingBox(new Point(minX - dx, minY - dy), new Point(maxX + dx, maxY + dy));
    }
}
