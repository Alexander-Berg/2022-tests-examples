package com.yandex.maps.testapp.common_routing;

import android.graphics.Color;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.transport.navigation.Navigation;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.driving.IconWithLetter;
import com.yandex.maps.testapp.driving.IconWithText;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class RoutePointsController {
    private final MapObjectCollection mapObjects;
    private final Navigation navigation;

    private class ManeuverDescription {
        PlacemarkMapObject mapObject;
        String maneuvers;
    }

    private List<PlacemarkMapObject> placemarks;
    private HashMap<Double, ManeuverDescription>  maneuvers;

    private List<RequestPoint> requestPoints;
    private RequestPoint currentLocation;

    public RoutePointsController(MapObjectCollection mapObjects, Navigation navigation) {
        this.mapObjects = mapObjects;
        this.navigation = navigation;

        placemarks = new ArrayList<>();
        maneuvers = new HashMap<>();
        requestPoints = new ArrayList<>();
    }

    public List<RequestPoint> getRequestPoints() {
        if (currentLocation == null) {
            return requestPoints;
        } else {
            List<RequestPoint> points = new ArrayList<>();
            points.add(currentLocation);
            points.addAll(requestPoints);
            return points;
        }
    }

    public void addPlacemark(Point point, char letter) {
        PlacemarkMapObject routePoint = mapObjects.addPlacemark(point);
        routePoint.setIcon(IconWithLetter.iconWithLetter(letter, Color.RED));
        placemarks.add(routePoint);
    }

    private void updatePlacemarks() {
        clearPlacemarks();
        for (int i = 0; i < requestPoints.size(); ++i) {
            RequestPoint point = requestPoints.get(i);
            char letter = (char) ('A' + i % ('Z' - 'A'));
            addPlacemark(point.getPoint(), letter);
        }
    }

    private void updateCurrentLocationPoint() {
        Location currentLocation = navigation.getGuidance().getLocation();
        if (currentLocation == null) {
            return;
        }

        this.currentLocation = new RequestPoint(
            currentLocation.getPosition(),
            RequestPointType.WAYPOINT,
            null /* pointContext */);
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

    public void removeManeuver(Double position) {
        ManeuverDescription maneuver = maneuvers.get(position);
        if (maneuver != null) {
            mapObjects.remove(maneuver.mapObject);
            maneuvers.remove(position);
        }
    }

    public void addManeuver(Double position, Character type, Point p) {

        if (!maneuvers.containsKey(position)) {
            ManeuverDescription maneuverDescription = new ManeuverDescription();
            maneuverDescription.mapObject = mapObjects.addPlacemark(p);
            maneuverDescription.maneuvers = "";

            maneuvers.put(position, maneuverDescription);
        }

        ManeuverDescription maneuverDescription = maneuvers.get(position);
        if(maneuverDescription.maneuvers.isEmpty())
            maneuverDescription.maneuvers += type;
        else
            maneuverDescription.maneuvers += "+" + type;


        if (maneuverDescription.maneuvers.length() == 1)
            maneuverDescription.mapObject.setIcon(IconWithLetter.iconWithLetter(maneuverDescription.maneuvers.charAt(0) , Color.RED));
        else
            maneuverDescription.mapObject.setIcon(IconWithText.iconWithText(maneuverDescription.maneuvers , Color.RED, R.dimen.alternative_time_text_size));
    }

    public void clearPlacemarks() {
        for (PlacemarkMapObject point : placemarks) {
            mapObjects.remove(point);
        }
        placemarks.clear();

        for (ManeuverDescription maneuver : maneuvers.values()) {
            mapObjects.remove(maneuver.mapObject);
        }

        maneuvers.clear();
    }

    public void clear() {
        clearPlacemarks();
        requestPoints.clear();
    }
}

