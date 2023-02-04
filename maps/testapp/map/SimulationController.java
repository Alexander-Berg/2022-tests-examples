package com.yandex.maps.testapp.map;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.guidance.ClassifiedLocation;
import com.yandex.mapkit.directions.guidance.GuidanceListener;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.directions.guidance.LocalizedSpeaker;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.Subpolyline;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationSimulator;
import com.yandex.mapkit.location.SimulationAccuracy;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.annotations.AnnotationLanguage;

import java.util.ArrayList;
import java.util.List;

public class SimulationController {
    private static final int SIMULATION_SPEED = 40;

    private final Guide guide;

    private boolean active = false;
    private LocationSimulator simulator;
    private PlacemarkMapObject simulationMarker;
    private PolylineMapObject simulationRoutePolyline;
    private GuidanceListener listener = new GuidanceListener() {
            @Override
            public void onLocationUpdated() {
                if (!active || simulationMarker == null) {
                    return;
                }

                ClassifiedLocation location = guide.getLocation();
                if (location == null) {
                    return;
                }

                Location currentLocation = location.getLocation();
                if (currentLocation == null || currentLocation.getPosition() == null) {
                    return;
                }

                simulationMarker.setGeometry(currentLocation.getPosition());
                if (location.getLocation().getHeading() != null) {
                    simulationMarker.setDirection(location.getLocation().getHeading().floatValue());
                }
                simulationMarker.setVisible(true);
            }

            @Override
            public void onRoutePositionUpdated() {
                if (!active) {
                    return;
                }

                PolylinePosition position = guide.getRoutePosition();
                if (position != null && simulationRoutePolyline != null) {
                    simulationRoutePolyline.hide(new Subpolyline(new PolylinePosition(0, 0.0), position));
                }
            }

            @Override
            public void onAnnotationsUpdated() {}

            @Override
            public void onRoadNameUpdated() {}

            @Override
            public void onFinishedRoute() {}

            @Override
            public void onLostRoute() {}

            @Override
            public void onReturnedToRoute() {}

            @Override
            public void onRouteUpdated() {}

            @Override
            public void onSpeedLimitUpdated() {}

            @Override
            public void onSpeedLimitExceededUpdated() {}

            @Override
            public void onSpeedLimitExceeded() {}

            @Override
            public void onLaneSignUpdated() {}

            @Override
            public void onUpcomingEventsUpdated() {}

            @Override
            public void onFasterAlternativeUpdated() {}

            @Override
            public void onParkingRoutesUpdated() {}

            @Override
            public void onManeuverAnnotated() {}

            @Override
            public void onFasterAlternativeAnnotated() {}

            @Override
            public void onLastViaPositionChanged() {}

            @Override
            public void onAlternativesUpdated() {}

            @Override
            public void onAlternativesTimeDifferenceUpdated() {}

            @Override
            public void onStandingStatusUpdated() {}

            @Override

            public void onFreeDriveRouteUpdated() {}

            @Override
            public void onReachedWayPoint() {}

            @Override
            public void onDirectionSignUpdated() {}
        };

    public SimulationController(PlacemarkMapObject simulationMarker) {
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        guide = DirectionsFactory.getInstance().createGuide();
        guide.setReroutingEnabled(false);
        guide.subscribe(listener);
        guide.resume();

        this.simulationMarker = simulationMarker;
    }

    public void onDestroy() {
        guide.unsubscribe(listener);
    }

    public void onPause() {
        guide.suspend();
    }

    public void onResume() {
        guide.resume();
    }

    public Guide getGuide() {
        return guide;
    }

    public void start(DrivingRoute route, PolylineMapObject routePolyline) {
        if (route == null) {
            return;
        }

        active = true;
        simulator = MapKitFactory.getInstance().createLocationSimulator(route.getGeometry());
        simulator.startSimulation(SimulationAccuracy.COARSE);
        simulator.setSpeed(SIMULATION_SPEED);
        MapKitFactory.getInstance().setLocationManager(simulator);
        guide.setRoute(route);

        simulationRoutePolyline = routePolyline;
    }

    public void stop() {
        if (!active) {
            return;
        }

        if (simulationMarker != null) {
            simulationMarker.setVisible(false);
        }

        simulator = null;
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        active = false;
    }
}
