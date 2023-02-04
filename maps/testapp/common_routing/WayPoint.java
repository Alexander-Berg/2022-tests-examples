package com.yandex.maps.testapp.common_routing;

import android.graphics.Color;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectDragListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.common.internal.point_context.PointContextKt;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WayPoint implements MapObjectDragListener {

    public interface MoveListener {
        void onPlacemarkMoved();
    }

    private PlacemarkMapObject placemark;
    private final MoveListener moveListener;
    private List<PlacemarkMapObject> arrivalPlacemarks;
    private MapObjectCollection parentCollection;

    public WayPoint(
            Point point,
            char iconLetter,
            MapObjectCollection collection,
            MoveListener listener)
    {
        this.moveListener = listener;
        this.parentCollection = collection;

        final ImageProvider icon = IconHelper.createIconWithLetter(iconLetter, Color.RED);
        final ImageProvider apIcon = IconHelper.createIconWithLetter(iconLetter, Color.MAGENTA);

        placemark = IconHelper.addPlacemark(collection, point, icon, 100);
        placemark.setDraggable(true);
        placemark.setDragListener(this);

        arrivalPlacemarks = new ArrayList<>();
        for (Point ap : getApllPoints(point)) {
            PlacemarkMapObject arrivalPlacemark = IconHelper.addPlacemark(parentCollection, ap, apIcon, 100);
            arrivalPlacemarks.add(arrivalPlacemark);
        }
    }

    public RequestPoint getRequestPoint() {
        List<Point> arrivalPoints = new ArrayList<>();
        for (PlacemarkMapObject ap : arrivalPlacemarks) {
            arrivalPoints.add(ap.getGeometry());
        }
        return new RequestPoint(
                placemark.getGeometry(),
                RequestPointType.WAYPOINT,
                PointContextKt.encode(arrivalPoints));
    }

    public void remove() {
        parentCollection.remove(placemark);
        for (PlacemarkMapObject ap : this.arrivalPlacemarks) {
            parentCollection.remove(ap);
        }
    }

    private static List<Point> getApllPoints(Point point) {
        List<Point> arrivalPoints = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            double scale = 50 * (1 + i / 3) / 2;
            double arg = 2 * Math.PI * i / 5;
            double lonShift = scale * Math.cos(arg);
            double latShift = scale * Math.sin(arg);
            arrivalPoints.add(Utils.shiftPoint(point, latShift, lonShift));
        }
        return arrivalPoints;
    }

    @Override
    public void onMapObjectDragStart(MapObject mapObject) {
    }

    @Override
    public void onMapObjectDrag(MapObject mapObject, Point point) {
    }

    @Override
    public void onMapObjectDragEnd(MapObject mapObject) {
        List<Point> points = getApllPoints(placemark.getGeometry());
        assert points.size() == arrivalPlacemarks.size();
        for (int i = 0; i < points.size(); ++i) {
            this.arrivalPlacemarks.get(i).setGeometry(points.get(i));
        }
        moveListener.onPlacemarkMoved();
    }
}
