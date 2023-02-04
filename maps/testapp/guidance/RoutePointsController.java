package com.yandex.maps.testapp.guidance;

import android.graphics.Color;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.directions.guidance.ClassifiedLocation;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.maps.testapp.driving.IconWithLetter;

import java.util.List;
import java.util.ArrayList;

public class RoutePointsController {
    private final MapObjectCollection mapObjects;
    private final Guide guide;

    private List<PlacemarkMapObject> placemarks;
    private List<RequestPoint> requestPoints;

    public RoutePointsController(MapObjectCollection mapObjects, Guide guide) {
        this.mapObjects = mapObjects;
        this.guide = guide;

        placemarks = new ArrayList<>();
        requestPoints = new ArrayList<>();
    }

    public List<RequestPoint> getRequestPoints() {
        return requestPoints;
    }

    private void addPlacemark(Point point, int color, char letter) {
        PlacemarkMapObject routePoint = mapObjects.addPlacemark(point);
        routePoint.setIcon(IconWithLetter.iconWithLetter(letter, color));
        placemarks.add(routePoint);
    }

    private void updatePlacemarks() {
        clearPlacemarks();
        for (int i = 0; i < requestPoints.size(); ++i) {
            RequestPoint point = requestPoints.get(i);
            char letter = (char) ('A' + i % ('Z' - 'A'));
            addPlacemark(point.getPoint(), Color.RED, letter);
        }
    }

    private void updateCurrentLocationPoint() {
        ClassifiedLocation currentLocation = guide.getLocation();
        if (currentLocation == null || currentLocation.getLocation() == null) {
            return;
        }
        RequestPoint point = new RequestPoint(
                currentLocation.getLocation().getPosition(),
                RequestPointType.WAYPOINT,
                null /* pointContext */);
        if (requestPoints.size() == 0) {
            requestPoints.add(point);
        } else {
            requestPoints.set(0, point);
        }
    }

    public void addPoint(RequestPoint point) {
        updateCurrentLocationPoint();
        requestPoints.add(point);
        updatePlacemarks();
    }

    public void setPoints(List<RequestPoint> points) {
        requestPoints = points;
        updatePlacemarks();
    }

    public void clearPlacemarks() {
        for (PlacemarkMapObject point : placemarks) {
            mapObjects.remove(point);
        }
        placemarks.clear();
    }

    public void clear() {
        clearPlacemarks();
        requestPoints.clear();
    }
}
