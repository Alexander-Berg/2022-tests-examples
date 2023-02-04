package com.yandex.maps.testapp.directions_navigation;

import android.graphics.Color;

import com.yandex.mapkit.directions.driving.ConditionsListener;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.RoutePoint;
import com.yandex.mapkit.directions.driving.StandingSegment;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.maps.testapp.driving.IconWithLetter;

import java.util.ArrayList;
import java.util.List;

public class ArrivalPointsView implements RouteListener, VisibilitySetting {

    private static final int CIRCLE_SIZE = 50;
    private static final float Z_INDEX = 101;


    private List<PlacemarkMapObject> placemarks = new ArrayList<>();
    private final MapObjectCollection mapObjectCollection;

    private DrivingRoute drivingRoute = null;
    private boolean visible = false;

    public ArrivalPointsView(MapObjectCollection mapObjectCollection) {
        this.mapObjectCollection = mapObjectCollection;
    }

    @Override
    public boolean getVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        update();
    }

    @Override
    public void onCurrentRouteChanged(DrivingRoute newRoute) {
        drivingRoute = newRoute;
        update();
    }

    private void update() {
        hidePoints();
        if (visible && drivingRoute != null)
            showPoints();
    }

    private void hidePoints() {
        for (PlacemarkMapObject placemark : placemarks) {
            mapObjectCollection.remove(placemark);
        }
        placemarks.clear();
    }

    private void showPoints() {
        assert drivingRoute != null;
        for (RoutePoint point : drivingRoute.getMetadata().getRoutePoints()) {
            if (point.getSelectedArrivalPoint() != null) {
                PlacemarkMapObject placemark = mapObjectCollection.addPlacemark(point.getSelectedArrivalPoint());
                placemark.setIcon(IconWithLetter.iconWithLetter(' ', Color.GREEN, CIRCLE_SIZE));
                placemark.setZIndex(Z_INDEX);
                placemarks.add(placemark);
            }
            if (point.getDrivingArrivalPointId() != null) {
                List<Point> polyline = drivingRoute.getGeometry().getPoints();
                PlacemarkMapObject placemark = mapObjectCollection.addPlacemark(
                        polyline.get(polyline.size() - 1));
                placemark.setIcon(IconWithLetter.iconWithLetter(
                        point.getDrivingArrivalPointId().charAt(0),
                        Color.YELLOW, CIRCLE_SIZE));
                placemark.setZIndex(Z_INDEX);
                placemarks.add(placemark);
            }
        }
    }
}
