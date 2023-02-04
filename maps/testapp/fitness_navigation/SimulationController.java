package com.yandex.maps.testapp.fitness_navigation;

import android.content.Context;
import android.graphics.Color;
import android.widget.Toast;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationSimulator;
import com.yandex.mapkit.location.SimulationAccuracy;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.PedestrianRouter;
import com.yandex.mapkit.transport.masstransit.BicycleRouterV2;
import com.yandex.mapkit.transport.masstransit.MasstransitRouter;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.mapkit.transport.masstransit.TransitOptions;
import com.yandex.mapkit.transport.masstransit.FilterVehicleTypes;
import com.yandex.mapkit.transport.navigation.Type;
import com.yandex.maps.testapp.common_routing.SpeedConvertor;
import com.yandex.runtime.Error;

import androidx.annotation.NonNull;

import java.util.List;

public class SimulationController {
    private static final PedestrianRouter pedestrianRouter = TransportFactory.getInstance().createPedestrianRouter();
    private static final BicycleRouterV2 bicycleRouter = TransportFactory.getInstance().createBicycleRouterV2();
    private static final BicycleRouterV2 scooterRouter = TransportFactory.getInstance().createScooterRouter();
    private static final MasstransitRouter masstransitRouter = TransportFactory.getInstance().createMasstransitRouter();

    private Session session;
    private final Context context;
    private double simulationSpeed = 2.0;

    private LocationSimulator locationSimulator;
    final private MapObjectCollection objects;

    private PolylineMapObject trajectoryPolyline;

    public SimulationController(Context context, MapObjectCollection objects) {
        this.context = context;
        this.objects = objects;
    }

    public void startSimulation(@NonNull Polyline route) {
        if (locationSimulator != null)
            return;

        locationSimulator = MapKitFactory.getInstance().createLocationSimulator(route);
        locationSimulator.setSpeed(simulationSpeed);
        locationSimulator.startSimulation(SimulationAccuracy.FINE);
        MapKitFactory.getInstance().setLocationManager(locationSimulator);
        drawRoute(route);
    }

    public void setSpeed(int value) {
        simulationSpeed = SpeedConvertor.toMs(value);
        if (locationSimulator != null)
            locationSimulator.setSpeed(simulationSpeed);
    }

    public LocationManager getLocationManager() {
        return locationSimulator;
    }

    public int getSpeed() {
        return (int)(SpeedConvertor.toKmh(simulationSpeed));
    }

    public void startSimulation(@NonNull List<RequestPoint> requestPoints, Type type) {
        if (session != null)
            session.cancel();

        Session.RouteListener routeListener = new Session.RouteListener() {
            @Override
            public void onMasstransitRoutes(@NonNull List<Route> routes) {
                if (!routes.isEmpty()) {
                    stopSimulation();
                    startSimulation(routes.get(0).getGeometry());
                }
            }

            @Override
            public void onMasstransitRoutesError(@NonNull Error error) {
                Toast.makeText(context, "onMasstransitRoutesError: " + error.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        switch (type) {
            case BICYCLE:
                session = bicycleRouter.requestRoutes(requestPoints, new TimeOptions(), routeListener);
                break;
            case SCOOTER:
                session = scooterRouter.requestRoutes(requestPoints, new TimeOptions(), routeListener);
                break;
            case PEDESTRIAN:
                session = pedestrianRouter.requestRoutes(requestPoints, new TimeOptions(), routeListener);
                break;
            case MASSTRANSIT:
                session = masstransitRouter.requestRoutes(requestPoints,
                        new TransitOptions(FilterVehicleTypes.NONE.value, new TimeOptions()),
                        routeListener);
                break;
        }
    }

    public void stopSimulation() {
        if (locationSimulator != null)
            locationSimulator.stopSimulation();
        locationSimulator = null;
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        eraseRoute();
    }

    private void eraseRoute() {
        if (trajectoryPolyline != null) {
            objects.remove(trajectoryPolyline);
            trajectoryPolyline = null;
        }
    }

    private void drawRoute(@NonNull Polyline route) {
        eraseRoute();

        trajectoryPolyline = objects.addPolyline(route);
        trajectoryPolyline.setZIndex(-10.0f);
        trajectoryPolyline.setStrokeWidth(4.0f);
        trajectoryPolyline.setStrokeColor(Color.GREEN);
        trajectoryPolyline.setDashLength(9.0f);
        trajectoryPolyline.setGapLength(9.0f);
    }
}
