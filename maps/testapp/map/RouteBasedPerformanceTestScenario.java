package com.yandex.maps.testapp.map;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.ConditionsListener;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.maps.testapp.driving.RouteView;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

public class RouteBasedPerformanceTestScenario extends PerformanceTestScenario {

    enum RouteStyle {
        Hidden,
        Normal,
        Navi
    }

    RouteBasedPerformanceTestScenario(
            Context context, MapWindow mapWindow, Callback callback, int routeId,
            RouteStyle routeStyle, float speed, float zoom, float azimuth, float tilt,
            String name, float durationInSeconds) {
        super(mapWindow, callback, name);
        this.context = context;
        this.routeId = routeId;
        this.routeStyle = routeStyle;
        this.speed = speed;
        this.zoom = zoom;
        this.azimuth = azimuth;
        this.tilt = tilt;
        this.durationInSeconds = durationInSeconds;
    }

    @Override
    protected void preExecute(final Runnable onComplete) {
        if (checkIfReady(onComplete)) {
            return;
        }

        byte[] data = loadData();
        DrivingRouter drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();
        route = drivingRouter.routeSerializer().load(data);

        route.addConditionsListener(new ConditionsListener() {
            @Override
            public void onConditionsUpdated() {
                if (areConditionsUpdated) {
                    return;
                }
                areConditionsUpdated = true;
                checkIfReady(onComplete);
            }

            @Override
            public void onConditionsOutdated() {
            }
        });

        makeScenario(onComplete);
    }

    private byte[] loadData() {
        BufferedInputStream stream = new BufferedInputStream(
                context.getResources().openRawResource(routeId));
        DataInputStream dataInputStream = new DataInputStream(stream);
        try {
            byte[] data = new byte[stream.available()];
            dataInputStream.readFully(data);
            return data;
        } catch (IOException e) {
            Log.e("yandex.maps", "Failed to load route from resource", e);
            return null;
        } finally {
            try {
                dataInputStream.close();
            } catch (IOException e) {
                Log.e("yandex.maps", "Failed to close stream", e);
            }
        }
    }

    private void makeScenario(Runnable onComplete) {
        List<Point> points = route.getGeometry().getPoints();
        if (points.size() <= 1) {
            return;
        }

        int pointNum = points.size();

        Point point = points.get(0);
        float totalTime = 0;
        while(totalTime < durationInSeconds) {
            steps.add(new PerformanceTestStep(
                    new CameraPosition(point, zoom, azimuth, tilt), 0.0f));
            float[] distance = new float[1];

            for (int i = 1; i < pointNum; ++i) {
                Point nextPoint = points.get(i);
                Location.distanceBetween(
                        point.getLatitude(), point.getLongitude(),
                        nextPoint.getLatitude(), nextPoint.getLongitude(),
                        distance);
                float time = distance[0] / speed;
                steps.add(new PerformanceTestStep(
                        new CameraPosition(nextPoint, zoom, azimuth, tilt), time));
                point = nextPoint;
                totalTime += time;
                if (totalTime >= durationInSeconds) {
                    break;
                }
            }
        }

        isScenarioReady = true;
        checkIfReady(onComplete);
    }

    private boolean checkIfReady(Runnable onComplete) {
        if ((areConditionsUpdated || (routeStyle == RouteStyle.Hidden)) && isScenarioReady) {
            start(onComplete);
            return true;
        }
        return false;
    }

    private void start(Runnable onComplete) {
        if (routeStyle != RouteStyle.Hidden) {
            routeView = new RouteView(mapWindow.getMap(), null, null);
            routeView.setRoute(route);
            if (routeStyle == RouteStyle.Navi) {
                NaviActivity.applyJamsStyle(routeView);
                NaviActivity.applyManeuverStyle(routeView);
                NaviActivity.applyPolylineStyle(routeView.getRoutePolyline());
            }
        }
        super.preExecute(onComplete);
    }

    @Override
    protected void postExecute() {
        if (routeStyle != RouteStyle.Hidden) {
            routeView.setRoute(null);
            routeView = null;
        }
    }

    private final Context context;
    private final int routeId;
    private DrivingRoute route;
    private final RouteStyle routeStyle;
    private final float speed;
    private final float zoom;
    private final float azimuth;
    private final float tilt;
    private final float durationInSeconds;

    private boolean areConditionsUpdated = false;
    private boolean isScenarioReady = false;

    private RouteView routeView;
}
