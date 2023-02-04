package com.yandex.maps.testapp.directions_navigation;

import android.graphics.Color;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.internal.RouteUtils;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationSimulator;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.LocationSimulatorListener;
import com.yandex.mapkit.location.SimulationAccuracy;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.runtime.Error;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SimulationController implements LocationSimulatorListener {
    private static final double DEFAULT_SPEED_VALUE = 20; // m/s
    private static final DrivingRouter router = DirectionsFactory.getInstance().createDrivingRouter();

    private final MapObjectCollection objects;

    private LocationSimulator locationSimulator;
    private DrivingRoute currentRoute;
    private DrivingSession session;
    private PolylineMapObject trajectoryPolyline;

    private final Listener listener;

    private Double speedValue;
    private Boolean locationSpeedProvidingValue;

    public interface Listener {
        void onSimulationStarted();
        void onSimulationStopped();
        void onSimulationFinished();
    }

    public SimulationController(MapObjectCollection objects, Listener listener) {
        this.objects = objects;
        this.listener = listener;
    }

    public void setSpeed(double speed) {
        speedValue = speed;
        if (isSimulationActive()) {
            locationSimulator.setSpeed(speed);
        }
    }

    public double getSpeed() {
        return speedValue != null ? speedValue : DEFAULT_SPEED_VALUE;
    }

    static private Polyline remainingGeometry(@NonNull DrivingRoute route) {
        PolylinePosition position = route.getPosition();
        Polyline geometry = route.getGeometry();
        List<Point> points = geometry.getPoints();
        ArrayList<Point> newPoints = new ArrayList<Point>();
        newPoints.add(PolylineUtils.pointByPolylinePosition(geometry, position));
        for (int i = position.getSegmentIndex() + 1; i < points.size(); ++i) {
            newPoints.add(points.get(i));
        }
        return new Polyline(newPoints);
    }

    public void startSimulation(@NonNull DrivingRoute route) {
        currentRoute = route;
        Polyline geometry = remainingGeometry(route);

        locationSimulator = MapKitFactory.getInstance().createLocationSimulator(geometry);
        locationSimulator.subscribeForSimulatorEvents(this);
        if (speedValue == null) {
            speedValue = locationSimulator.getSpeed();
        } else {
            locationSimulator.setSpeed(speedValue);
        }
        if (locationSpeedProvidingValue != null) {
            locationSimulator.setLocationSpeedProviding(locationSpeedProvidingValue);
        }
        drawRoute(geometry);
        MapKitFactory.getInstance().setLocationManager(locationSimulator);
        locationSimulator.startSimulation(SimulationAccuracy.COARSE);
        listener.onSimulationStarted();
    }

    public void stopSimulation() {
        reset();
        listener.onSimulationStopped();
    }

    public boolean isSimulationActive() {
        return locationSimulator != null;
    }

    private void setGeometry(@NonNull DrivingRoute route) {
        if (!isSimulationActive()) {
            return;
        }
        currentRoute = route;
        Polyline geometry = remainingGeometry(route);
        locationSimulator.setGeometry(geometry);
        drawRoute(geometry);
    }

    public void setLocationSpeedProviding(boolean provide) {
        locationSpeedProvidingValue = provide;
        if (isSimulationActive()) {
            locationSimulator.setLocationSpeedProviding(provide);
        }
    }

    public void addPoint(Location currentLocation, Point point) {
        if (!isSimulationActive()) {
            return;
        }

        PolylinePosition currentPosition = locationSimulator.polylinePosition();

        List<RequestPoint> requestPoints =
                new ArrayList<>(RouteUtils.getRequestPointsAfterPosition(currentPosition, currentRoute));
        requestPoints.add(1, new RequestPoint(
                point,
                RequestPointType.VIAPOINT,
                null /* pointContext */));

        DrivingOptions drivingOptions = new DrivingOptions();
        if (currentLocation != null) {
            drivingOptions.setInitialAzimuth(currentLocation.getHeading());
        }
        drivingOptions.setRoutesCount(1);

        if (session != null) {
            session.cancel();
        }

        session = router.requestRoutes(requestPoints, drivingOptions, new VehicleOptions(), new DrivingSession.DrivingRouteListener() {
            @Override
            public void onDrivingRoutes(@NonNull List<DrivingRoute> routes) {
                if (!routes.isEmpty()) {
                    setGeometry(routes.get(0));
                }
            }

            @Override
            public void onDrivingRoutesError(@NonNull Error error) {}
        });
    }

    public LocationManager simulator() {
        return locationSimulator;
    }

    private void drawRoute(Polyline geometry) {
        if (trajectoryPolyline != null) {
            objects.remove(trajectoryPolyline);
            trajectoryPolyline = null;
        }

        if (geometry == null) {
            return;
        }

        trajectoryPolyline = objects.addPolyline(geometry);
        trajectoryPolyline.setZIndex(10.0f);
        trajectoryPolyline.setStrokeWidth(4.0f);
        trajectoryPolyline.setStrokeColor(Color.GREEN);
        trajectoryPolyline.setDashLength(9.0f);
        trajectoryPolyline.setGapLength(9.0f);
    }

    @Override
    public void onSimulationFinished() {
        reset();
        listener.onSimulationFinished();
    }

    private void reset() {
        if (isSimulationActive()) {
            locationSimulator.unsubscribeFromSimulatorEvents(this);
        }
        locationSimulator = null;
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        currentRoute = null;
        drawRoute(null);
    }
}
