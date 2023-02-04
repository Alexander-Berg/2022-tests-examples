package com.yandex.maps.testapp.common_routing;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.MapObjectCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;


public class PlacemarkManager {

    private final MapObjectCollection collection;
    private final WayPoint.MoveListener moveListener;
    private Stack<WayPoint> wayPoints = new Stack<>();

    public PlacemarkManager(
        MapObjectCollection collection,
        WayPoint.MoveListener moveListener)
    {
        this.collection = collection;
        this.moveListener = moveListener;
    }

    public void reset() {
        for (WayPoint wayPoint : wayPoints) {
            wayPoint.remove();
        }
        wayPoints.clear();
    }

    public void reset(List<Point> points) {
        reset();
        for (Point point : points) {
            append(point);
        }
    }

    public List<RequestPoint> getRequestPoints() {
        List<RequestPoint> requestPoints = new ArrayList<>();
        for (WayPoint wayPoint : wayPoints) {
            requestPoints.add(wayPoint.getRequestPoint());
        }
        return requestPoints;
    }

    public boolean isReady() {
        return wayPoints.size() >= 2;
    }

    public void append(Point point) {
        final char letter = (char)('A' + wayPoints.size());
        wayPoints.add(new WayPoint(point, letter, collection, moveListener));
    }
}
