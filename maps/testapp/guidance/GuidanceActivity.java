package com.yandex.maps.testapp.guidance;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.Action;
import com.yandex.mapkit.directions.driving.ConditionsListener;
import com.yandex.mapkit.directions.driving.DirectionSign;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.internal.DirectionSignBitmapFactory;
import com.yandex.mapkit.directions.driving.StandingSegment;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.guidance.AnnotatedEventTag;
import com.yandex.mapkit.directions.guidance.AnnotationWithDistance;
import com.yandex.mapkit.directions.guidance.ClassifiedLocation;
import com.yandex.mapkit.directions.guidance.FasterAlternative;
import com.yandex.mapkit.directions.guidance.GuidanceListener;
import com.yandex.mapkit.directions.guidance.Guide;
import com.yandex.mapkit.directions.guidance.LocationClass;
import com.yandex.mapkit.directions.guidance.PerformanceMonitor;
import com.yandex.mapkit.directions.guidance.SpeedingPolicy;
import com.yandex.mapkit.directions.guidance.StandingStatus;
import com.yandex.mapkit.directions.guidance.UpcomingEvent;
import com.yandex.mapkit.directions.guidance.ViewArea;
import com.yandex.mapkit.directions.simulation.RecordedSimulator;
import com.yandex.mapkit.directions.simulation.RecordedSimulatorListener;
import com.yandex.mapkit.geometry.Circle;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.geometry.SubpolylineHelper;
import com.yandex.mapkit.LocalizedValue;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationSimulatorListener;
import com.yandex.mapkit.map.CameraListener;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.CameraUpdateReason;
import com.yandex.mapkit.map.CircleMapObject;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.maps.recording.RecordCollector;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.datacollect.requests.DatacollectRequestsFragment;
import com.yandex.maps.testapp.driving.RouteView;
import com.yandex.maps.testapp.driving.VehicleOptionsProvider;
import com.yandex.maps.testapp.guidance.test.TestCase;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.maps.testapp.R;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.recording.EventListener;
import com.yandex.runtime.recording.EventLoggingFactory;
import com.yandex.runtime.recording.ReportData;
import com.yandex.runtime.Runtime;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class GuidanceSettings {
    GuidanceSettings() {
        switches.put(R.id.maneuvers, true);
        switches.put(R.id.speed_excess, true);
        switches.put(R.id.road_events, true);
        switches.put(R.id.freedrive, true);

        // AnnotatedEventTags
        switches.put(R.id.speed_control, true);
        switches.put(R.id.mobile_control, true);
        switches.put(R.id.cross_road_control, true);
        switches.put(R.id.road_marking_control, true);
        switches.put(R.id.lane_control, true);
        switches.put(R.id.school, true);
        switches.put(R.id.accident, true);
        switches.put(R.id.reconstruction, true);
        switches.put(R.id.danger, true);
        switches.put(R.id.overtaking_danger, true);
        switches.put(R.id.pedestrian_danger, true);
        switches.put(R.id.cross_road_danger, true);

        // Display settings
        switches.put(R.id.show_events, true);
        switches.put(R.id.show_lanes, false);
        switches.put(R.id.show_maneuvers, true);
        switches.put(R.id.show_traffic_lights, true);
        switches.put(R.id.show_direction_signs, true);

        // Driving options
        switches.put(R.id.guidance_avoid_tolls, false);
        switches.put(R.id.guidance_avoid_unpaved, false);
        switches.put(R.id.guidance_avoid_poor_conditions, false);
    }

    Float speedLimitsRatio = 1.0f;
    AnnotationLanguage speakerLanguage = AnnotationLanguage.RUSSIAN;
    TreeMap<Integer, Boolean> switches = new TreeMap<>();
}

class SimulationParameters {
    static final int MAX_SIMULATION_SPEED = 40;
    static final int MAX_CLOCK_RATE = 10;
    static final int MIN_CLOCK_RATE = 1;

    boolean muted = false;
    boolean showStandingSegments = false;
    boolean useParkingRoutes = false;
    boolean backgroundWork = false;
    boolean recordedSimulationPaused = false;
}

public class GuidanceActivity extends MapBaseActivity implements
        InputListener,
        GuidanceListener,
        CameraListener,
        MapObjectTapListener,
        SimulationDockFragment.SimulationDockContext,
        GuidanceSettingsFragment.AnnotationSettingsContext,
        EventListener {

    enum AnnotationTableau {
        Primary,
        Secondary
    }

    private static Logger LOGGER = Logger.getLogger("yandex.maps.guidance");

    private static final String MENU_FRAGMENT_TAG = "GUIDANCE_MENU_FRAGMENT_TAG";
    private static final String SETTINGS_FRAGMENT_TAG = "GUIDANCE_SETTINGS_FRAGMENT_TAG";
    private static final String DATACOLLECT_FRAGMENT_TAG = "DATACOLLECT_REQUESTS_FRAGMENT_TAG";

    private static final String FLAG_HAS_ROADS_IN_POOR_CONDITION = new String(new int[] {0x26a0, 0xfe0f}, 0, 2); // ⚠️
    private static final String FLAG_HAS_UNPAVED_ROADS = new String(new int[] {0x1f69c}, 0, 1); // 🚜

    private static final int RED_COLOR = 0xc0ff0000;

    private final long START_TIMESTAMP = 0;

    private TextView roadNameText;
    private Button followButton;
    private TextView simulationSpeedText;
    private TextView nextCameraText;
    private ImageView nextCameraImage;
    private TextView timeText;
    private TextView trafficTimeText;
    private TextView distanceText;
    private TextView zoomText;
    private TextView speedingText;
    private View primaryAnnotation;
    private View secondaryAnnotation;
    private TextView tollRouteLabel;
    private TextView vehicleTypeLabel;
    private TextView ruggedRouteLabel;
    private Button offlineButton;
    private TextView estimatedTimeOfArrival;
    private ImageView directionSignImage;

    private VehicleOptions selectedVehicleOptions = new VehicleOptions();

    private MapObjectCollection mapObjects;
    private PlacemarkMapObject locationPoint;
    private CircleMapObject locationPointCircle;
    private ArrayList<PolylineMapObject> standingSegments = new ArrayList();

    private MockSpeaker mockSpeaker;

    private Guide guide;
    private GuidanceSettings guidanceSettings;
    private SimulationParameters simulationParameters;

    private RouterController routerController;
    private RouteView routeView;
    private SimpleRouteView fasterAlternativeRouteViewer;
    private RouteView freeDriveRouteViewer;
    private ArrayList<RouteView> alternativesView = new ArrayList<>();
    private boolean updateAlternativesTimes = false;

    private static final float ALTERNATIVE_Z_INDEX = -2.0f;
    private static final float FASTER_ALTERNATIVE_Z_INDEX = -1.0f;
    private static final float FREE_DRIVE_ROUTE_Z_INDEX = -1.0f;

    private RoutePointsController routePointsController;
    private SimulationController simulationController;

    private RecordedSimulator recordedSimulator;
    private RecordedSimulatorListener recordedSimulatorListener;
    private boolean simulationRunning = false;

    private final float ZOOM_VIEW_OUT = 0.2f;

    private RecordCollector recordCollector;

    private ImageProvider arrowIcon;
    private ImageProvider arrowStandingIcon;
    private ImageProvider transparentArrowIcon;
    private ImageProvider pointIcon;
    private ImageProvider currentPositionIcon;

    private boolean follow;

    static final int TEST_CASE_REQUEST_CODE = 1;
    static final int FETCH_REPORT_REQUEST_CODE = 2;

    private boolean routeLocked = false;
    private boolean isStandingDetected = false;

    private ArrayList<PerformanceMonitor> performanceMonitors;

    private String currentSimulationSpeedText;
    private String currentNextCameraText;

    private ConditionsListener conditionsListener = new ConditionsListener() {
        @Override
        public void onConditionsUpdated() {
            updateStandingSegments();
        }

        @Override
        public void onConditionsOutdated() {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.guidance);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); //TTS

        toolbar.setVisibility(View.GONE);

        roadNameText = findViewById(R.id.roadName);
        followButton = findViewById(R.id.findMeButton);
        simulationSpeedText = findViewById(R.id.speedValue);
        nextCameraText = findViewById(R.id.nextCameraText);
        nextCameraImage = findViewById(R.id.nextCameraImage);
        timeText = findViewById(R.id.timeValue);
        trafficTimeText = findViewById(R.id.trafficTimeValue);
        distanceText = findViewById(R.id.distanceValue);
        zoomText = findViewById(R.id.zoomValue);
        speedingText = findViewById(R.id.speedLimitValue);
        primaryAnnotation = findViewById(R.id.primaryAnnotation);
        secondaryAnnotation = findViewById(R.id.secondaryAnnotation);
        tollRouteLabel = findViewById(R.id.toll_route_text);
        vehicleTypeLabel = findViewById(R.id.vehicle_type_text);
        ruggedRouteLabel = findViewById(R.id.rugged_route_text);
        offlineButton = findViewById(R.id.offlineButton);
        estimatedTimeOfArrival = findViewById(R.id.estimatedTimeOfArrival);
        directionSignImage = findViewById(R.id.directionSignImage);

        MapKitFactory.getInstance().resetLocationManagerToDefault();
        guide = DirectionsFactory.getInstance().createGuide();
        guidanceSettings = new GuidanceSettings();
        simulationParameters = new SimulationParameters();
        recordCollector = RecordingFactory.getInstance().recordCollector();

        arrowIcon = ImageProvider.fromResource(getApplicationContext(), R.drawable.navigation_icon);
        arrowStandingIcon = ImageProvider.fromResource(getApplicationContext(), R.drawable.navigation_black_icon);
        transparentArrowIcon = ImageProvider.fromBitmap(adjustOpacity(arrowIcon.getImage(), 125));
        pointIcon = ImageProvider.fromResource(getApplicationContext(), R.drawable.ya_point);

        mapObjects = mapview.getMap().getMapObjects().addCollection();

        routerController = new RouterController();

        routeView = new RouteView(mapview.getMap(), null, new RouteView.ImageProviderFactory() {
            @Override
            public ImageProvider fromResource(int resourceId) {
                return ImageProvider.fromResource(getApplicationContext(), resourceId);
            }
        });

        routeView.setSelectedArrivalPointsEnabled(true);
        routeView.setTapListener(this);

        setDirectionSignEnabled(true);

        freeDriveRouteViewer = new RouteView(mapview.getMap(), null, new RouteView.ImageProviderFactory() {
            @Override
            public ImageProvider fromResource(int resourceId) {
                return ImageProvider.fromResource(getApplicationContext(), resourceId);
            };
        });
        freeDriveRouteViewer.setZIndex(FREE_DRIVE_ROUTE_Z_INDEX);
        freeDriveRouteViewer.setWidthScale(0.3f);
        freeDriveRouteViewer.setJamsEnabled(true);
        freeDriveRouteViewer.setEventsEnabled(true);
        freeDriveRouteViewer.setManeuversEnabled(false);
        freeDriveRouteViewer.setTrafficLightsAndBarriersEnabled(false);
        freeDriveRouteViewer.setSelectedArrivalPointsEnabled(false);

        EventLoggingFactory.getEventLogging().subscribe(this);

        routePointsController = new RoutePointsController(mapObjects, guide);

        simulationController = new SimulationController(mapObjects, guide);

        mapview.getMap().addInputListener(this);
        mapview.getMap().addCameraListener(this);
        // Force map type to vector (MAPSMOBCORE-8957).
        // Note that this doesn't affect global map type setting.
        mapview.getMap().setMapType(com.yandex.mapkit.map.MapType.VECTOR_MAP);
        guide.subscribe(this);

        mockSpeaker = new MockSpeaker(
            this,
            guidanceSettings.speakerLanguage,
            new MockSpeaker.SayCallback() {
                @Override
                public void onSay(final String string) {
                    showUserMessage(string, Level.INFO); // TODO UIKit
                }
            }
        );

        guide.setLocalizedSpeaker(
            mockSpeaker,
            guidanceSettings.speakerLanguage
        );

        guide.setFreeDrivingAnnotationsEnabled(true);
        guide.setAlternativesEnabled(true);
        onSettingsChanged();

        updateZoomText(mapview.getMap().getCameraPosition().getZoom());

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

    void updateZoomText(double zoom) {
        zoomText.setText(String.format("zoom: %.1f", zoom));
    }

    public int resourceIdByEventTag(EventTag tag) {
        switch (tag) {
            case ACCIDENT:
                return R.drawable.accident;
            case CHAT:
            case LOCAL_CHAT:
                return R.drawable.chat;
            case CLOSED:
                return R.drawable.closed;
            case DRAWBRIDGE:
                return R.drawable.drawbridge;
            case RECONSTRUCTION:
                return R.drawable.reconstruction;
            case SCHOOL:
                return R.drawable.school;
            case TRAFFIC_ALERT:
                return R.drawable.traffic_alert;
            case DANGER:
                return R.drawable.other; // TODO(dbeliakov) icon for danger

            case POLICE:
                return R.drawable.police;
            case SPEED_CONTROL:
                return R.drawable.speed_camera;
            case LANE_CONTROL:
                return R.drawable.lane_camera;
            case MOBILE_CONTROL:
                return R.drawable.mobile_control;
            case ROAD_MARKING_CONTROL:
                return R.drawable.road_marking_control;
            case CROSS_ROAD_CONTROL:
                return R.drawable.cross_road_control;
            case NO_STOPPING_CONTROL:
                return R.drawable.no_stopping_control;

            default:
                return R.drawable.other;
        }
    }

    private ImageProvider imageByEventTag(EventTag tag) {
        return ImageProvider.fromResource(this, resourceIdByEventTag(tag));
    }

    public int resourceIdByAnnotationAction(Action type) {
        switch (type) {
            case UNKNOWN: return R.drawable.other;
            case STRAIGHT: return R.drawable.ra_next_forward;
            case SLIGHT_LEFT: return R.drawable.ra_next_take_left;
            case SLIGHT_RIGHT: return R.drawable.ra_next_take_right;
            case LEFT: return R.drawable.ra_next_turn_left;
            case RIGHT: return R.drawable.ra_next_turn_right;
            case HARD_LEFT: return R.drawable.ra_next_hard_turn_left;
            case HARD_RIGHT: return R.drawable.ra_next_hard_turn_right;
            case FORK_LEFT: return R.drawable.ra_next_take_left;
            case FORK_RIGHT: return R.drawable.ra_next_take_right;
            case UTURN_LEFT: return R.drawable.ra_next_hard_turn_left;
            case UTURN_RIGHT: return R.drawable.ra_next_hard_turn_right;
            case ENTER_ROUNDABOUT: return R.drawable.ra_next_in_circular_movement;
            case LEAVE_ROUNDABOUT: return R.drawable.ra_next_out_circular_movement;
            case BOARD_FERRY: return R.drawable.ra_next_boardferry;
            case LEAVE_FERRY: return R.drawable.ra_next_boardferry;
            case EXIT_LEFT: return R.drawable.ra_next_turn_left;
            case EXIT_RIGHT: return R.drawable.ra_next_turn_right;
            case FINISH: return R.drawable.ra_next_finish;
            default: return R.drawable.other;
        }
    }

    private ImageProvider imageByAnnotationAction(Action action) {
        return ImageProvider.fromResource(this, resourceIdByAnnotationAction(action));
    }

    @Override
    protected void onDestroy() {
        if (simulationParameters.backgroundWork)
            stopBackgroundService();

        guide.setFreeDrivingAnnotationsEnabled(false);
        guide.resetRoute();

        resetSimulationOrTestCase();
        resetRecordedSimulation();
        guide.unsubscribe(this);
        EventLoggingFactory.getEventLogging().unsubscribe(this);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!simulationParameters.backgroundWork) {
            if (recordedSimulator != null) {
                simulationRunning = recordedSimulator.isActive();
                pauseRecordedSimulation();
            }
            else {
                guide.suspend();
            }
        } else {
            startBackgroundService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!simulationParameters.backgroundWork) {
            if (recordedSimulator == null) {
                guide.resume();
            }
            else if (simulationRunning) {
                resumeRecordedSimulation();
            }
        } else {
            stopBackgroundService();
        }
    }

    private void startBackgroundService() {
        final Context context = Runtime.getApplicationContext();
        Intent intent = new Intent(context, BackgroundService.class);
        context.startService(intent);
    }

    private void stopBackgroundService() {
        final Context context = Runtime.getApplicationContext();
        Intent intent = new Intent(context, BackgroundService.class);
        context.stopService(intent);
    }

    private void followArrow() {
        follow = true;
        followButton.setVisibility(View.GONE);
    }

    private void updateTollRouteLabel(DrivingRoute route) {
        if (route == null) {
            tollRouteLabel.setVisibility(View.INVISIBLE);
            return;
        }
        tollRouteLabel.setVisibility(View.VISIBLE);
        if (route.getMetadata().getFlags().getHasTolls()) {
            tollRouteLabel.setText("$$$");
            tollRouteLabel.setTextColor(Color.RED);
        } else {
            tollRouteLabel.setText("Free");
            tollRouteLabel.setTextColor(Color.GREEN);
        }
    }

    private void updateVehicleTypeLabel(DrivingRoute route) {
        if (route == null) {
            vehicleTypeLabel.setVisibility(View.INVISIBLE);
            return;
        }
        vehicleTypeLabel.setVisibility(View.VISIBLE);
        vehicleTypeLabel.setText(VehicleOptionsProvider.nameOf(
                route.getVehicleOptions().getVehicleType()));
    }

    private void updateRuggedRouteLabel(DrivingRoute route) {
        ruggedRouteLabel.setVisibility(View.GONE);
        if (route == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (route.getMetadata().getFlags().getHasUnpavedRoads()) {
            sb.append(FLAG_HAS_UNPAVED_ROADS);
        }
        if (route.getMetadata().getFlags().getHasInPoorConditionRoads()) {
            sb.append(FLAG_HAS_ROADS_IN_POOR_CONDITION);
        }

        ruggedRouteLabel.setText(sb);
        ruggedRouteLabel.setVisibility(View.VISIBLE);
    }

    private void updateOfflineButton(DrivingRoute route){
        if (route == null) {
            offlineButton.setVisibility(View.INVISIBLE);
        } else {
            offlineButton.setVisibility(route.getMetadata().getFlags().getBuiltOffline()?
                View.VISIBLE : View.INVISIBLE
            );
            offlineButton.setEnabled(!simulationController.isActive());
            offlineButton.setTextColor(Color.RED);
        }
    }

    private void updateTimeLabel(DrivingRoute route) {
        if (route == null) {
            timeText.setVisibility(View.INVISIBLE);
            trafficTimeText.setVisibility(View.INVISIBLE);
        } else {
            timeText.setVisibility(View.VISIBLE);
            trafficTimeText.setVisibility(View.VISIBLE);
            timeText.setText("time: " +
                route.getMetadata().getWeight().getTime().getText());
            trafficTimeText.setText("traffic time: " +
                route.getMetadata().getWeight().getTimeWithTraffic().getText());
        }
    }

    private void updateDistanceLabel(DrivingRoute route) {
        if (route == null) {
            distanceText.setVisibility(View.INVISIBLE);
        } else {
            distanceText.setVisibility(View.VISIBLE);
            distanceText.setText("distance: " +
                route.getMetadata().getWeight().getDistance().getText());
        }
    }

    private void runTestCase(final TestCase testCase) {
        assert testCase != null;

        resetSimulationOrTestCase();
        resetRecordedSimulation();

        simulationParameters.useParkingRoutes = testCase.useParkingRoutes;
        guide.setParkingRoutesEnabled(simulationParameters.useParkingRoutes);
        guide.setAlternativesEnabled(!testCase.disableAlternatives);
        guide.setVehicleOptions(testCase.vehicleOptions);
        guide.suspend();

        disableRouteChanging();

        followArrow();

        if (testCase.route != null) {
            routePointsController.setPoints(testCase.routePoints);
            guide.setRoute(testCase.route);
            setRoute(testCase.route);
        }
        simulationController.start(testCase.simulationRoute, new LocationSimulatorListener() {
            @Override
            public void onSimulationFinished() { resetSimulationOrTestCase(); }
        });
        guide.resume();
    }

    private void runRecordedSimulation(ReportData report) {

        resetSimulationOrTestCase();
        resetRecordedSimulation();

        disableRouteChanging();

        guide.setAlternativesEnabled(false);
        guide.setReroutingEnabled(false);

        recordedSimulator = DirectionsFactory.getInstance().createRecordedSimulator(report);
        recordedSimulatorListener = new RecordedSimulatorListener() {

            @Override
            public void onLocationUpdated() {
            }

            @Override
            public void onRouteUpdated() {
                DrivingRoute route = recordedSimulator.getRoute();
                routeView.setRoute(route);
                if (route == null)
                    routerController.cancelSession();
                updateTollRouteLabel(route);
                updateVehicleTypeLabel(route);
                updateRuggedRouteLabel(route);
                updateOfflineButton(route);
                updateTimeLabel(route);
                updateDistanceLabel(route);
                guide.setRoute(route);
            }

            @Override
            public void onRouteUriUpdated() {
            }

            @Override
            public void onProblemMark() {
                showUserMessage("Problem mark", Level.INFO);
            }

           @Override
            public void onFinish() {
                showUserMessage("Simulation finished", Level.INFO);
                pauseRecordedSimulation();
                recordedSimulator.setTimestamp(START_TIMESTAMP);
            }
        };

        recordedSimulator.subscribeForSimulatorEvents(recordedSimulatorListener);

        followArrow();
        simulationRunning = true;
    }

    @Override
    public void onCameraPositionChanged(Map map, CameraPosition cameraPosition,
                                        CameraUpdateReason reason, boolean finished) {
        // FIXME: updating UI elements in this listener is a really bad idea
        // need to cache value and update only if changed
        updateZoomText(cameraPosition.getZoom());
        if (reason == CameraUpdateReason.GESTURES) {
            follow = false;
            followButton.setVisibility(View.VISIBLE);
        }
    }

    private void showUserMessage(String message, Level level) {
        if (level != Level.OFF)
            LOGGER.log(level, message);
        Toast.makeText(GuidanceActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        if (routeLocked) {
            return;
        }

        ClassifiedLocation classifiedLocation = guide.getLocation();
        if (classifiedLocation == null) {
            return;
        }
        Location currentLocation = classifiedLocation.getLocation();
        if (currentLocation == null) {
            return;
        }

        DrivingOptions drivingOptions = drivingOptionFromSettings();
        drivingOptions.setInitialAzimuth(currentLocation.getHeading());

        if (simulationController.isActive()) {
            simulationController.addRoutePoint(point, drivingOptions);
        } else {
            routePointsController.addPoint(
                    new RequestPoint(
                            point,
                            RequestPointType.WAYPOINT,
                            null /* pointContext */));
            routerController.requestRoute(
                    routePointsController.getRequestPoints(),
                    drivingOptions,
                    selectedVehicleOptions,
                    new RouterController.RequestRouteCallback() {
                        @Override
                        public void onSuccess(DrivingRoute route) {
                            setRoute(route);
                            guide.setRoute(route);
                        }

                        @Override
                        public void onError(Error error) {
                            showUserMessage(error.toString(), Level.WARNING);
                        }
                    });
        }
    }

    private static Bitmap adjustOpacity(Bitmap bitmap, int opacity)
    {
        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        int colour = (opacity & 0xFF) << 24;
        canvas.drawColor(colour, PorterDuff.Mode.DST_IN);
        return mutableBitmap;
    }

    private void updatePositionIcon(ImageProvider icon) {
        if (currentPositionIcon != icon) {
            currentPositionIcon = icon;
            locationPoint.setIcon(icon, new IconStyle().setFlat(true).setRotationType(RotationType.ROTATE));
        }
    }

    private void updatePosition(ClassifiedLocation location) {
        Location currentLocation = location.getLocation();

        if (locationPoint != null) {
            locationPoint.setGeometry(currentLocation.getPosition());
        } else {
            locationPoint = mapObjects.addPlacemark(currentLocation.getPosition());
            locationPoint.setZIndex(50.0f);
        }

        Double accuracy = currentLocation.getAccuracy();
        if (accuracy != null) {
            Circle circle = new Circle(currentLocation.getPosition(), accuracy.floatValue());
            int component = (int) (255 / (1.0 + accuracy));
            int fillColor = Color.argb(0x80, 0xff - component, component, 0);
            if (locationPointCircle != null) {
                locationPointCircle.setGeometry(circle);
                locationPointCircle.setStrokeColor(Color.TRANSPARENT);
                locationPointCircle.setStrokeWidth(0.f);
                locationPointCircle.setFillColor(fillColor);
            } else {
                locationPointCircle = mapObjects.addCircle(circle, Color.TRANSPARENT, 0.f, fillColor);
            }
        } else if (locationPointCircle != null) {
            locationPointCircle.setVisible(false);
        }

        LocationClass locationClass = location.getLocationClass();
        if (isStandingDetected) {
            updatePositionIcon(arrowStandingIcon);
            locationPointCircle.setVisible(false);
        } else if (locationClass == LocationClass.FINE) {
            updatePositionIcon(arrowIcon);
            locationPointCircle.setVisible(false);
        } else if (locationClass == LocationClass.EXTRAPOLATED) {
            updatePositionIcon(transparentArrowIcon);
            locationPointCircle.setVisible(false);
        } else if (locationClass == LocationClass.COARSE) {
            updatePositionIcon(pointIcon);
            locationPointCircle.setVisible(true);
        } else {
            LOGGER.warning("Unknown location class");
        }
    }

    private void updateEstimatedTimeOfArrival() {
        DrivingRoute route = routeView.getRoute();

        if (route == null) {
            estimatedTimeOfArrival.setText("");
            return;
        }

        long remainingTime = (long) route
                .getMetadata().getWeight().getTimeWithTraffic().getValue(); // seconds
        String arrivalTime = new SimpleDateFormat("HH:mm:ss")
                .format(Calendar.getInstance().getTimeInMillis() + remainingTime * 1000);

        estimatedTimeOfArrival.setText("ETA: " + arrivalTime);
    }

    private void setRoute(DrivingRoute route) {
        if (routeView.getRoute() != null) {
            routeView.getRoute().removeConditionsListener(conditionsListener);
        }
        routeView.setRoute(route);
        updateTollRouteLabel(route);
        updateVehicleTypeLabel(route);
        updateOfflineButton(route);
        updateTimeLabel(route);
        updateDistanceLabel(route);
        updateDirectionSign();
        updateStandingSegments();

        if (route == null) {
            routePointsController.clear();
        } else {
            route.addConditionsListener(conditionsListener);
        }
    }

    @Override
    public void onLocationUpdated() {
        ClassifiedLocation location = guide.getLocation();
        if (location == null ||
                location.getLocation() == null ||
                location.getLocation().getPosition() == null) {
            LOGGER.warning("Null instead of guide location");
            return;
        }

        updatePosition(location);

        assert locationPoint != null;

        updateSimulationSpeedText(location.getLocation().getSpeed());

        if (follow) {
            moveToCurrentPosition(false, null);
        }
        if (location.getLocation().getHeading() != null) {
            locationPoint.setDirection(location.getLocation().getHeading().floatValue());
        }
        DrivingRoute freeDriveRoute = guide.getFreeDriveRoute();
        if (freeDriveRoute != null) {
            freeDriveRouteViewer.setPosition(freeDriveRoute.getPosition());
        }

        updateEstimatedTimeOfArrival();
    }

    private void moveToCurrentPosition(boolean animated, Map.CameraCallback callback) {
        if (guide.getLocation() == null || guide.getLocation().getLocation() == null) {
            showUserMessage("Current location is unknown", Level.WARNING);
            return;
        }
        Location currentLocation = guide.getLocation().getLocation();
        ViewArea viewArea = guide.getViewArea();
        CameraPosition position = mapview.getMap().getCameraPosition();
        float azimuth = currentLocation.getHeading() != null ?
                currentLocation.getHeading().floatValue() : position.getAzimuth();
        Point center = GuidanceUtils.calculateViewCenter(viewArea, azimuth, currentLocation.getPosition());
        float zoom = mapview.getMap().cameraPosition(GuidanceUtils.calculateViewBoundingBox(viewArea, center)).getZoom();
        position = new CameraPosition(
                center,
                zoom - ZOOM_VIEW_OUT,
                azimuth,
                position.getTilt());
        Animation animation = new Animation(Animation.Type.SMOOTH, animated ? 0.5f : 0.0f);
        mapview.getMap().move(position, animation, callback);
    }

    private void setAnnotation(AnnotationWithDistance awd, AnnotationTableau tableau) {
        ImageView image = null;
        TextView text = null;
        View tableauView = null;
        if (tableau == AnnotationTableau.Primary) {
            image = (ImageView) findViewById(R.id.primaryAnnotationImage);
            text = (TextView) findViewById(R.id.primaryAnnotationText);
            tableauView = findViewById(R.id.primaryAnnotation);
        } else if (tableau == AnnotationTableau.Secondary) {
            image = (ImageView) findViewById(R.id.secondaryAnnotationImage);
            text = (TextView) findViewById(R.id.secondaryAnnotationText);
            tableauView = findViewById(R.id.secondaryAnnotation);
        }
        if (image == null || text == null) {
            throw new AssertionError("Cannot find tableau for annotation");
        }
        image.setImageDrawable(getApplicationContext().getResources().getDrawable(
                resourceIdByAnnotationAction(awd.getAnnotation().getAction())));
        text.setText(awd.getDistance().getText());
        tableauView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLostRoute() {
        showUserMessage("Route lost!", Level.WARNING);
    }

    @Override
    public void onAnnotationsUpdated() {
        primaryAnnotation.setVisibility(View.INVISIBLE);
        secondaryAnnotation.setVisibility(View.INVISIBLE);

        List<AnnotationWithDistance> annotations = guide.getDisplayedAnnotations().getAnnotations();
        if (annotations.size() == 1) {
            setAnnotation(annotations.get(0), AnnotationTableau.Primary);
        } else if (annotations.size() > 1) {
            setAnnotation(annotations.get(0), AnnotationTableau.Primary);
            setAnnotation(annotations.get(1), AnnotationTableau.Secondary);
        }
    }

    @Override
    public void onReturnedToRoute() {
        showUserMessage("Returned to route", Level.INFO);
    }

    @Override
    public void onFinishedRoute() {
        showUserMessage("Route finished", Level.INFO);
    }

    @Override
    public void onReachedWayPoint() {
        showUserMessage("Waypoint reached", Level.INFO);
    }

    @Override
    public void onRouteUpdated() {
        if (recordedSimulator != null) {
            return;
        }

        if (guide.getRoute() != null) {
            showUserMessage("Route updated", Level.INFO);
        }
        setRoute(guide.getRoute());
    }

    @Override
    public void onRoadNameUpdated() {
        if (guide.getRoadName() != null) {
            roadNameText.setText(guide.getRoadName());
        } else {
            roadNameText.setText("");
        }
    }

    private void updateSimulationSpeedText(Double speedMS) {
        String text = "";
        if (speedMS == null) {
            text = "? km/h";
        } else {
            double speedKMH = speedMS * 3.6;
            text = String.format("%.0f km/h", speedKMH);
        }
        if (text != currentSimulationSpeedText) {
            currentSimulationSpeedText = text;
            simulationSpeedText.setText(text);
        }
    }

    private void updateNextCameraText(UpcomingEvent camera) {
        String text;
        if (camera != null) {
            nextCameraText.setVisibility(View.VISIBLE);
            if (camera.getAnnotatingNow()) {
                nextCameraImage.setImageResource(R.drawable.lane_camera);
            } else {
                nextCameraImage.setImageResource(R.drawable.speed_camera);
            }
            nextCameraImage.setVisibility(View.VISIBLE);
            switch (camera.getSpeedStatus()) {
                case BELOW_LIMIT:
                    text = "Below limit";
                    break;
                case STRICT_LIMIT_EXCEEDED:
                    text = "Strict limit exceeded";
                    break;
                case TOLERANT_LIMIT_EXCEEDED:
                    text = "Tolerant limit exceeded";
                    break;
                default:
                    text = "Unknown status";
            }

            if (text != currentNextCameraText) {
                currentNextCameraText = text;
                nextCameraText.setText(text);
            }
        } else {
            nextCameraText.setVisibility(View.GONE);
            nextCameraImage.setVisibility(View.INVISIBLE);
        }
    }

    private void runSimulation() {
        if (routeView.getRoute() == null) {
            showUserMessage("No route for simulation", Level.WARNING);
            return;
        }
        simulationController.start(routeView.getRoute(), new LocationSimulatorListener() {
            @Override
            public void onSimulationFinished() {
                resetSimulationOrTestCase();
            }
        });

        updateOfflineButton(routeView.getRoute());
    }

    private void showStandingSegments() {
        for (StandingSegment segment : routeView.getRoute().getStandingSegments()) {
            List<Point> standingSegmentGeometry = SubpolylineHelper.subpolyline(
                    routeView.getRoute().getGeometry(),
                    segment.getPosition()
            ).getPoints();
            PolylineMapObject polyline = mapObjects.addPolyline(
                    new Polyline(standingSegmentGeometry)
            );
            polyline.setStrokeColor(Color.CYAN);
            standingSegments.add(polyline);
        }
    }

    private void hideStandingSegments() {
        for (PolylineMapObject polyline : standingSegments) {
            mapObjects.remove(polyline);
        }
        standingSegments.clear();
    }

    private void resetSimulationOrTestCase() {
        if (!simulationController.isActive()) {
            return;
        }

        simulationController.stop();

        guide.resetRoute();
        setRoute(null);
        guide.setAlternativesEnabled(true);
        guide.setVehicleOptions(selectedVehicleOptions);

        enableRouteChanging();

        recordCollector.startReport();
    }

    private void resumeRecordedSimulation() {
        if (recordedSimulator == null) {
            return;
        }

        guide.resetRoute();
        MapKitFactory.getInstance().setLocationManager(recordedSimulator);
        guide.resume();
        recordedSimulator.resume();
    }

    private void pauseRecordedSimulation() {
        if (recordedSimulator == null) {
            return;
        }

        guide.suspend();
        recordedSimulator.suspend();
    }

    private void resetRecordedSimulation() {
        if (recordedSimulator == null)
            return;
        simulationRunning = false;
        pauseRecordedSimulation();
        guide.resetRoute();
        MapKitFactory.getInstance().resetLocationManagerToDefault();
        setRoute(null);
        recordedSimulator.setClockRate(SimulationParameters.MIN_CLOCK_RATE);
        recordedSimulator.unsubscribeFromSimulatorEvents(recordedSimulatorListener);
        recordedSimulator = null;
        recordedSimulatorListener = null;
        enableRouteChanging();
        guide.setAlternativesEnabled(true);
        guide.setReroutingEnabled(true);
        guide.resume();

        recordCollector.startReport();
    }

    private void enableRouteChanging() {
        routeLocked = false;
    }

    private void disableRouteChanging() {
        routeLocked = true;
    }

    private void updateAlternativeTimeLabels() {
        if (guide.getRoute() == null || guide.getRoutePosition() == null ||
                guide.getAlternativesTimeDifference().isEmpty()) {
            return;
        }
        Polyline routePolyline = guide.getRoute().getGeometry();
        PolylinePosition routePosition = guide.getRoutePosition();
        assert routePosition != null;

        List<DrivingRoute> alternatives = guide.getAlternatives();
        List<LocalizedValue> timeDiffs = guide.getAlternativesTimeDifference();

        for (int i = 0; i < alternatives.size(); ++i) {
            Polyline alternativePolyline = alternatives.get(i).getGeometry();
            PolylinePosition alternativePosition = alternatives.get(i).getPosition();

            List<PolylinePosition> positions = PolylineUtils.positionsOfFork(
                    alternativePolyline,
                    alternativePosition,
                    routePolyline,
                    routePosition);

            if (positions.size() == 0) {
                LOGGER.warning("Route and alternative don't match");
                continue;
            }

            PolylinePosition position = PolylineUtils.advancePolylinePosition(alternativePolyline, positions.get(0), 200.0);
            LocalizedValue timeDiff = timeDiffs.get(i);

            alternativesView.get(i).setText(
                    position,
                    (timeDiff.getValue() < 0 ? "+ " : "- ") + timeDiff.getText(),
                    timeDiff.getValue() < 0 ? Color.RED : Color.GREEN,
                    R.dimen.alternative_time_text_size);
            }
    }

    @Override
    public void onSpeedLimitUpdated() {
        String text = "?";
        if (guide.getSpeedLimit() != null)
            text = guide.getSpeedLimit().getText();
        speedingText.setText("limit: " + text);
    }

    @Override
    public void onSpeedLimitExceededUpdated() {
        speedingText.setTextColor(guide.isSpeedLimitExceeded() ? Color.RED : Color.BLACK);
    }

    // Parameters' callbacks

    @Override
    public void onSimulation(boolean turnOn) {
        if (turnOn) {
            runSimulation();
        } else {
            resetSimulationOrTestCase();
            resetRecordedSimulation();
        }
    }

    @Override
    public void onSelectTestCase() {
        Intent intent = new Intent(this, TestCasesActivity.class);
        intent.putExtra("language", guidanceSettings.speakerLanguage);
        intent.putExtra("vehicle_options", Serialization.serializeToBytes(selectedVehicleOptions));
        startActivityForResult(intent, TEST_CASE_REQUEST_CODE);
    }

    @Override
    public void onSelectReport() {
        if (AuthUtil.getCurrentAccount() == null) {
            showUserMessage("You should login to download a report", Level.WARNING);
            return;
        }
        Intent intent = new Intent(this, ReportFetchActivity.class);
        startActivityForResult(intent, FETCH_REPORT_REQUEST_CODE);
    }

    @Override
    public void onResetRoute() {
        if (routeLocked == true) {
            return;
        }
        routerController.cancelSession();
        setRoute(null);
        guide.resetRoute();
    }

    @Override
    public void onRecordPause() {
        if (recordedSimulator == null) {
            return;
        }
        if (simulationParameters.recordedSimulationPaused) {
            pauseRecordedSimulation();
        } else {
            resumeRecordedSimulation();
        }
    }

    @Override
    public void updateStandingSegments() {
        // hide old segments
        hideStandingSegments();
        // render new if any
        if (simulationParameters.showStandingSegments && routeView.getRoute() != null) {
            showStandingSegments();
        }
    }

    @Override
    public Guide getGuide() {
        return guide;
    }

    @Override
    public boolean isRecordedSimulationOn() {
        return simulationRunning;
    }

    @Override
    public void setSimulationClockRate(int clockRate) {
        if (recordedSimulator != null) {
            recordedSimulator.setClockRate(clockRate);
        }
    }

    @Override
    public int getSimulationClockRate() {
        if (recordedSimulator == null) {
            return SimulationParameters.MIN_CLOCK_RATE;
        }
        return recordedSimulator.getClockRate();
    }

    @Override
    public boolean isSimulationOn() {
        return simulationController != null && simulationController.isActive();
    }

    @Override
    public void setSimulationSpeed(int speed) {
        if (simulationController != null) {
            simulationController.setSpeed(speed);
        }
    }

    @Override
    public double getSimulationSpeed() {
        if (simulationController == null) {
            return 20.0;
        }

        return simulationController.getSpeed();
    }

    @Override
    public SimulationParameters getSimulationParameters() {
        return simulationParameters;
    }

    // Annotation Settings' callbacks

    @Override
    public void onSettingsChanged() {
        guide.setSpeedingToleranceRatio(guidanceSettings.speedLimitsRatio);
        guide.setSpeakerLanguage(guidanceSettings.speakerLanguage);
        mockSpeaker.setLanguage(guidanceSettings.speakerLanguage);

        guide.setRouteActionsAnnotated(guidanceSettings.switches.get(R.id.maneuvers));
        guide.setSpeedLimitExceededAnnotated(guidanceSettings.switches.get(R.id.speed_excess));
        guide.setRoadEventsAnnotated(guidanceSettings.switches.get(R.id.road_events));
        guide.setFreeDrivingAnnotationsEnabled(guidanceSettings.switches.get(R.id.freedrive));

        guide.setTollAvoidanceEnabled(guidanceSettings.switches.get(R.id.guidance_avoid_tolls));

        // AnnotatedEventTags
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.SPEED_CONTROL,
                guidanceSettings.switches.get(R.id.speed_control));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.MOBILE_CONTROL,
                guidanceSettings.switches.get(R.id.mobile_control));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.CROSS_ROAD_CONTROL,
                guidanceSettings.switches.get(R.id.cross_road_control));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.ROAD_MARKING_CONTROL,
                guidanceSettings.switches.get(R.id.road_marking_control));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.LANE_CONTROL,
                guidanceSettings.switches.get(R.id.lane_control));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.SCHOOL,
                guidanceSettings.switches.get(R.id.school));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.ACCIDENT,
                guidanceSettings.switches.get(R.id.accident));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.RECONSTRUCTION,
                guidanceSettings.switches.get(R.id.reconstruction));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.DANGER,
                guidanceSettings.switches.get(R.id.danger));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.OVERTAKING_DANGER,
                guidanceSettings.switches.get(R.id.overtaking_danger));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.PEDESTRIAN_DANGER,
                guidanceSettings.switches.get(R.id.pedestrian_danger));
        guide.setRoadEventTagAnnotated(
                AnnotatedEventTag.CROSS_ROAD_DANGER,
                guidanceSettings.switches.get(R.id.cross_road_danger));

        routeView.setLaneSignsEnabled(guidanceSettings.switches.get(R.id.show_lanes));
        routeView.setEventsEnabled(guidanceSettings.switches.get(R.id.show_events));
        routeView.setManeuversEnabled(guidanceSettings.switches.get(R.id.show_maneuvers));
        routeView.setTrafficLightsAndBarriersEnabled(
                guidanceSettings.switches.get(R.id.show_traffic_lights));

        setDirectionSignEnabled(guidanceSettings.switches.get(R.id.show_direction_signs));
    }

    @Override
    public GuidanceSettings getGuidanceSettings() {
        return guidanceSettings;
    }

    @Override
    public SpeedingPolicy getSpeedingPolicy() {
        return guide.getSpeedingPolicy();
    }

    // Actions

    public void onBugTap(View view) {
        recordCollector.markProblem();
        showUserMessage("Problem has been marked", Level.INFO);
    }

    private static class CameraCallbackImpl implements Map.CameraCallback {
        private WeakReference<GuidanceActivity> weakSelf;
        public CameraCallbackImpl(WeakReference<GuidanceActivity> weakSelf) {
            this.weakSelf = weakSelf;
        }

        @Override
        public void onMoveFinished(boolean completed) {
            if (completed) {
                GuidanceActivity self = weakSelf.get();
                if (self != null) {
                    self.followArrow();
                }
            }
        }
    }

    public void onFindMeTap(View view) {
        moveToCurrentPosition(
                /* animated= */true, new CameraCallbackImpl(new WeakReference<>(this)));
    }

    public void onSetVehicleOptions() {
        VehicleOptionsProvider.poll(
            this,
            selectedVehicleOptions,
            (vehicleOptions) -> {
                selectedVehicleOptions = vehicleOptions;
                guide.setVehicleOptions(vehicleOptions);
            });
    }

    public void onSettingsTap(@NonNull View view) {
        showFragment(SETTINGS_FRAGMENT_TAG);
    }

    public void onMenuTap(@NonNull View view) {
        showFragment(MENU_FRAGMENT_TAG);
    }

    public void onDatacollectTap(@NonNull View view) {
        showFragment(DATACOLLECT_FRAGMENT_TAG);
    }

    private void showFragment(@NonNull String fragmentTag) {
        final String backStackName = "GUIDANCE_BACK_STACK";

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);

        fragmentManager.popBackStack(backStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (fragment != null) {
            return;
        }

        fragment = createFragmentByTag(fragmentTag);
        fragmentManager.beginTransaction()
                .add(R.id.submenu_container, fragment, fragmentTag)
                .addToBackStack(backStackName)
                .commit();
    }

    private Fragment createFragmentByTag(@NonNull String fragmentTag) {
        if (fragmentTag.equals(MENU_FRAGMENT_TAG)) {
            return new SimulationDockFragment();
        } else if (fragmentTag.equals(SETTINGS_FRAGMENT_TAG)) {
            return new GuidanceSettingsFragment();
        } else if (fragmentTag.equals(DATACOLLECT_FRAGMENT_TAG)) {
            return new DatacollectRequestsFragment();
        }
        throw new RuntimeException(String.format("Unknown tag: %s", fragmentTag));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        guide.setSpeakerLanguage(guidanceSettings.speakerLanguage);
        mockSpeaker.setLanguage(guidanceSettings.speakerLanguage);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == TEST_CASE_REQUEST_CODE) {
            TestCase testCase = data.getParcelableExtra("testCase");
            if (testCase != null) {
                runTestCase(testCase);
            }
        }
        else if (requestCode == FETCH_REPORT_REQUEST_CODE) {
            byte[] reportBytes = ReportFetchActivity.getDownloadedReport();
            ReportData report = DirectionsFactory.getInstance().createReportFactory().createReportData(reportBytes);
            runRecordedSimulation(report);
        }

    }

    @Override
    public void onLaneSignUpdated() {
        if (guide == null || guide.getDisplayedAnnotations() == null) {
            routeView.setLaneSign(null);
            return;
        }
        routeView.setLaneSign(guide.getDisplayedAnnotations().getLaneSign());
    }

    @Override
    public void onUpcomingEventsUpdated() {
        if (guide.getDisplayedAnnotations() != null) {
            for (UpcomingEvent upcomingEvent : guide.getDisplayedAnnotations().getUpcomingEvents()) {
                if (upcomingEvent.getEvent().getTags().contains(EventTag.SPEED_CONTROL)) {
                    updateNextCameraText(upcomingEvent);
                    return;
                }
            }
        }

        updateNextCameraText(null);
    }

    @Override
    public void onFasterAlternativeUpdated() {
        FasterAlternative alternative = guide.getFasterAlternative();
        TextView fasterAlternativeText = (TextView) findViewById(R.id.faster_alternative_text);
        if (alternative != null) {
            fasterAlternativeText.setText("Faster: -" + alternative.getTimeDifference().getText());
            fasterAlternativeText.setVisibility(View.VISIBLE);
            if (fasterAlternativeRouteViewer != null) {
                fasterAlternativeRouteViewer.setRoute(null);
            }
            fasterAlternativeRouteViewer = new SimpleRouteView(mapObjects, new SimpleRouteView.PolylineStyle(RED_COLOR, 5.0f));
            fasterAlternativeRouteViewer.setZIndex(FASTER_ALTERNATIVE_Z_INDEX);
            fasterAlternativeRouteViewer.setRoute(alternative.getRoute());
        } else {
            fasterAlternativeText.setVisibility(View.INVISIBLE);
            fasterAlternativeRouteViewer.setRoute(null);
        }
    }

    @Override
    public void onParkingRoutesUpdated() {
        if (guide.getParkingRoute() == null) {
            return;
        }
        DrivingRoute route = guide.getParkingRoute();
        setRoute(route);
        guide.setRoute(route);
    }

    public void onOfflineButtonTap(View view) {
        offlineButton.setTextColor(Color.GRAY);
        offlineButton.setEnabled(false);
        DrivingOptions drivingOptions = drivingOptionFromSettings();
        routerController.requestRoute(
                routePointsController.getRequestPoints(),
                drivingOptions,
                selectedVehicleOptions,
                new RouterController.RequestRouteCallback() {
                    @Override
                    public void onSuccess(DrivingRoute route) {
                        routeView.setRoute(route);
                        guide.setRoute(route);
                        setRoute(route);
                    }

                    @Override
                    public void onError(Error error) {
                        updateOfflineButton(routeView.getRoute());
                        showUserMessage(error.toString(), Level.WARNING);
                    }
                });
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
            RouteView view = new RouteView(mapview.getMap(), null, null);
            view.setJamsEnabled(false);
            view.setEventsEnabled(false);
            view.setManeuversEnabled(false);
            view.setTrafficLightsAndBarriersEnabled(false);
            view.setZIndex(ALTERNATIVE_Z_INDEX - (float)i);
            view.setTapListener(this);
            view.setRoute(alternative);
            alternativesView.add(view);
        }
    }

    @Override
    public void onRoutePositionUpdated() {
        PolylinePosition routePosition = guide.getRoutePosition();
        if (routePosition != null && routeView != null) {
            routeView.setPosition(routePosition);
            updateTollRouteLabel(routeView.getRoute());
            updateRuggedRouteLabel(routeView.getRoute());
            updateTimeLabel(routeView.getRoute());
            updateDistanceLabel(routeView.getRoute());
        }

        List<DrivingRoute> alternatives = guide.getAlternatives();
        assert alternatives.size() == alternativesView.size();
        for (int i = 0; i < alternatives.size(); ++i) {
            alternativesView.get(i).setPosition(alternatives.get(i).getPosition());
        }

        if (updateAlternativesTimes) {
            updateAlternativesTimes = false;
            updateAlternativeTimeLabels();
        }
    }

    @Override
    public void onAlternativesTimeDifferenceUpdated() {
        updateAlternativesTimes = true;
    }

    @Override
    public boolean onMapObjectTap(MapObject mapObject, Point point) {
        if (routeView.getRoutePolyline().equals(mapObject)) {
            return true;
        }
        for (RouteView alternative : alternativesView) {
            if (alternative.getRoutePolyline().equals(mapObject)) {
                setRoute(alternative.getRoute());
                guide.setRoute(alternative.getRoute());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStandingStatusUpdated() {
        StandingStatus status = guide.getStandingStatus();
        isStandingDetected = status == StandingStatus.STANDING;
    }

    @Override
    public void onFreeDriveRouteUpdated() {
        freeDriveRouteViewer.setRoute(guide.getFreeDriveRoute());
    }

    @Override
    public void onDirectionSignUpdated() {
        updateDirectionSign();
    }

    private void updateDirectionSign() {
        if (guide == null || guide.getDisplayedAnnotations() == null ||
                guide.getDisplayedAnnotations().getDirectionSign() == null) {
            setDirectionSignBitmap(null);
            return;
        }
        DirectionSign directionSign = guide.getDisplayedAnnotations().getDirectionSign();
        Bitmap directionSignBitmap = DirectionSignBitmapFactory.createDirectionSignBitmap(directionSign);
        setDirectionSignBitmap(directionSignBitmap);
    }

    private void setDirectionSignBitmap(Bitmap bitmap) {
        directionSignImage.setImageBitmap(bitmap);
    }

    private void setDirectionSignEnabled(boolean enabled) {
        directionSignImage.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        routeView.setDirectionSignsEnabled(enabled);
    }

    private String toString(PerformanceMonitor.MetricTag tag) {
        switch (tag) {
            case LOCATION_PROCESSING_TIME:
                return "Location processing time";
            case EMIT_FRAME_DURATION:
                return "Emit frame duration";
        }
        throw new IllegalArgumentException("Unknown tag");
    }

    public void onStatisticsButtonTap(View view) {
        Intent intent = new Intent(this, GuidanceStatisticsActivity.class);

        ArrayList<Float> percents = new ArrayList<>();
        percents.add(0.5f);
        percents.add(0.8f);
        percents.add(0.9f);
        percents.add(0.99f);
        percents.add(1.0f);
        intent.putExtra(GuidanceStatisticsActivity.PERCENTS_EXTRA, percents);

        HashMap<String, List<Float>> quantiles = new HashMap<>();
        for (PerformanceMonitor monitor : performanceMonitors) {
            quantiles.put(toString(monitor.tag()), monitor.quantiles(percents));
        }

        intent.putExtra(GuidanceStatisticsActivity.QUANTILES_EXTRA, quantiles);

        startActivity(intent);
    }

    // EventListener
    @Override
    public void onEvent(@NonNull String event, @NonNull java.util.Map<String, String> data) {
        // noop
    }

    // Unused callbacks

    @Override
    public void onMapTap(Map map, Point point) {

    }

    @Override
    public void onFasterAlternativeAnnotated() {

    }

    @Override
    public void onSpeedLimitExceeded() {

    }

    @Override
    public void onManeuverAnnotated() {

    }

    @Override
    public void onLastViaPositionChanged() {

    }

    private DrivingOptions drivingOptionFromSettings() {
        DrivingOptions drivingOptions = new DrivingOptions();

        drivingOptions.setAvoidTolls(guidanceSettings.switches.get(R.id.guidance_avoid_tolls));
        drivingOptions.setAvoidUnpaved(guidanceSettings.switches.get(R.id.guidance_avoid_unpaved));
        drivingOptions.setAvoidPoorConditions(guidanceSettings.switches.get(R.id.guidance_avoid_poor_conditions));
        drivingOptions.setAnnotationLanguage(guidanceSettings.speakerLanguage);

        return drivingOptions;
    }
}
