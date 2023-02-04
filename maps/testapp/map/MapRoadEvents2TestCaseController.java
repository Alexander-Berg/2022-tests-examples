package com.yandex.maps.testapp.map;

import android.app.Activity;
import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationSimulatorListener;
import com.yandex.mapkit.location.LocationSimulator;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.location.SimulationAccuracy;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.RequestPoint;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.Error;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class MapRoadEventsTestRoute {
    MapRoadEventsTestRoute(
            List<RequestPoint> waypoints,
            long delayAfterFinish)
    {
        this.waypoints = waypoints;
        this.delayAfterFinish = delayAfterFinish;
    }

    public List<RequestPoint> getWaypoints() { return waypoints; }
    public long getDelayAfterFinish() { return delayAfterFinish; }

    private List<RequestPoint> waypoints;
    private long delayAfterFinish;
}

class MapRoadEventsTestCase {
    public MapRoadEventsTestCase(
            List<MapRoadEventsTestRoute> routes,
            float zoom)
    {
        this.routes = routes;
        this.zoom = zoom;
    }

    public List<MapRoadEventsTestRoute> getRoutes() {
        return routes;
    }

    public float getZoom() {
        return zoom;
    }

    private List<MapRoadEventsTestRoute> routes;
    private float zoom;
}

class MapRoadEventsSimpleRouteView {
    public MapRoadEventsSimpleRouteView(MapObjectCollection objectCollection) {
        this.objectCollection = objectCollection;
        this.routePolyline = null;
    }

    public void setRoute(DrivingRoute route) {
        if (routePolyline != null) {
            routePolyline.setGeometry(route.getGeometry());
        } else {
            routePolyline = objectCollection.addPolyline(route.getGeometry());
            routePolyline.setZIndex(0.0f);
            routePolyline.setStrokeWidth(9.0f);
            routePolyline.setStrokeColor(0xc00000ff);
            routePolyline.setDashLength(9.0f);
            routePolyline.setGapLength(9.0f);
        }
    }

    public void resetRoute() {
        if (routePolyline != null) {
            objectCollection.remove(routePolyline);
        }
        routePolyline = null;
    }

    private PolylineMapObject routePolyline;
    private MapObjectCollection objectCollection;
}

public class MapRoadEvents2TestCaseController implements
        LocationSimulatorListener,
        LocationListener
{
    public MapRoadEvents2TestCaseController(MapView mapview, Activity activity)
    {
        this.mapview = mapview;
        this.activity = activity;
        simulationRouteView = new MapRoadEventsSimpleRouteView(
            mapview.getMap().getMapObjects().addCollection());

        currentZoom = mapview.getMap().getCameraPosition().getZoom();
    }

    public void runTestCase(MapRoadEventsTestCase testCase) {
        if (isTestCaseRunning()) {
            stopTestCase();
        }

        this.testCase = testCase;
        currentZoom = testCase.getZoom();
        currentRouteIdx = 0;
        createLocationSimulator();
        testCaseRunning = true;
        runNextRoute();
    }

    public void stopTestCase() {
        if (!isTestCaseRunning()) {
            return;
        }

        locationSimulator.unsubscribe(this);
        locationSimulator.stopSimulation();
        if (delayTimer != null) {
            delayTimer.cancel();
        }
        simulationRouteView.resetRoute();
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        locationSimulator = null;
        drivingSession = null;
        followPoint = false;
        testCaseRunning = false;
        testCase = null;
    }

    public boolean isTestCaseRunning() {
        return testCaseRunning;
    }

    public void setFollowPoint(boolean followPoint) {
        this.followPoint = followPoint;
    }

    @Override
    public void onSimulationFinished() {
        long delay = testCase.getRoutes().get(currentRouteIdx).getDelayAfterFinish();
        delayTimer = new Timer();
        delayTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentRouteIdx += 1;
                        runNextRoute();
                    }
                });
            }
        }, delay);
    }

    // LocationListener
    @Override
    public void onLocationUpdated(Location location) {
        if (followPoint) {
            moveToCurrentPosition(location);
        }
    }

    // LocationListener
    @Override
    public void onLocationStatusUpdated(LocationStatus status) {
    }

    private void runNextRoute() {
        if (currentRouteIdx >= testCase.getRoutes().size()) {
            stopTestCase();
            return;
        }

        DrivingRouter router = DirectionsFactory.getInstance().createDrivingRouter();
        drivingSession = router.requestRoutes(
                testCase.getRoutes().get(currentRouteIdx).getWaypoints(),
                new DrivingOptions(),
                new VehicleOptions(),
                new DrivingSession.DrivingRouteListener()
                {
                    @Override
                    public void onDrivingRoutes(@NonNull List<DrivingRoute> routes) {
                        DrivingRoute route = null;
                        if (!routes.isEmpty()) {
                            route = routes.get(0);
                            if (route != null) {
                                runSimulation(route);
                                return;
                            }
                        }
                        handleResponseError("Cannot find route");
                        onSimulationFinished();
                    }

                    @Override
                    public void onDrivingRoutesError(@NonNull Error error) {
                        handleResponseError(error.getClass().getName());
                        onSimulationFinished();
                    }
                });
    }

    private void createLocationSimulator() {
        locationSimulator = MapKitFactory.getInstance().createLocationSimulator();
        locationSimulator.subscribeForLocationUpdates(
                5.0,
                100,
                10.0,
                /*allowUseInBackground=*/false,
                FilteringMode.ON,
                this);
        locationSimulator.subscribeForSimulatorEvents(this);
    }

    private void moveToCurrentPosition(@NonNull Location currentLocation) {
        CameraPosition position = mapview.getMap().getCameraPosition();
        float azimuth = currentLocation.getHeading() != null ?
                currentLocation.getHeading().floatValue() : position.getAzimuth();

        position = new CameraPosition(
                currentLocation.getPosition(),
                currentZoom,
                azimuth,
                0f);
        Animation animation = new Animation(Animation.Type.SMOOTH, 0.0f);
        mapview.getMap().move(position, animation, null);
    }

    private void runSimulation(DrivingRoute route) {
        simulationRouteView.setRoute(route);
        locationSimulator.setGeometry(route.getGeometry());
        locationSimulator.setSpeed(20);
        locationSimulator.startSimulation(SimulationAccuracy.COARSE);
        MapKitFactory.getInstance().setLocationManager(locationSimulator);
        followPoint = true;
        testCaseRunning = true;
    }

    private void handleResponseError(String message) {
        Utils.showMessage(activity, "Error: " + message);
        locationSimulator.unsubscribe(this);
        locationSimulator = null;
        currentZoom = mapview.getMap().getCameraPosition().getZoom();
    }

    private MapRoadEventsTestCase testCase;
    int currentRouteIdx = 0;
    Timer delayTimer;
    private MapRoadEventsSimpleRouteView simulationRouteView;
    private LocationSimulator locationSimulator;
    private DrivingSession drivingSession;
    private MapView mapview;
    private boolean followPoint = false;
    private boolean testCaseRunning = false;
    private Activity activity;
    private float currentZoom;
}
