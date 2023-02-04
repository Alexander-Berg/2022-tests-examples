package com.yandex.maps.testapp.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.RouteHelper;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Cluster;
import com.yandex.mapkit.map.ClusterListener;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapLoadStatistics;
import com.yandex.mapkit.map.MapLoadedListener;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.ClusterizedPlacemarkCollection;
import com.yandex.mapkit.map.MapWindow;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.road_events_layer.RoadEventsLayer;
import com.yandex.mapkit.styling.roadevents.RoadEventsLayerDefaultStyleProvider;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.driving.Router;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MapPerformanceActivity extends MapBaseActivity
        implements PerformanceTestScenario.Callback, MapLoadedListener {

    private com.yandex.mapkit.road_events_layer.StyleProvider roadEventsLayerStyleProvider;
    private RoadEventsLayer roadEventsLayer;

    @Override
    public void onScenarioFinished(final String scenarioName, final String renderResult, final String decodersResult) {
        showPerformanceResult(scenarioName, renderResult, decodersResult);
    }

    public static void logResult(final String tag, final String scenarioName, final String msg) {
        Log.w(tag, "#" + scenarioName);
        String[] strings = msg.split("\n");
        for (String str : strings) {
            Log.w(tag, str);
        }
        Log.w(tag, "##########");
    }

    private void showPerformanceResult(final String scenarioName, final String renderResult, final String decoderResult) {
        logResult("RENDER_PERFORMANCE:", scenarioName, renderResult);
        logResult("DECODER_PERFORMANCE:", scenarioName, decoderResult);
        showMessage(renderResult, scenarioName);
    }

    private static final float ZOOM_AND_SCROLL_TIME = 5.f;

    private static final PerformanceTestStep[] performanceTestSteps = new PerformanceTestStep[] {
            new PerformanceTestStep(new CameraPosition(new Point(59.918421, 30.289192), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.918421, 30.289192), 10.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.918421, 30.289192), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.909838, 30.289564), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.909838, 30.289564), 10.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.909838, 30.289564), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.889838, 30.293640), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.848345, 30.291913), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.878345, 30.306913), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.909363, 30.319510), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(59.925782, 30.318223), 15.5f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME)
    };

    private static PerformanceTestStep[] createTerrainSteps(float zoom, float tilt, float stepDuration, int repeatCount) {
        PerformanceTestStep[] steps = new PerformanceTestStep[] {
                new PerformanceTestStep(new CameraPosition(new Point(42.901983, 47.456097), zoom, 0.0f, tilt), stepDuration),
                new PerformanceTestStep(new CameraPosition(new Point(40.157494, 45.702723), zoom, 0.0f, tilt), stepDuration),
                new PerformanceTestStep(new CameraPosition(new Point(43.551083, 41.403403), zoom, 0.0f, tilt), stepDuration),
                new PerformanceTestStep(new CameraPosition(new Point(44.170283, 45.157868), zoom, 0.0f, tilt), stepDuration),
                new PerformanceTestStep(new CameraPosition(new Point(44.095538, 46.766488), zoom, 0.0f, tilt), stepDuration),
        };

        PerformanceTestStep[] result = new PerformanceTestStep[steps.length * repeatCount];

        for (int i = 0; i < repeatCount; i++) {
            for (int j = 0; j < steps.length; j++) {
                result[i * steps.length + j] = steps[j];
            }
        }

        return result;
    }

    private static final PerformanceTestStep[] roadEventsTestSteps = new PerformanceTestStep[] {
            new PerformanceTestStep(new CameraPosition(new Point(55.246921, 36.940149), 9.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(56.220100, 36.940149), 9.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.708769, 37.622579), 9.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.708769, 37.622579), 8.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.708769, 37.622579), 19.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.708769, 37.622579), 13.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.700937, 37.622579), 13.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.700937, 37.315745), 13.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.705663, 37.883023), 13.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.705663, 37.883023), 7.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.705663, 37.883023), 19.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME),
            new PerformanceTestStep(new CameraPosition(new Point(55.705663, 37.883023), 8.0f, 0.0f, 60.0f), ZOOM_AND_SCROLL_TIME)
    };

    private static final PerformanceTestStep[] roadEventsZoomTestSteps = new PerformanceTestStep[] {
            new PerformanceTestStep(new CameraPosition(new Point(59.959597, 30.404075), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.959597, 30.404075), 0.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.959597, 30.404075), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.904087, 30.421707), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.904087, 30.421707), 0.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.904087, 30.421707), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.867327, 30.413701), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.867327, 30.413701), 0.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.867327, 30.413701), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.904087, 30.421707), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.904087, 30.421707), 0.0f, 0.0f, 60.0f), 0.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.904087, 30.421707), 17.0f, 0.0f, 60.0f), 0.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.867327, 30.413701), 17.0f, 0.0f, 60.0f), 1.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.867327, 30.413701), 0.0f, 0.0f, 60.0f), 0.0f),
            new PerformanceTestStep(new CameraPosition(new Point(59.867327, 30.413701), 17.0f, 0.0f, 60.0f), 0.0f)
    };

    private static PerformanceTestStep[] createZoomTestSteps(
            Point firstPoint, float firstZoom, Point secondPoint, float secondZoom, float tilt, float durationInSeconds, float loopTime) {
        final int LOOP_COUNT = Math.round(durationInSeconds / loopTime);
        PerformanceTestStep[] steps = new PerformanceTestStep[LOOP_COUNT * 2];
        for (int i = 0; i < LOOP_COUNT * 2; i += 2) {
            steps[i] = new PerformanceTestStep(new CameraPosition(firstPoint, firstZoom, 0.0f, tilt), 0.5f * loopTime);
            steps[i + 1] = new PerformanceTestStep(new CameraPosition(secondPoint, secondZoom, 0.0f, tilt), 0.5f * loopTime);
        }
        return steps;
    }

    static public PerformanceTestStep[] createScrollTestSteps(float zoom, Point center, float radius) {
        return createScrollTestSteps(zoom, center, radius, 120.f, 10.f);
    }

    static public PerformanceTestStep[] createScrollTestSteps(float zoom, Point center, float radius, float durationInSeconds, float loopTime) {
        final float ANGLE_STEP = 9.0f;
        final int LOOP_SIZE = Math.round(360.0f / ANGLE_STEP);
        final float STEP_TIME = loopTime / LOOP_SIZE;
        final int LOOP_COUNT = Math.round(durationInSeconds / loopTime);
        final double ANGLE_STEP_RADIANS = Math.toRadians(ANGLE_STEP);

        double longitudeRadius = radius * Math.pow(2, 8 - zoom);
        double latitudeRadius = 0.5 * longitudeRadius;

        PerformanceTestStep[] steps = new PerformanceTestStep[1 + LOOP_COUNT * LOOP_SIZE];
        steps[0] = new PerformanceTestStep(
                new CameraPosition(new Point(center.getLatitude(), center.getLongitude() + longitudeRadius), zoom, 0.0f, 0.0f), 0.f);

        for (int loop = 0; loop < LOOP_COUNT; ++loop) {
            double angle = 0.0;
            for (int step = 0; step < LOOP_SIZE; ++step, angle += ANGLE_STEP_RADIANS) {
                Point point = new Point(
                        center.getLatitude() + latitudeRadius * Math.sin(angle),
                        center.getLongitude() + longitudeRadius * Math.cos(angle));
                steps[loop * LOOP_SIZE + step + 1] = new PerformanceTestStep(
                        new CameraPosition(point, zoom, 0.f, 0.f), STEP_TIME);
            }
        }
        return steps;
    }

    private static final float DEFAULT_ROUTE_BASED_SCENARIO_DURATION = 300;
    private static final Point MOSCOW_COORDINATES = new Point(55.7504, 37.6728);
    private static final Point SPB_COORDINATES = new Point(59.945933, 30.320045);

    public static PerformanceTestScenario createMskTilesDecoderTest(
            MapWindow mapWindow, PerformanceTestScenario.Callback callback, int durationInSeconds) {
        return createTilesDecoderTest(
                mapWindow, callback, durationInSeconds, MOSCOW_COORDINATES, "MSK ");
    }

    private static PerformanceTestScenario createSpbTilesDecoderTest(
            MapWindow mapWindow, PerformanceTestScenario.Callback callback, int durationInSeconds) {
        return createTilesDecoderTest(
                mapWindow, callback, durationInSeconds, SPB_COORDINATES, "SPB ");
    }

    public static PerformanceTestScenario createTilesDecoderTest(
            MapWindow mapWindow, PerformanceTestScenario.Callback callback, int durationInSeconds, Point center, String testNamePrefix) {
        return new PerformanceTestScenario(
                mapWindow,
                callback,
                createScrollTestSteps(14.5f, center, 5, durationInSeconds, 10.f),
                testNamePrefix + "Tiles decoder(" + Integer.toString(durationInSeconds) + "sec)");
    }

    public static PerformanceTestScenario createDefaultSpbCenterTest(
            Context context, MapWindow mapWindow, PerformanceTestScenario.Callback callback) {
        return new RouteBasedPerformanceTestScenario(
                context, mapWindow, callback, R.raw.route_spb_center,
                RouteBasedPerformanceTestScenario.RouteStyle.Hidden, 500.f,
                16.5f, 0.f, 60.f, "SPB Center 16.5", DEFAULT_ROUTE_BASED_SCENARIO_DURATION);
    }

    List<PerformanceTestScenario> createScenarios() {
        List<PerformanceTestScenario> scenarios = new ArrayList<>();

        scenarios.add(new PerformanceTestScenario(
                mapview.getMapWindow(), this, createScrollTestSteps(3.f, MOSCOW_COORDINATES, 1), "Scroll Zoom 3"));
        scenarios.add(new PerformanceTestScenario(
                mapview.getMapWindow(), this, createScrollTestSteps(8.5f, MOSCOW_COORDINATES, 1), "Scroll Zoom 8.5 + Jams") {
            boolean wasTrafficVisible = false;
            @Override
            protected void preExecute(Runnable onComplete) {
                wasTrafficVisible = jamsSwitchBt.isChecked();
                jamsSwitchBt.setChecked(true);
                super.preExecute(onComplete);
            }

            @Override
            protected void postExecute() {
                super.postExecute();
                jamsSwitchBt.setChecked(wasTrafficVisible);
            }
        });
        scenarios.add(new PerformanceTestScenario(
                mapview.getMapWindow(), this, createScrollTestSteps(16.f, MOSCOW_COORDINATES, 1), "Scroll Zoom 16"));

        //uncomment for testing of terrain
        //created for https://st.yandex-team.ru/MAPSMOBCORE-12731
        //scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, createTerrainSteps(5.51f, 30.f, 0.5f, 80), "Terrain 5.5"));
        //scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, createTerrainSteps(7.51f, 30.f, 2.f, 20), "Terrain 7.5"));

        scenarios.add(new PerformanceTestScenario(
            mapview.getMapWindow(), this,
            createZoomTestSteps(new Point(59.918421, 30.306913), 15.5f, new Point(59.918421, 30.289192), 7.0f, 60.0f, 200.0f, 1.0f),
            "Long zoom"));
        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, performanceTestSteps, "Zoom/Scroll"));
        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, performanceTestSteps, "Zoom/Scroll + ClearCache") {
            @Override
            protected void onNextStep() {
                modelSwitchBt.setChecked(!modelSwitchBt.isChecked());
            }
        });

        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, roadEventsTestSteps, "Road Events Baseline"));

        scenarios.add(new RoadEventsPerformanceTestScenario(
                traffic,
                mapview.getMapWindow(),
                this,
                roadEventsTestSteps,
                "Road Events"));

        scenarios.add(new RoadEvents2PerformanceTestScenario(
                roadEventsLayer,
                this,
                mapview.getMapWindow(),
                this,
                roadEventsTestSteps,
                "Road Events 2",
                0,
                false));

        scenarios.add(new RoadEvents2PerformanceTestScenario(
                roadEventsLayer,
                this,
                mapview.getMapWindow(),
                this,
                roadEventsTestSteps,
                "Road Events 2 with route events (native)",
                1000,
                true));

        scenarios.add(new RoadEvents2PerformanceTestScenario(
                roadEventsLayer,
                this,
                mapview.getMapWindow(),
                this,
                roadEventsTestSteps,
                "Road Events 2 with route events (platform)",
                300,
                false));

        scenarios.add(new RoadEvents2PerformanceTestScenario(
                roadEventsLayer,
                this,
                mapview.getMapWindow(),
                this,
                roadEventsZoomTestSteps,
                "Road Events 2: Zoom 0 - 17",
                0,
                false));

        scenarios.add(new PerformanceTestScenario(
                mapview.getMapWindow(),
                this,
                new PerformanceTestStep[]{
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.959597, 30.404075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.809597, 30.404075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.809597, 30.654075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.959597, 30.654075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.959597, 30.404075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.809597, 30.404075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.809597, 30.654075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.959597, 30.654075), 15, 0, 0), 10.f),
                    new PerformanceTestStep(
                        new CameraPosition(new Point(59.959597, 30.404075), 15, 0, 0), 10.f),
                    },
          "Fast scroll"));

        scenarios.add(new HeatmapPerformanceTestScenario(
                false,
                this,
                mapview.getMapWindow(),
                this,
          "Heatmap Zoom/Scroll (without layer)"));

        scenarios.add(new HeatmapPerformanceTestScenario(
                true,
                this,
                mapview.getMapWindow(),
                this,
          "Heatmap Zoom/Scroll (with layer)"));

        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, performanceTestSteps, "Zoom/Scroll + Route") {
            @Override
            protected void preExecute(final Runnable onComplete) {
                routePolyline = mapview.getMap().getMapObjects().addPolyline();
                Router router = new Router();
                for(PerformanceTestStep step : steps) {
                    router.addWaypoint(step.position.getTarget());
                }
                final DrivingSession.DrivingRouteListener listener = new DrivingSession.DrivingRouteListener() {
                    @Override
                    public void onDrivingRoutes(List<DrivingRoute> routes) {
                        if (routes.size() > 0) {
                            RouteHelper.updatePolyline(routePolyline, routes.get(0), RouteHelper.createDefaultJamStyle(), true);
                        }
                        onComplete.run();
                    }

                    @Override
                    public void onDrivingRoutesError(Error error) {
                        onComplete.run();
                    }
                };
                router.requestRoute(new DrivingOptions(), new VehicleOptions(), listener);
            }

            @Override
            protected void postExecute() {
                mapview.getMap().getMapObjects().remove(routePolyline);
            }

            private PolylineMapObject routePolyline;
        });

        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this, performanceTestSteps, "Clustered placemarks") {
            private final int placemarksCount = 10000;
            private ImageProvider imageProvider = ImageProvider.fromResource(
                MapPerformanceActivity.this, R.drawable.a0);

            class ClusterListenerImpl implements ClusterListener {
                private boolean started = false;
                private long timeBegan = 0;

                public void start(long timeBegan) {
                    this.timeBegan = timeBegan;
                }

                @Override
                public void onClusterAdded(Cluster cluster) {
                    if (!started) {
                        long timeFinished = System.currentTimeMillis();
                        Log.w(placemarksCount + " PLACEMARKS CLUSTERIZED ALL",
                            "Elapsed time: " + (timeFinished - timeBegan));
                        started = true;
                    }

                    cluster.getAppearance().setIcon(imageProvider);
                }
            };
            private ClusterListenerImpl clusterListener;

            @Override
            protected void preExecute(final Runnable onComplete) {
                final double latitude = 59.918421;
                final double longitude = 30.289192;
                Random rand = new Random();
                List<Point> points = new ArrayList<Point>();
                for (int i = 0; i < placemarksCount; ++i) {
                    points.add(new Point(
                        latitude + (rand.nextFloat() - 0.5),
                        longitude + (rand.nextFloat() - 0.5)));
                }

                clusterListener = new ClusterListenerImpl();
                ClusterizedPlacemarkCollection mapObjects =
                    mapview.getMap().getMapObjects().addClusterizedPlacemarkCollection(
                        clusterListener);
                mapObjects.addPlacemarks(points, imageProvider, new IconStyle());

                long timeBegan = System.currentTimeMillis();
                clusterListener.start(timeBegan);
                mapObjects.clusterPlacemarks(60, 15);

                long timeFinished = System.currentTimeMillis();
                Log.w(placemarksCount + " PLACEMARKS CLUSTERIZED UI",
                    "Elapsed time: " + (timeFinished - timeBegan));
                onComplete.run();
            }

            @Override
            protected void postExecute() {
                mapview.getMap().getMapObjects().clear();
            }
        });

        scenarios.add(createDefaultSpbCenterTest(this, mapview.getMapWindow(), this));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_center,
                RouteBasedPerformanceTestScenario.RouteStyle.Hidden, 500.f,
                16.5f, 0.f, 60.f, "SPB Center 16.5(30sec)", 30));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_center,
                RouteBasedPerformanceTestScenario.RouteStyle.Hidden, 2000.f,
                14.5f, 0.f, 60.f, "SPB Center 14.5", DEFAULT_ROUTE_BASED_SCENARIO_DURATION));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_center,
                RouteBasedPerformanceTestScenario.RouteStyle.Hidden, 2000.f,
                14.5f, 0.f, 60.f, "SPB Center 14.5(30sec)", 30));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_center,
                RouteBasedPerformanceTestScenario.RouteStyle.Normal, 50.f,
                15.5f, 0.f, 60.f, "SPB Center + Route", DEFAULT_ROUTE_BASED_SCENARIO_DURATION));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_center,
                RouteBasedPerformanceTestScenario.RouteStyle.Navi, 50.f,
                15.5f, 0.f, 60.f, "SPB Center + Route + Navi Style", DEFAULT_ROUTE_BASED_SCENARIO_DURATION));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_ring_road,
                RouteBasedPerformanceTestScenario.RouteStyle.Hidden, 80.f,
                15.5f, 0.f, 60.f, "SPB Ring Road", DEFAULT_ROUTE_BASED_SCENARIO_DURATION));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_ring_road,
                RouteBasedPerformanceTestScenario.RouteStyle.Normal, 80.f,
                15.5f, 0.f, 60.f, "SPB Ring Road + Route", DEFAULT_ROUTE_BASED_SCENARIO_DURATION));

        scenarios.add(new RouteBasedPerformanceTestScenario(
                this, mapview.getMapWindow(), this, R.raw.route_spb_ring_road,
                RouteBasedPerformanceTestScenario.RouteStyle.Navi, 80.f,
                15.5f, 0.f, 60.f, "SPB Ring Road + Route + Navi Style", DEFAULT_ROUTE_BASED_SCENARIO_DURATION));

        // Scenarios to add many placemarks on the map.
        final int nPlacemarks = 2000;

        final double latitude = 59.918421;
        final double longitude = 30.289192;
        final float stepTime = 5.f;
        PerformanceTestStep step =
                new PerformanceTestStep(new CameraPosition(
                        new Point(latitude, longitude), 15.f, 0.0f, 60.0f), stepTime);
        final int nSteps = 10;
        final PerformanceTestStep[] addingPlacemarksPerformanceTestSteps = new PerformanceTestStep[nSteps];
        for (int i = 0; i < nSteps; i++) {
            addingPlacemarksPerformanceTestSteps[i] = step;
        }

        final double[] latitudes = new double[nPlacemarks];
        final double[] longitudes = new double[nPlacemarks];
        Random rand = new Random();
        for (int i = 0; i < nPlacemarks; ++i) {
            latitudes[i] = latitude + (rand.nextFloat() - 0.5) / 100.;
            longitudes[i] = longitude + (rand.nextFloat() - 0.5) / 100.;
        }

        final MapObjectCollection mapObjects = mapview.getMap().getMapObjects();

        String scenarioName = "add-" + nPlacemarks + "-empty-placemarks";
        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this,
                addingPlacemarksPerformanceTestSteps, scenarioName) {
            @Override
            protected void onNextStep() {

                long timeBegan = System.currentTimeMillis();

                for (int i = 0; i < nPlacemarks; ++i) {
                    mapObjects.addEmptyPlacemark(new Point(latitudes[i], longitudes[i]));
                }

                long timeFinished = System.currentTimeMillis();
                Log.w(nPlacemarks + " E-PLACEMARKS ADDED", "Elapsed time: " + (timeFinished - timeBegan));

                mapObjects.clear();
            }
        });

        scenarioName = "add-" + nPlacemarks + "-empty-placemarks-pkg";
        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this,
                addingPlacemarksPerformanceTestSteps, scenarioName) {
            @Override
            protected void onNextStep() {

                long timeBegan = System.currentTimeMillis();

                List<Point> points = new ArrayList<>(nPlacemarks);
                for (int i = 0; i < nPlacemarks; ++i) {
                    points.add(new Point(latitudes[i], longitudes[i]));
                }
                List<PlacemarkMapObject> placemarks = mapObjects.addEmptyPlacemarks(points);

                long timeFinished = System.currentTimeMillis();
                Log.w(nPlacemarks + " E-PLACEMARKS ADDED", "Elapsed time: " + (timeFinished - timeBegan));

                mapObjects.clear();
            }
        });

        final ImageProvider rectangleImage =
                ImageProvider.fromBitmap(createRectangleBitmap(Color.BLUE, Color.RED, 15, 15));
        final IconStyle iconStyle = new IconStyle();

        scenarioName = "add-" + nPlacemarks + "-placemarks";
        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this,
                addingPlacemarksPerformanceTestSteps, scenarioName) {

            @Override
            protected void onNextStep() {
                long timeBegan = System.currentTimeMillis();
                for (int i = 0; i < nPlacemarks; ++i) {
                    PlacemarkMapObject placemark = mapObjects.addPlacemark(
                            new Point(latitudes[i], longitudes[i]), rectangleImage, iconStyle);
                }
                long timeFinished = System.currentTimeMillis();
                Log.w(nPlacemarks + " I-PLACEMARKS ADDED", "Elapsed time: " + (timeFinished - timeBegan));

                mapObjects.clear();
            }
        });

        scenarioName = "add-" + nPlacemarks + "-placemarks-pkg";
        scenarios.add(new PerformanceTestScenario(mapview.getMapWindow(), this,
                addingPlacemarksPerformanceTestSteps, scenarioName) {

            @Override
            protected void onNextStep() {
                long timeBegan = System.currentTimeMillis();

                List<Point> points = new ArrayList<>(nPlacemarks);
                for (int i = 0; i < nPlacemarks; ++i) {
                    points.add(new Point(latitudes[i], longitudes[i]));
                }
                List<PlacemarkMapObject> placemarks = mapObjects.addPlacemarks(points, rectangleImage, iconStyle);
                long timeFinished = System.currentTimeMillis();

                Log.w(nPlacemarks + " I-PLACEMARKS ADDED", "Elapsed time: " + (timeFinished - timeBegan));
                mapObjects.clear();
            }
        });

        scenarios.add(createMskTilesDecoderTest(mapview.getMapWindow(), this, 120));
        scenarios.add(createSpbTilesDecoderTest(mapview.getMapWindow(), this, 40));
        scenarios.add(new PerformanceTestScenario(
            mapview.getMapWindow(),
            this,
            createScrollTestSteps(14.5f, MOSCOW_COORDINATES, 5.f, 120.f, 40.f),
            "MSK 40s Loop Tiles decoder(120s)"));

        scenarios.add(new PerformanceTestScenario(
            mapview.getMapWindow(), this,
            createZoomTestSteps(new Point(55.752121, 37.617664), 16.5f, new Point(55.752121, 37.617664), 7.0f, 60.0f, 144.0f, 24.0f),
            "MSK Long zoom Slow"));

        return scenarios;
    }

    @Override
    protected void setNightModeEnabled(boolean enabled) {
        mapview.getMap().setNightModeEnabled(enabled);
        startStopwatch();
    }

    @Override
    protected void setModelsEnabled(boolean enabled) {
        mapview.getMap().setModelsEnabled(enabled);
        startStopwatch();
    }

    private Bitmap createRectangleBitmap(int colorTop, int colorBottom, int width, int height) {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        width = (int)(width * metrics.density + 0.5);
        height = (int)(height * metrics.density + 0.5);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Paint p = new Paint();
        p.setColor(colorTop);
        p.setStyle(Paint.Style.FILL);
        Canvas c = new Canvas(bitmap);
        c.drawRect(0, 0, width, 0.5f * height, p);
        p.setColor(colorBottom);
        c.drawRect(0, 0.5f * height, width, height, p);
        return bitmap;
    }

    protected List<PerformanceTestScenario> scenarios;
    private TextView loadedTimeView_ = null;
    private Spinner scenarioSpinner;

    @SuppressLint("SetTextI18n")
    @Override
    public void onMapLoaded(MapLoadStatistics statistics) {
        loadedTimeView_.setText(Float.toString(statistics.getFullyAppeared() / 1000.f));
        final String message = MapBaseActivity.toString(statistics);
        Log.w("MAP_LOAD_STATISTICS", message);
        loadedTimeView_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage(message, "Map Load Status");
            }
        });
        String renderingResult = mapview.stopPerformanceMetricsCapture();
        String decoderResult = mapview.getMap().stopTileLoadMetricsCapture();
        showPerformanceResult("Map Load Status", renderingResult, decoderResult);
    }

    private void startStopwatch() {
        mapview.getMap().setMapLoadedListener(this);
        mapview.startPerformanceMetricsCapture();
        mapview.getMap().startTileLoadMetricsCapture();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.map_performance);
        super.onCreate(savedInstanceState);

        roadEventsLayerStyleProvider = new RoadEventsLayerDefaultStyleProvider(
                getApplicationContext());
        roadEventsLayer = MapKitFactory.getInstance().createRoadEventsLayer(
                mapview.getMapWindow(),
                roadEventsLayerStyleProvider);

        loadedTimeView_ = findViewById(R.id.map_base_loaded_time);

        scenarios = createScenarios();
        scenarioSpinner = findViewById(R.id.scenario_spinner);
        ArrayAdapter<PerformanceTestScenario> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, scenarios);
        scenarioSpinner.setAdapter(adapter);

        startStopwatch();
    }

    public void executeTest(View view)
    {
        scenarios.get(scenarioSpinner.getSelectedItemPosition()).execute();
    }
}
