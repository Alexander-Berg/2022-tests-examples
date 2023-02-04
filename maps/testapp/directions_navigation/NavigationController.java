package com.yandex.maps.testapp.directions_navigation;

import android.widget.Toast;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.internal.RouteUtils;
import com.yandex.mapkit.directions.navigation.Navigation;
import com.yandex.mapkit.directions.navigation.Guidance;
import com.yandex.mapkit.directions.navigation_layer.RoutesSource;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.runtime.recording.ReportData;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import androidx.annotation.Nullable;

public class NavigationController {
    private Navigation navigation;
    private Guidance guidance;
    private SimulationController simulationController;
    private RecordedSimulationController recordedSimulationController;

    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    public NavigationController(byte[] serializedNavigation,
                                MapObjectCollection collection,
                                SimulationController.Listener simulationControllerListener,
                                RecordedSimulationController.Listener recordedSimulationControllerListener,
                                Context context) {
        simulationController = new SimulationController(collection, simulationControllerListener);
        if (serializedNavigation == null) {
            navigation = DirectionsFactory.getInstance().createNavigation();
        } else {
            navigation = DirectionsFactory.getInstance().deserializeNavigation(serializedNavigation);
            if (navigation == null) {
                throw new RuntimeException("Failed to deserialize navigation");
            }
        }
        guidance = navigation.getGuidance();
	    recordedSimulationController = new RecordedSimulationController(navigation, guidance, recordedSimulationControllerListener, context);
        navigation.resume();
    }

    public byte[] serializeNavigation() {
        return DirectionsFactory.getInstance().serializeNavigation(navigation);
    }

    public void addRoutePoint(Point point, RoutesSource routesSource) {
        if (routesSource == RoutesSource.GUIDANCE && simulationController.isSimulationActive()) {
            simulationController.addPoint(guidance.getLocation(), point);
            return;
        }

        Location currentLocation = guidance.getLocation();
        if (currentLocation == null) {
            return;
        }

        List<RequestPoint> requestPoints = new ArrayList<>();
        requestPoints.add(new RequestPoint(currentLocation.getPosition(), RequestPointType.WAYPOINT, null));

        DrivingRoute route = null;
        if (routesSource == RoutesSource.GUIDANCE) {
            route = guidance.getCurrentRoute();
        } else {
            if (!navigation.getRoutes().isEmpty()) {
                route = navigation.getRoutes().get(0);
            }
        }
        if (route != null && !route.getMetadata().getFlags().getPredicted()) {
            PolylinePosition currentPosition = route.getPosition();
            if (currentPosition != null) {
                List<RequestPoint> points = RouteUtils.getRequestPointsAfterPosition(currentPosition, route);
                if (!points.isEmpty()) {
                    // The first element is currentPosition, skip it.
                    requestPoints.addAll(points.subList(1, points.size()));
                }
            }
        }

        DrivingOptions drivingOptions = new DrivingOptions();
        if (currentLocation.getHeading() != null) {
            drivingOptions.setInitialAzimuth(currentLocation.getHeading());
        }

        requestPoints.add(new RequestPoint(point, RequestPointType.WAYPOINT, null));
        navigation.requestRoutes(requestPoints, drivingOptions);
    }

    public void startGuidance(@Nullable DrivingRoute route) {
        navigation.startGuidance(route);
    }

    public void stopGuidance() {
        navigation.stopGuidance();
    }

    public void switchToRoute(@Nullable DrivingRoute route) {
        guidance.switchToRoute(route);
    }

    public void resetRoutes() {
        navigation.resetRoutes();
    }

    public void requestParkingRoutes() {
        navigation.requestParkingRoutes();
    }

    public void requestAlternatives() {
        navigation.requestAlternatives();
    }

    public void startSimulation() {
        if (guidance.getCurrentRoute() != null) {
            simulationController.startSimulation(guidance.getCurrentRoute());
        }
    }

    public void stopSimulation() {
        simulationController.stopSimulation();
    }

    public double getSimulationSpeed() {
        return simulationController.getSpeed();
    }

    public void setSimulationSpeed(double speed) {
        simulationController.setSpeed(speed);
    }

    public boolean isSimulationActive() {
        return simulationController.isSimulationActive();
    }

    public void setSimulationLocationSpeedProviding(boolean provide) {
        simulationController.setLocationSpeedProviding(provide);
    }

    public void startRecordedSimulation(ReportData report) {
	    recordedSimulationController.startRecordedSimulation(report);
    }

    public String recordedSimulationTimeLeft() {
        return recordedSimulationController.timeLeft();
    }

    public void stopRecordedSimulation() {
	    recordedSimulationController.stopRecordedSimulation();
    }

    public boolean isRecordedSimulationActive() {
        return recordedSimulationController.isRecordedSimulationActive();
    }

    public int getRecordedSimulationClockRate() {
	    return  recordedSimulationController.getRecordedSimulationClockRate();
    }

    public void setRecordedSimulationClockRate(int clockRate) {
        recordedSimulationController.setRecordedSimulationClockRate(clockRate);
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public SimulationController getSimulationController() {
        return simulationController;
    }

    public List<DrivingRoute> navigationRoutes() {
        return navigation.getRoutes();
    }

    public void suspend() {
        navigation.suspend();
        if (simulationController.isSimulationActive()) {
            simulationController.simulator().suspend();
        }
        if (isRecordedSimulationActive()) {
            recordedSimulationController.suspend();
        }
    }

    public void resume() {
        navigation.resume();
        if (simulationController.isSimulationActive()) {
            assert !isSimulationActive();
            simulationController.simulator().resume();
        }
        if (isRecordedSimulationActive()) {
            assert !isRecordedSimulationActive();
            recordedSimulationController.resume();
        }
    }
}
