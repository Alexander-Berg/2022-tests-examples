package com.yandex.maps.testapp.guidance;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.internal.RouteUtils;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationSimulatorListener;
import com.yandex.mapkit.location.SimulationAccuracy;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.location.LocationSimulator;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;

public class SimulationController implements LocationSimulatorListener {
    private final int BLUE_COLOR = 0xc00000ff;

    private RouterController routerController;
    private SimpleRouteView simulationRouteView;
    private final MapObjectCollection mapObjects;

    private LocationSimulator simulator;
    private double speedValue = 20;
    private Guide guide;
    private LocationSimulatorListener onFinishedCallback;

    public void setSpeed(double speed) {
        speedValue = speed;
        if (simulator != null) {
            simulator.setSpeed(speed);
        }
    }

    public double getSpeed() {
        return speedValue;
    }

    public boolean isActive() {
        return simulator != null;
    }

    public SimulationController(MapObjectCollection mapObjects, Guide guide) {
        this.mapObjects = mapObjects;
        this.guide = guide;
        routerController = new RouterController();
    }

    public void start(DrivingRoute route, final LocationSimulatorListener onSimulationFinished) {
        assert simulationRouteView == null;
        assert simulator == null;


        SimpleRouteView.PolylineStyle simulatorPolylineStyle = new SimpleRouteView.PolylineStyle(BLUE_COLOR, 9.0f);
        simulatorPolylineStyle.dashLength = 9.0f;
        simulatorPolylineStyle.gapLength = 9.0f;


        simulationRouteView = new SimpleRouteView(mapObjects, simulatorPolylineStyle);
        simulationRouteView.setRoute(route);

        onFinishedCallback = onSimulationFinished;
        simulator = MapKitFactory.getInstance().createLocationSimulator(route.getGeometry());
        simulator.startSimulation(SimulationAccuracy.COARSE);
        simulator.subscribeForSimulatorEvents(this);
        speedValue = simulator.getSpeed();
        MapKitFactory.getInstance().setLocationManager(simulator);
    }

    public void stop() {
        assert simulationRouteView != null;

        simulator.unsubscribeFromSimulatorEvents(this);
        simulator = null;
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        simulationRouteView.setRoute(null);
        simulationRouteView = null;
        routerController.cancelSession();
    }

    public void addRoutePoint(Point point, DrivingOptions drivingOptions) {
        assert isActive();
        assert simulationRouteView != null;

        DrivingRoute route = simulationRouteView.getRoute();
        assert route != null;

        PolylinePosition currentPosition = simulator.polylinePosition();
        assert currentPosition != null;

        List<RequestPoint> requestPointsAfterPosition =
                RouteUtils.getRequestPointsAfterPosition(currentPosition, route);
        ArrayList<RequestPoint> newRequestPoints = new ArrayList<>();
        newRequestPoints.add(requestPointsAfterPosition.get(0));
        newRequestPoints.add(new RequestPoint(
                point,
                RequestPointType.VIAPOINT,
                null /* pointContext */));
        for (int i = 1; i < requestPointsAfterPosition.size(); ++i) {
            newRequestPoints.add(requestPointsAfterPosition.get(i));
        }
        routerController.requestRoute(newRequestPoints, drivingOptions, new VehicleOptions(),
                new RouterController.RequestRouteCallback() {
                    @Override
                    public void onSuccess(DrivingRoute route) {
                        if (route == null) {
                            return;
                        }
                        simulationRouteView.setRoute(route);
                        if (simulator != null && isActive()) {
                            simulator.setGeometry(route.getGeometry());
                            simulator.startSimulation(SimulationAccuracy.COARSE);
                        }
                    }

                    @Override
                    public void onError(Error error) {
                    }
                });
    }

    @Override
    public void onSimulationFinished() {
        onFinishedCallback.onSimulationFinished();
    }
}
