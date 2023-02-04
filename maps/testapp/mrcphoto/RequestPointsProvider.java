package com.yandex.maps.testapp.mrcphoto;

import android.graphics.Color;

import androidx.annotation.NonNull;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.common_routing.IconHelper;

import java.util.ArrayList;
import java.util.List;

class RequestPointsProvider implements InputListener {
    interface PointsReceiver {
        // Provide listener with points once they reach their limit.
        void receivePoints(List<Point> points);
    }

    private static final int ZINDEX = 30;

    private ArrayList<PointsReceiver> pointReceivers = new ArrayList<>();
    private int limit = 0;
    private MapView mapview = null;
    private MapObjectCollection mapUserPoints = null;
    private ArrayList<PlacemarkMapObject> placemarkPoints = new ArrayList<>();

    RequestPointsProvider(MapView mapview, int limit) {
        this.mapview = mapview;
        this.mapUserPoints = mapview.getMap().getMapObjects().addCollection();
        this.limit = limit;
        mapview.getMap().addInputListener(this);
    }

    public void subscribe(@NonNull PointsReceiver pointsReceiver) {
        if (!pointReceivers.contains(pointsReceiver)) {
            pointReceivers.add(pointsReceiver);
        }
    }

    public void unsubscribe(@NonNull PointsReceiver pointsReceiver) {
        if (pointReceivers.contains(pointsReceiver)) {
            pointReceivers.remove(pointsReceiver);
        }

        if (pointReceivers.isEmpty()) {
            clear();
        }
    }

    public void addPoint(Point point) {
        if (placemarkPoints.size() >= limit) {
            clear();
        }

        if (placemarkPoints.size() < limit) {
            char letter = (char) ('A' + placemarkPoints.size());
            Integer requestPointIndex = placemarkPoints.size();

            PlacemarkMapObject placeMark = IconHelper.addPlacemark(
                    mapUserPoints,point, IconHelper.createIconWithLetter(letter, Color.RED), ZINDEX);
            placeMark.setUserData(requestPointIndex);
            placemarkPoints.add(placeMark);
        }

        ArrayList<Point> points = new ArrayList<>();
        if (placemarkPoints.size() == limit) {
            for (PlacemarkMapObject requestPoint : placemarkPoints) {
                points.add(requestPoint.getGeometry());
            }

            for (PointsReceiver pointsReceiver : pointReceivers) {
                pointsReceiver.receivePoints(points);
            }
        }
    }

    public void clear() {
        placemarkPoints.clear();
        mapUserPoints.clear();
    }

    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
        if (!pointReceivers.isEmpty()) {
            addPoint(point);
        }
    }
}
