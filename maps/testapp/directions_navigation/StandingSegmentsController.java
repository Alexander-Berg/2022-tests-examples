package com.yandex.maps.testapp.directions_navigation;

import com.yandex.mapkit.directions.driving.ConditionsListener;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.StandingSegment;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;

import java.util.ArrayList;
import java.util.List;

public class StandingSegmentsController implements RouteListener, VisibilitySetting {
    private static final int SEGMENT_POLYLINE_COLOR = 0xFF404040;
    private static final float SEGMENT_POLYLINE_Z_INDEX = 2.0f;
    private static final float SEGMENT_DEFAULT_WIDTH = 8.0f;
    private static final float SEGMENT_ADDITIONAL_WIDTH = 6.0f;

    private final List<PolylineMapObject> standingSegments = new ArrayList<>();
    private final MapObjectCollection mapObjectCollection;

    private DrivingRoute drivingRoute = null;
    private boolean visible = false;

    private float width = SEGMENT_DEFAULT_WIDTH;

    public StandingSegmentsController(MapObjectCollection mapObjectCollection) {
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

    private void update() {
        hideStandingSegments();
        if (visible && drivingRoute != null)
            showStandingSegments();
    }

    @Override
    public void onCurrentRouteChanged(DrivingRoute newRoute) {
        onCurrentRouteChanged(newRoute, SEGMENT_DEFAULT_WIDTH);
    }

    public void onCurrentRouteChanged(DrivingRoute newRoute, float routeWidth) {
        hideStandingSegments();

        width = routeWidth;
        drivingRoute = newRoute;

        if (drivingRoute == null)
            return;

        if (visible)
            showStandingSegments();

        drivingRoute.addConditionsListener(new ConditionsListener() {
            @Override
            public void onConditionsUpdated() {
                update();
            }

            @Override
            public void onConditionsOutdated() { }
        });
    }

    private void hideStandingSegments() {
        for(PolylineMapObject polyline : standingSegments) {
            mapObjectCollection.remove(polyline);
        }
        standingSegments.clear();
    }

    private void showStandingSegments() {
        assert drivingRoute != null;
        for (StandingSegment segment : drivingRoute.getStandingSegments()) {
            List<Point> standingSegmentGeometry = SubpolylineHelper.subpolyline(
                    drivingRoute.getGeometry(),
                    segment.getPosition()
            ).getPoints();
            PolylineMapObject polyline = mapObjectCollection.addPolyline(
                    new Polyline(standingSegmentGeometry)
            );
            polyline.setStrokeColor(SEGMENT_POLYLINE_COLOR);
            polyline.setStrokeWidth(width +  SEGMENT_ADDITIONAL_WIDTH);
            polyline.setZIndex(SEGMENT_POLYLINE_Z_INDEX);
            standingSegments.add(polyline);
        }
    }
}
