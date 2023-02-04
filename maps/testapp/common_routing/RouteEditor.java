package com.yandex.maps.testapp.common_routing;

import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectDragListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RouteEditor implements MapObjectDragListener {

    private final MapObjectCollection mapObjects;
    private final List<PlacemarkMapObject> placemarkMapObjects;
    private PolylineMapObject polylineMapObject;
    private List<Point> polyline;

    public RouteEditor(MapObjectCollection mapObjects) {
        this.mapObjects = mapObjects.addCollection();
        placemarkMapObjects = new ArrayList<>();
    }

    public void startEdit(List<Point> points) {
        polyline = new ArrayList<>(points);

        for (Point p : polyline) {
            PlacemarkMapObject placemark = mapObjects.addPlacemark(p);
            placemark.setZIndex(10);
            placemark.setDraggable(true);
            placemark.setDragListener(this);
            placemarkMapObjects.add(placemark);
        }

        polylineMapObject = mapObjects.addPolyline(new Polyline(polyline));
        polylineMapObject.setZIndex(-10);
        polylineMapObject.setDashLength(2);
        polylineMapObject.setGapLength(2);
        polylineMapObject.setStrokeColor(Color.BLUE);
    }

    @Nullable public Polyline endEdit() {
        if (polylineMapObject == null)
            return null;
        Polyline geometry = polylineMapObject.getGeometry();
        mapObjects.clear();
        polyline = null;
        polylineMapObject = null;
        placemarkMapObjects.clear();
        return geometry;
    }

    @Override
    public void onMapObjectDragStart(@NonNull MapObject mapObject) {
    }

    @Override
    public void onMapObjectDrag(@NonNull MapObject mapObject, @NonNull Point point) {
    }

    @Override
    public void onMapObjectDragEnd(@NonNull MapObject mapObject) {
        for (int i = 0; i < placemarkMapObjects.size(); ++i) {
            if (placemarkMapObjects.get(i).equals(mapObject)) {
                polyline.set(i, placemarkMapObjects.get(i).getGeometry());
                polylineMapObject.setGeometry(new Polyline(polyline));
            }
        }
    }
}
