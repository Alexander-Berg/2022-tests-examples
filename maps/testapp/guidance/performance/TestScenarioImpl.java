package com.yandex.maps.testapp.guidance.performance;

import android.content.Context;
import androidx.annotation.NonNull;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.internal.RouteUtils;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.guidance.ClassifiedLocation;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.directions.guidance.PerformanceMonitor;
import com.yandex.mapkit.directions.guidance.ViewArea;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationSimulatorListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.maps.testapp.driving.RouteView;
import com.yandex.maps.testapp.guidance.GuidanceUtils;
import com.yandex.maps.testapp.guidance.RoutePointsController;
import com.yandex.maps.testapp.guidance.SimulationController;
import com.yandex.maps.testapp.guidance.test.RouteSelector;
import com.yandex.maps.testapp.guidance.test.TestCase;
import com.yandex.maps.testapp.guidance.test.TestCaseData;
import com.yandex.maps.testapp.guidance.test.TestCaseFactory;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.recording.EventListener;
import com.yandex.runtime.recording.EventLoggingFactory;

import java.io.IOException;
import java.lang.StringBuilder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

class TestScenarioImpl implements
        TestScenario,
        EventListener,
        LocationSimulatorListener,
        TestCaseFactory.TestCaseFactoryCallback,
        GuidanceListenerImpl.LocationUpdateListener {

    private static final AnnotationLanguage language = AnnotationLanguage.RUSSIAN;
    private static final float ZOOM_VIEW_OUT = 0.2f;
    private static final float ALTERNATIVE_Z_INDEX = -2.0f;
    private static final long MAX_MILLIS_SINCE_ROUTE_LOST = 5000;

    private final Guide guide = DirectionsFactory.getInstance().createGuide();
    private final Context ctx;
    private final MapView mapView;
    private final SimulationController simulationController;
    private final RoutePointsController routePointsController;
    private final RouteView routeView;
    private final TestScenarioListener listener;

    private ArrayList<RouteView> alternativesView = new ArrayList<>();

    private ArrayList<PerformanceMonitor> performanceMonitors;
    private GuidanceListenerImpl locationUpdateListener = new GuidanceListenerImpl(this);

    private Long lostRouteAt;
    private boolean finishedRoute = false;

    TestScenarioImpl(final Context ctx, MapView mapView, TestScenarioListener listener) {
        this.ctx = ctx;
        this.mapView = mapView;
        this.listener = listener;

        guide.setAlternativesEnabled(true);
        guide.setReroutingEnabled(true);

        MapObjectCollection mapObjects = mapView.getMap().getMapObjects().addCollection();
        routePointsController = new RoutePointsController(mapObjects, guide);
        simulationController = new SimulationController(mapObjects, guide);

        routeView = new RouteView(mapView.getMap(), null, new RouteView.ImageProviderFactory() {
            @Override
            public ImageProvider fromResource(int resourceId) {
                return ImageProvider.fromResource(ctx.getApplicationContext(), resourceId);
            }
        });

        routeView.setJamsEnabled(true);
        routeView.setEventsEnabled(false);
        routeView.setManeuversEnabled(false);
        routeView.setSelectedArrivalPointsEnabled(true);

        simulationController.setSpeed(60.0);

        performanceMonitors = new ArrayList<PerformanceMonitor>() {{
            add(guide.createPerformanceMonitor(
                    PerformanceMonitor.MetricTag.EMIT_FRAME_DURATION));
            add(guide.createPerformanceMonitor(
                    PerformanceMonitor.MetricTag.LOCATION_PROCESSING_TIME));
        }};

        for (PerformanceMonitor monitor : performanceMonitors) {
            monitor.start();
        }
    }

    @Override
    public void execute() {
        try {
            TestCaseFactory factory = new TestCaseFactory(ctx, language);
            factory.createTestCase(getTestCaseData(), this);
        } catch (IOException e) {
            StringBuilder builder = new StringBuilder(200);
            listener.onError(
                    builder.append("Error while creating test case! ").append(e.getMessage()).toString(),
                    Level.SEVERE);
        }
    }

    @Override
    public void stop() {
        routeView.setRoute(null);
        for(int i = 0; i < alternativesView.size(); ++i) {
            alternativesView.get(i).setRoute(null);
        }
        alternativesView.clear();

        EventLoggingFactory.getEventLogging().unsubscribe(this);

        routePointsController.clear();
        if (simulationController.isActive()) {
            simulationController.stop();
        }

        guide.unsubscribe(locationUpdateListener);
        guide.resetRoute();
    }

    @Override
    public void onTestCaseCreated(TestCase testCase) {
        if (testCase == null) {
            listener.onError("Filed to create TestCase!", Level.SEVERE);
            return;
        }

        guide.suspend();

        guide.setParkingRoutesEnabled(testCase.useParkingRoutes);
        guide.setAlternativesEnabled(!testCase.disableAlternatives);

        if (testCase.route != null) {
            EventLoggingFactory.getEventLogging().subscribe(this);
            routePointsController.setPoints(testCase.routePoints);
            guide.subscribe(locationUpdateListener);
            guide.setRoute(testCase.route);
            routeView.setRoute(testCase.route);

            simulationController.start(testCase.simulationRoute, this);
        } else {
            listener.onError("Scenario contains no root!", Level.SEVERE);
        }

        guide.resume();
    }

    @Override
    public void onLocationUpdated() {
        ClassifiedLocation location = guide.getLocation();
        if (location == null) {
            listener.onError("Current location is unknown!", Level.WARNING);
            return;
        }

        setPosition(getNewCameraPosition(
                location.getLocation(),
                guide.getViewArea(),
                mapView.getMap().getCameraPosition()));
    }

    @Override
    public void onAlternativesUpdated() {
        for (RouteView alternativeView : alternativesView) {
            alternativeView.setRoute(null);
        }
        alternativesView = new ArrayList<>();

        List<DrivingRoute> alternatives = guide.getAlternatives();
        for (int i = 0; i < alternatives.size(); ++i) {
            DrivingRoute alternative = alternatives.get(i);
            RouteView view = new RouteView(mapView.getMap(), null, null);
            view.setJamsEnabled(false);
            view.setEventsEnabled(false);
            view.setManeuversEnabled(false);
            view.setZIndex(ALTERNATIVE_Z_INDEX - (float)i);
            view.setRoute(alternative);
            alternativesView.add(view);
        }
    }

    @Override
    public void onRoutePositionUpdated() {
        PolylinePosition routePosition = guide.getRoutePosition();
        if (routePosition != null && routeView != null) {
            routeView.setPosition(routePosition);
        }

        List<DrivingRoute> alternatives = guide.getAlternatives();
        for (int i = 0; i < alternatives.size(); ++i) {
            alternativesView.get(i).setPosition(alternatives.get(i).getPosition());
        }
    }

    private void checkTimeSinceRouteLost() {
        if (lostRouteAt == null) {
            return;
        }

        long timeDifference = Calendar.getInstance().getTimeInMillis() - lostRouteAt;
        if (timeDifference > MAX_MILLIS_SINCE_ROUTE_LOST) {
            listener.onError(new StringBuilder()
                    .append(timeDifference)
                    .append(" ms passed since route lost, which is more than allowed ")
                    .append(MAX_MILLIS_SINCE_ROUTE_LOST)
                    .append(" ms").toString(), Level.SEVERE);
        }
    }

    @Override
    public void onRouteUpdated() {
        checkTimeSinceRouteLost();
        lostRouteAt = null;
        routeView.setRoute(guide.getRoute());
    }

    @Override
    public void onReturnedToRoute() {
        checkTimeSinceRouteLost();
        lostRouteAt = null;
    }

    @Override
    public void onFinishedRoute() {
        finishedRoute = true;
    }

    @Override
    public void onLostRoute() {
        if (lostRouteAt == null) {
            lostRouteAt = Calendar.getInstance().getTimeInMillis();
        } else {
            listener.onError("Route lost twice in a row", Level.SEVERE);
        }
    }

    @Override
    public void onSimulationFinished() {
        if (finishedRoute) {
            stop();
            for (PerformanceMonitor monitor : performanceMonitors) {
                monitor.stop();
            }
            listener.onScenarioFinished(performanceMonitors);
        } else {
            listener.onError("Didn't recieve route finished", Level.SEVERE);
        }
    }

    @Override
    public void onEvent(@NonNull String event, @NonNull Map<String, String> data) {
    }

    static private class BestRouteWithNoViaPoints implements RouteSelector {
        @Override
        public DrivingRoute selectRoute(List<DrivingRoute> routes) {
            for (DrivingRoute route : routes) {
                return RouteUtils.dropRouteViaPoints(route);
                //return route;
            }
            return null;
        }
    }

    private void setPosition(CameraPosition position) {
        mapView.getMap().move(
                position,
                new Animation(Animation.Type.SMOOTH, 0.0f),
                null);
    }

    private CameraPosition getNewCameraPosition(
            Location currentLocation,
            ViewArea viewArea,
            CameraPosition oldPosition) {
        final float azimuth = currentLocation.getHeading() != null ?
                currentLocation.getHeading().floatValue() : oldPosition.getAzimuth();
        final Point center = GuidanceUtils.calculateViewCenter(
                viewArea, azimuth, currentLocation.getPosition());
        final float zoom = mapView.getMap().cameraPosition(
                GuidanceUtils.calculateViewBoundingBox(viewArea, center)).getZoom() - ZOOM_VIEW_OUT;
        return new CameraPosition(center, zoom, azimuth, oldPosition.getTilt());
    }

    private static TestCaseData getTestCaseData() {
        return new TestCaseData(
                "Performance test scenario",
                "Tests route guidance with alternatives",
                new Point[]{
                        new Point(53.195296, 50.092688),
                        new Point(53.196757, 50.096457),
                        new Point(53.189439, 50.094093),
                        new Point(53.193568, 50.099756),
                        new Point(53.191433, 50.097326),
                },
                new Point[]{
                        new Point(53.195238, 50.092677),
                        new Point(53.193390, 50.095233),
                        new Point(53.191433, 50.097326),
                },
                new VehicleOptions(),
                new BestRouteWithNoViaPoints()
        );
    }
}
