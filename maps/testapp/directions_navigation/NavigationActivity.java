package com.yandex.maps.testapp.directions_navigation;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.Flags;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.driving.VehicleType;
import com.yandex.mapkit.directions.navigation.Alternative;
import com.yandex.mapkit.directions.navigation.NavigationListener;
import com.yandex.mapkit.directions.navigation.GuidanceListener;
import com.yandex.mapkit.directions.navigation.PerformanceMonitor;
import com.yandex.mapkit.directions.navigation.SpeedLimitsPolicy;
import com.yandex.mapkit.directions.navigation_layer.RoutesSource;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.directions.navigation.Annotator;
import com.yandex.mapkit.directions.navigation.AnnotatorListener;
import com.yandex.mapkit.directions.navigation.Navigation;
import com.yandex.mapkit.directions.navigation.Guidance;
import com.yandex.mapkit.directions.navigation.RouteChangeReason;
import com.yandex.mapkit.directions.navigation_layer.NavigationLayer;
import com.yandex.mapkit.directions.navigation_layer.RouteView;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.road_events_layer.RoadEventsLayer;
import com.yandex.maps.recording.RecordCollector;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.common_routing.BaseNavigationActivity;
import com.yandex.mapkit.road_events.EventTag;
import com.yandex.maps.testapp.datacollect.requests.DatacollectRequestsFragment;
import com.yandex.maps.testapp.directions_navigation.test.TestCase;
import com.yandex.maps.testapp.directions_navigation.test.TestCaseLauncher;
import com.yandex.maps.testapp.driving.VehicleOptionsProvider;
import com.yandex.runtime.Error;
import com.yandex.runtime.Runtime;
import com.yandex.runtime.bindings.Serialization;
import com.yandex.runtime.i18n.I18nManager;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.recording.ReportData;

class NavigationLayerSettings {
    boolean autoCamera = true;
    boolean autoRotation = true;
    boolean autoZoom = true;
    boolean balloonsEnabled = true;
    boolean trafficLightsEnabled = true;
    boolean showPredicted = false;
    boolean showBalloonsGeometry = false;
    float zoomOffset = 0.f;

    NavigationLayerController.JamsMode jamsMode = NavigationLayerController.JamsMode.ENABLED_FOR_CURRENT_ROUTE;
}

class NavigationSettings {
    AnnotationLanguage annotationLanguage;
    double speedLimitsRatio;

    VehicleOptions vehicleOptions;

    boolean avoidTolls;
    boolean avoidUnpaved;
    boolean avoidPoorConditions;

    boolean alternativesEnabled;
    boolean backgroundWorkEnabled;
    boolean muted;

    int annotatedEvents;
    int annotatedRoadEvents;

    boolean simulationEnabled;
    boolean fillSimulationSpeedEnabled;

    VisibilitySetting arrivalPoints;
    VisibilitySetting standingSegments;
    VisibilitySetting roadObjects;

    NavigationSettings(
            Navigation navigation,
            AnnotationLanguage annotationLanguage,
            VisibilitySetting arrivalPoints,
            VisibilitySetting standingSegments,
            VisibilitySetting roadObjects) {
        this.annotationLanguage = annotationLanguage;
        this.arrivalPoints = arrivalPoints;
        this.standingSegments = standingSegments;
        this.roadObjects = roadObjects;

        backgroundWorkEnabled = true;
        muted = false;

        avoidTolls = navigation.isAvoidTolls();
        avoidUnpaved = navigation.isAvoidUnpaved();
        avoidPoorConditions = navigation.isAvoidPoorConditions();

        vehicleOptions = navigation.getVehicleOptions();

        Guidance guidance = navigation.getGuidance();
        speedLimitsRatio = guidance.getSpeedLimitTolerance();
        alternativesEnabled = guidance.isEnableAlternatives();

        Annotator annotator = guidance.getAnnotator();
        annotatedEvents = annotator.getAnnotatedEvents();
        annotatedRoadEvents = annotator.getAnnotatedRoadEvents();

        simulationEnabled = true;
        fillSimulationSpeedEnabled = true;

        arrivalPoints.setVisible(false);
        standingSegments.setVisible(false);
        roadObjects.setVisible(true);
    }
}

public class NavigationActivity extends BaseNavigationActivity implements
        InputListener,
        NavigationListener,
        GuidanceListener,
        SimulationController.Listener,
        RecordedSimulationController.Listener,
        NavigationLayerFragment.NavigationLayerDockContext,
        NavigationSettingsFragment.NavigationSettingsContext,
        NavigationActionsFragment.NavigationActionsDockContext {
    private static int ROUTES_OVERVIEW_INDENT_DP = 20;
    private static int TOP_BUTTONS_SIZE_DP = 35;
    private static int BOTTOM_BUTTONS_SIZE_DP = 50;
    private static int RIGHT_BUTTONS_SIZE_DP = 40;
    private static int MAX_SIMULATION_SPEED = 144; // km/h;
    private static int MIN_SIMULATION_SPEED = 0; // km/h
    private static double SPEED_CONVERSION_COEFFICIENT = 3.6;
    private static double SIMULATION_SPEED_INCREMENT = 10; // km/h
    private static int MAX_RECORDED_SIMULATION_CLOCK_RATE = 8;
    private static int MIN_RECORDED_SIMULATION_CLOCK_RATE = 1;
    private static int RECORDED_SIMULATION_CLOCK_RATE_INCREMENT = 1;

    // Activities request codes
    private static final int ROAD_EVENTS_SETTINGS_REQUEST_ID = 0;
    private static final int TEST_CASES_REQUEST_ID = 1;
    private static final int ANNOTATED_EVENTS_SETTINGS_REQUEST_ID = 2;
    private static final int MAPKITSIM_REQUEST_ID = 3;


    private static final String NAVIGATION_LAYER_FRAGMENT_TAG = "NAVIGATION_LAYER_FRAGMENT_TAG";
    private static final String NAVIGATION_SETTINGS_FRAGMENT_TAG = "NAVIGATION_SETTINGS_FRAGMENT_TAG";
    private static final String NAVIGATION_ACTIONS_FRAGMENT_TAG = "NAVIGATION_ACTIONS_FRAGMENT_TAG";
    private static final String DATACOLLECT_REQUESTS_FRAGMENT_TAG = "DATACOLLECT_REQUESTS_FRAGMENT_TAG";

    private NavigationLayerSettings navigationLayerSettings;
    private NavigationSettings navigationSettings;
    private StandingSegmentsController standingSegmentsController;
    private ArrivalPointsView arrivalPointsView;
    private RoadObjectsView roadObjectsView;

    private static Logger LOGGER = Logger.getLogger("yandex.maps");

    private static final String SPEED_TEXT_TEMPLATE = "Simulation speed: %.0f km/h";
    private static final String CLOCK_RATE_TEXT_TEMPLATE = "Clock rate: %d x";

    private static final String DIRECTIONS_NAVIGATION_STORAGE = "directions_navigation";
    private static final String SERIALIZED_NAVIGATION_STRING = "serialized_navigation";

    // Use Unicode code points so that the source code contains only ASCII encoding
    private static final String FLAG_BLOCKED = new String(new int[] {0x26d4},0, 1); // ⛔
    private static final String FLAG_BUILT_OFFLINE = new String(new int[] {0x2708, 0xfe0f}, 0, 2); // ✈️
    private static final String FLAG_CROSSES_BORDERS = new String(new int[] {0x1f6c3}, 0, 1); //  🛃
    private static final String FLAG_FOR_PARKING = new String(new int[] {0x1f17f, 0xfe0f}, 0, 2); // 🅿️
    private static final String FLAG_HAS_FERRIES = new String(new int[] {0x26f4, 0xfe0f}, 0, 2); // ⛴️
    private static final String FLAG_HAS_FORD_CROSSING  = new String(new int[] {0x1f3ca}, 0, 1); // 🏊
    private static final String FLAG_HAS_ROADS_IN_POOR_CONDITION = new String(new int[] {0x26a0, 0xfe0f}, 0, 2); // ⚠️
    private static final String FLAG_HAS_UNPAVED_ROADS = new String(new int[] {0x1f69c}, 0, 1); // 🚜
    private static final String FLAG_HAS_TOLLS = new String(new int[] {0x1f4b0}, 0, 1); // 💰
    private static final String FLAG_HAS_VEHICLE_RESTRICTIONS = new String(new int[] {0x1f69b}, 0, 1); // 🚛
    private static final String FLAG_PREDICTED = new String(new int[] {0x1f52e}, 0, 1); // 🔮
    private static final String FLAG_REQUIRES_ACCESS_PASS = new String(new int[] {0x1f510}, 0, 1); // 🔐

    private MapObjectCollection mapObjectCollection;
    private NavigationController navigationController;
    private Navigation navigation;
    private Guidance guidance;
    private NavigationLayerController navigationLayerController;
    private RoadEventsLayer roadEventsLayer;
    private Button startButton;
    private Button stopButton;
    private TextView currentSpeed;
    private TextView speedLimit;
    private TextView routesSource;
    private TextView routeTimeLabel;
    private TextView routeFlagsLabel;
    private TextView vehicleTypeLabel;
    private TextView roadNameLabel;
    private TextView recordedSimulationTimeLabel;

    private Button increaseSpeedButton;
    private Button decreaseSpeedButton;
    private TextView simulationSpeedText;

    private Annotator annotator;
    private MockSpeaker speaker;
    private Boolean showRoadEventsOnRoute;
    private Boolean showRoadEventsOutsideRoute;
    private ArrayList<EventTag> visibleRoadEventInLayerTags;
    private ArrayList<EventTag> visibleRoadEventOnRouteTags;

    private TestCaseLauncher testCaseLauncher;

    private List<PerformanceMonitor> monitors;
    private RecordCollector recordCollector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.directions_navigation);
        super.onCreate(savedInstanceState);

        navigationLayerSettings = new NavigationLayerSettings();

        freeModeButton = findViewById(R.id.camera_enable_free_mode);
        overviewModeButton = findViewById(R.id.camera_enable_overview_mode);
        followingModeButton = findViewById(R.id.camera_enable_following_mode);
        startButton = findViewById(R.id.start_button);
        stopButton = findViewById(R.id.stop_button);
        currentSpeed = findViewById(R.id.current_speed);
        speedLimit = findViewById(R.id.speed_limit);
        routesSource = findViewById(R.id.routes_source);
        routeTimeLabel = findViewById(R.id.route_time);
        routeFlagsLabel = findViewById(R.id.route_flags);
        vehicleTypeLabel = findViewById(R.id.vehicle_type);
        roadNameLabel = findViewById(R.id.road_name);
        recordedSimulationTimeLabel = findViewById(R.id.recorded_simulation_time_label);

        increaseSpeedButton = findViewById(R.id.increase_speed_button);
        decreaseSpeedButton = findViewById(R.id.decrease_speed_button);
        simulationSpeedText = findViewById(R.id.speed_label);

        showRoadEventsOnRoute = true;
        showRoadEventsOutsideRoute = false;
        visibleRoadEventInLayerTags = new ArrayList<>(Arrays.asList(EventTag.values()));
        visibleRoadEventOnRouteTags = new ArrayList<>(Arrays.asList(EventTag.values()));

        mapview.getMap().addInputListener(this);

        routesOverviewIndentDp = ROUTES_OVERVIEW_INDENT_DP;
        topButtonsSizeDp = TOP_BUTTONS_SIZE_DP;
        bottomButtonsSizeDp = BOTTOM_BUTTONS_SIZE_DP;
        rightButtonsSizeDp = RIGHT_BUTTONS_SIZE_DP;
        userYPos = 0.7f;

        mapObjectCollection = mapview.getMap().getMapObjects().addCollection();

        recordCollector = RecordingFactory.getInstance().recordCollector();

        initNavigation(null);
    }

    @Override
    protected void onDestroy() {
        stopSimulationAndGuidance();
        navigationController.suspend();

        navigation.removeListener(this);
        guidance.removeListener(this);

        MapKitFactory.getInstance().resetLocationManagerToDefault();

        if (navigationSettings.backgroundWorkEnabled)
            stopBackgroundService();

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (navigationSettings.backgroundWorkEnabled) {
            startBackgroundService();
        } else {
            navigationController.suspend();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (navigationSettings.backgroundWorkEnabled) {
            stopBackgroundService();
        } else {
            navigationController.resume();
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

    @Override
    public void onMapTap(Map map, Point point) {
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        navigationController.addRoutePoint(point, navigationLayerController.routesSource());
    }

    public void onStartClicked(View view) {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        switch (navigationLayerController.routesSource()) {
        case NAVIGATION:
            RouteView currentRoute = navigationLayerController.getCurrentRoute();
            if (currentRoute == null || !currentRoute.isValid())
                return;
            navigationController.startGuidance(currentRoute.getRoute());
            if (navigationSettings.simulationEnabled && !navigationController.isSimulationActive()) {
                startSimulation();
            }
            break;
        case GUIDANCE:
            navigationController.startGuidance(null);
            break;
        }
    }

    public void onStopClicked(View view) {
        switch (navigationLayerController.routesSource()) {
        case NAVIGATION:
            if (!navigationController.isRecordedSimulationActive()) {
                navigationController.resetRoutes();
            }
            break;
        case GUIDANCE:
            stopSimulationAndGuidance();
            break;
        }
    }

    @Override
    public void onParkingClicked() {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        if (navigationLayerController.routesSource() == RoutesSource.GUIDANCE) {
            navigationController.requestParkingRoutes();
        }
    }

    @Override
    public void onAlternativesClicked() {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        if (navigationLayerController.routesSource() == RoutesSource.GUIDANCE) {
            navigationController.requestAlternatives();
        }
    }

    @Override
    public void onSimulationEnabledUpdated() {
        if (navigationSettings.simulationEnabled) {
            if (navigationLayerController.routesSource() == RoutesSource.GUIDANCE) {
                if (guidance.getCurrentRoute() != null) {
                    startSimulation();
                }
            }
        } else {
            stopSimulationAndGuidance();
        }
    }

    @Override
    public void onFillSimulationSpeedUpdated() {
        navigationController.setSimulationLocationSpeedProviding(navigationSettings.fillSimulationSpeedEnabled);
    }

    @Override
    public void onSelectMapkitsim() {
        if (AuthUtil.getCurrentAccount() == null) {
            Utils.showMessage(
                    getApplicationContext(),
                    "You should login to download a report",
                    Level.WARNING,
                    LOGGER);
            return;
        }
        Intent intent = new Intent(this, ReportFetchActivity.class);
        startActivityForResult(intent, MAPKITSIM_REQUEST_ID);

    }

    @Override
    public void onSelectTestCase() {
        Intent intent = new Intent(this, TestCasesActivity.class);
        intent.putExtra("language", navigationSettings.annotationLanguage);
        intent.putExtra("vehicle_options", Serialization.serializeToBytes(new VehicleOptions()));
        startActivityForResult(intent, TEST_CASES_REQUEST_ID);
    }

    @Override
    public void onSerializeClicked() {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        byte[] serializedNavigation = navigationController.serializeNavigation();
        if (serializedNavigation == null || serializedNavigation.length == 0) {
            String message = "Failed to serialize navigation";
            Utils.showMessage(getApplicationContext(), message, Level.WARNING, LOGGER);
            return;
        }
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                DIRECTIONS_NAVIGATION_STORAGE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        String serializedString = Base64.encodeToString(serializedNavigation, Base64.DEFAULT);
        editor.putString(SERIALIZED_NAVIGATION_STRING, serializedString);
        editor.commit();
        Toast.makeText(getApplicationContext(), "Serialized", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeserializeClicked() {
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                DIRECTIONS_NAVIGATION_STORAGE, Context.MODE_PRIVATE);
        String serializedString = sharedPref.getString(SERIALIZED_NAVIGATION_STRING, null);
        if (serializedString == null) {
            String message = "Saved serialization not found";
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] serializedNavigation = Base64.decode(serializedString, Base64.DEFAULT);
        if (serializedNavigation.length == 0) {
            String message = "Serialization is empty";
            Utils.showMessage(getApplicationContext(), message, Level.WARNING, LOGGER);
            return;
        }
        initNavigation(serializedNavigation);
    }

    @Override
    public NavigationLayerSettings getNavigationLayerSettings() {
        return navigationLayerSettings;
    }

    @Override
    public void onAutoCameraUpdated() {
        camera.setSwitchModesAutomatically(navigationLayerSettings.autoCamera);
    }

    @Override
    public void onAutoRotationUpdated() {
        camera.setAutoRotation(navigationLayerSettings.autoRotation, null);
    }

    @Override
    public void onAutoZoomUpdated() {
        camera.setAutoZoom(navigationLayerSettings.autoZoom, null);
    }

    public void onBalloonsEnabledUpdated() {
        navigationLayerController.setShowBalloons(navigationLayerSettings.balloonsEnabled);
    }

    @Override
    public void onTrafficLightsEnabledUpdated() {
        navigationLayerController.setShowTrafficLights(navigationLayerSettings.trafficLightsEnabled);
    }

    @Override
    public void onShowBalloonsGeometryUpdated() {
        navigationLayerController.getNavigationLayer().setShowBalloonsGeometry(navigationLayerSettings.showBalloonsGeometry);
    }

    @Override
    public void onZoomOffsetUpdated() {
        camera.setFollowingModeZoomOffset(navigationLayerSettings.zoomOffset, null);
    }

    @Override
    public void onJamsModeUpdated() {
        navigationLayerController.setJamsMode(navigationLayerSettings.jamsMode);
    }

    @Override
    public void onShowPredictedUpdated() {
        navigationLayerController.setShowPredicted(navigationLayerSettings.showPredicted);
    }

    @Override
    public void onRecreateLayerClicked() {
        initNavigationLayer();
    }

    @Override
    public void onEventSettingsClicked() {
        Intent intent = new Intent(NavigationActivity.this,
                NavigationEventsSettingsActivity.class);
        intent.putExtra(getString(R.string.extra_show_road_events_on_route), showRoadEventsOnRoute);
        intent.putExtra(getString(R.string.extra_show_road_events_outside_route), showRoadEventsOutsideRoute);
        intent.putExtra(getString(R.string.extra_road_events_in_layer_shown_tags),
                roadEventTagsToFlags(visibleRoadEventInLayerTags));
        intent.putExtra(getString(R.string.extra_road_events_on_route_shown_tags),
                roadEventTagsToFlags(visibleRoadEventOnRouteTags));
        startActivityForResult(intent, ROAD_EVENTS_SETTINGS_REQUEST_ID);
    }

    @Override
    public NavigationSettings getNavigationSettings() {
        return navigationSettings;
    }

    @Override
    public SpeedLimitsPolicy getSpeedLimitsPolicy() {
        return guidance.getSpeedLimitsPolicy();
    }

    @Override
    public void onAnnotationLanguageUpdated() {
        navigation.setAnnotationLanguage(navigationSettings.annotationLanguage);
        if (speaker != null) {
            speaker.setLanguage(navigationSettings.annotationLanguage);
        }
    }

    @Override
    public void onSetAnnotatedEventsClicked() {
        Intent intent = new Intent(this, AnnotatedEventsActivity.class);
        intent.putExtra("annotatedEvents", navigationSettings.annotatedEvents);
        intent.putExtra("annotatedRoadEvents", navigationSettings.annotatedRoadEvents);
        startActivityForResult(intent, ANNOTATED_EVENTS_SETTINGS_REQUEST_ID);
    }

    @Override
    public void onSpeedLimitsRatioUpdated() {
        guidance.setSpeedLimitTolerance(navigationSettings.speedLimitsRatio);
    }

    @Override
    public void onSetVehicleOptions() {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        VehicleOptionsProvider.poll(
                this,
                navigationSettings.vehicleOptions,
                (vehicleOptions) -> {
                    navigationSettings.vehicleOptions = vehicleOptions;
                    navigation.setVehicleOptions(vehicleOptions);
                });
    }

    @Override
    public void onAvoidTollsUpdated() {
        if (!navigationController.isRecordedSimulationActive()) {
            navigation.setAvoidTolls(navigationSettings.avoidTolls);
        }
    }

    @Override
    public void onAvoidUnpavedUpdated() {
        if (!navigationController.isRecordedSimulationActive()) {
            navigation.setAvoidUnpaved(navigationSettings.avoidUnpaved);
        }
    }

    @Override
    public void onAvoidPoorConditionsUpdated() {
        if (!navigationController.isRecordedSimulationActive()) {
            navigation.setAvoidPoorConditions(navigationSettings.avoidPoorConditions);
        }
    }

    @Override
    public void onAlternativesEnabledUpdated() {
        if (navigationController.isRecordedSimulationActive()) {
            return;
        }
        guidance.setEnableAlternatives(navigationSettings.alternativesEnabled);
    }

    @Override
    public void onMuteUpdated() {
        if (navigationSettings.muted) {
            annotator.mute();
        } else {
            annotator.unmute();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ROAD_EVENTS_SETTINGS_REQUEST_ID) {
            if (resultCode == Activity.RESULT_OK) {
                showRoadEventsOnRoute = data.getBooleanExtra(
                        getString(R.string.extra_show_road_events_on_route),
                        showRoadEventsOnRoute);
                navigationLayerController.setShowRoadEventsOnRoute(showRoadEventsOnRoute);

                boolean[] inLayerTagFlags = data.getBooleanArrayExtra(
                        getString(R.string.extra_road_events_in_layer_shown_tags));
                if (inLayerTagFlags != null) {
                    visibleRoadEventInLayerTags = flagsToRoadEventTags(inLayerTagFlags);
                }
                navigationLayerController.setVisibleRoadEventInLayerTags(visibleRoadEventInLayerTags);

                boolean[] onRouteTagFlags = data.getBooleanArrayExtra(
                        getString(R.string.extra_road_events_on_route_shown_tags));
                if (onRouteTagFlags != null) {
                    visibleRoadEventOnRouteTags = flagsToRoadEventTags(onRouteTagFlags);
                }
                navigationLayerController.setVisibleRoadEventOnRouteTags(visibleRoadEventOnRouteTags);

                showRoadEventsOutsideRoute = data.getBooleanExtra(
                        getString(R.string.extra_show_road_events_outside_route),
                        showRoadEventsOutsideRoute);
                navigationLayerController.setShowRoadEventsOutsideRoute(showRoadEventsOutsideRoute);
            }
        } else if (requestCode == TEST_CASES_REQUEST_ID && resultCode == Activity.RESULT_OK) {
            TestCase testCase = data.getParcelableExtra("testCase");
            if (testCase != null) {
                launchTestCase(testCase);
            } else {
                Utils.showMessage(getApplicationContext(), "Got NULL testcase", Level.WARNING, LOGGER);
            }
        } else if (requestCode == ANNOTATED_EVENTS_SETTINGS_REQUEST_ID && resultCode == Activity.RESULT_OK) {
            int annotatedEvents = data.getIntExtra("annotatedEvents", -1);
            int annotatedRoadEvents = data.getIntExtra("annotatedRoadEvents", -1);
            if (annotatedEvents == -1 || annotatedRoadEvents == -1) {
                LOGGER.warning("Can't set annotated events");
                return;
            }
            navigationSettings.annotatedEvents = annotatedEvents;
            navigationSettings.annotatedRoadEvents = annotatedRoadEvents;
            annotator.setAnnotatedEvents(annotatedEvents);
            annotator.setAnnotatedRoadEvents(annotatedRoadEvents);
        } else if (requestCode == MAPKITSIM_REQUEST_ID && resultCode == Activity.RESULT_OK) {
            byte[] reportBytes = ReportFetchActivity.getDownloadedReport();
            ReportData report = DirectionsFactory.getInstance().createReportFactory().createReportData(reportBytes);
            startRecordedSimulation(report);
        }
    }

    public void onPerformanceClicked(View view) {
        Intent intent = new Intent(this, PerformanceActivity.class);

        final ArrayList<Float> percents = new ArrayList<Float>() {{
            add(0.5f);
            add(0.8f);
            add(0.9f);
            add(0.99f);
            add(1.0f);
        }};

        ArrayList<PerformanceMonitor.MetricTag> metricTags = new ArrayList<>();
        ArrayList<List<Float>> quantiles = new ArrayList<>();
        for(PerformanceMonitor monitor: monitors) {
            metricTags.add(monitor.tag());
            quantiles.add(monitor.quantiles(percents));
        }

        intent.putExtra(PerformanceActivity.PERCENTS_EXTRA, percents);
        intent.putExtra(PerformanceActivity.METRIC_TAGS_EXTRA, metricTags);
        intent.putExtra(PerformanceActivity.QUANTILES_EXTRA, quantiles);

        startActivity(intent);
    }

    public void onNavigationLayerMenuClicked(View view) {
        showFragment(NAVIGATION_LAYER_FRAGMENT_TAG);
    }

    public void onNavigationSettingsClicked(View view) {
        showFragment(NAVIGATION_SETTINGS_FRAGMENT_TAG);
    }

    public void onNavigationActionsClicked(View view) {
        showFragment(NAVIGATION_ACTIONS_FRAGMENT_TAG);
    }

    public void onDatacollectRequestsClicked(View view) {
        showFragment(DATACOLLECT_REQUESTS_FRAGMENT_TAG);
    }

    private void showFragment(@NonNull String fragmentTag) {
        final String backStackName = "NAVIGATION_BACK_STACK";

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(fragmentTag);

        fragmentManager.popBackStack(backStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (fragment != null) {
            return;
        }

        ViewGroup container = findViewById(R.id.submenu_container);
        container.bringToFront();
        container.invalidate();

        fragment = createFragmentByTag(fragmentTag);
        fragmentManager.beginTransaction()
                .add(R.id.submenu_container, fragment, fragmentTag)
                .addToBackStack(backStackName)
                .commit();
    }

    private Fragment createFragmentByTag(@NonNull String fragmentTag) {
        if (fragmentTag.equals(NAVIGATION_ACTIONS_FRAGMENT_TAG)) {
            return NavigationActionsFragment.newInstance(navigationController.isRecordedSimulationActive());
        } else if (fragmentTag.equals(NAVIGATION_SETTINGS_FRAGMENT_TAG)) {
            return NavigationSettingsFragment.newInstance(navigationController.isRecordedSimulationActive());
        } else if (fragmentTag.equals(NAVIGATION_LAYER_FRAGMENT_TAG)) {
            return new NavigationLayerFragment();
        } else if (fragmentTag.equals(DATACOLLECT_REQUESTS_FRAGMENT_TAG)) {
            return new DatacollectRequestsFragment();
        }
        throw new RuntimeException(String.format("Unknown tag: %s", fragmentTag));
    }

    public void onBugClick(View view) {
        recordCollector.markProblem();
        Utils.showMessage(getApplicationContext(), "Problem has been marked");
    }

    public void onFindMeClick(View view) {
        Location currentLocation = guidance.getLocation();
        if (currentLocation == null)
            return;

        CameraPosition cameraPosition = mapview.getMap().getCameraPosition();
        mapview.getMap().move(new CameraPosition(currentLocation.getPosition(), cameraPosition.getZoom(),
                cameraPosition.getAzimuth(), cameraPosition.getTilt()),
                new Animation(Animation.Type.SMOOTH, 0.5f), null);
    }

    private NavigationLayerController.UserPlacemarkType guidanceUserPlacemark() {
        Boolean guidanceIsStanding = guidance.isIsStanding();
        if (guidanceIsStanding == null || !guidanceIsStanding) {
            return NavigationLayerController.UserPlacemarkType.GENERAL;
        }
        return NavigationLayerController.UserPlacemarkType.STANDING;
    }

    private void updateRoutesSourceInfo() {
        routesSource.setText("");
        startButton.setVisibility(View.INVISIBLE);
        stopButton.setVisibility(View.INVISIBLE);

        if (navigationLayerController == null) {
            return;
        }

        switch (navigationLayerController.routesSource()) {
            case NAVIGATION:
                if (navigationController.isRecordedSimulationActive())
                    break;
                routesSource.setText("Navigation");

                startButton.setText("Start");
                startButton.setVisibility(View.VISIBLE);

                stopButton.setText("Cancel");
                stopButton.setVisibility(View.VISIBLE);
                break;
            case GUIDANCE:
                routesSource.setText("Guidance");

                startButton.setText("Free Drive");
                startButton.setVisibility(
                        navigationController.isRecordedSimulationActive() ? View.INVISIBLE : View.VISIBLE);

                boolean needToHideStop =
                        guidance.getCurrentRoute() == null
                        && !navigationController.isSimulationActive()
                        && !navigationController.isRecordedSimulationActive();

                stopButton.setText("Stop");
                stopButton.setVisibility(needToHideStop ? View.INVISIBLE : View.VISIBLE);
                break;
        }
    }

    private void updateRouteTime() {
        if (navigationLayerController != null) {
            RouteView routeView = navigationLayerController.getCurrentRoute();
            if (routeView != null && routeView.isValid()) {
                DrivingRoute route = routeView.getRoute();
                if (route != null && !route.getMetadata().getFlags().getPredicted()) {
                    int time = (int) route.getMetadata().getWeight().getTimeWithTraffic().getValue(); // seconds
                    I18nManager i18nManager = I18nManagerFactory.getI18nManagerInstance();
                    String localizedTime = i18nManager.localizeDuration(time);
                    routeTimeLabel.setText("Route time: " + localizedTime);
                    return;
                }
            }
        }
        routeTimeLabel.setText("");
    }

    private void updateRouteFlagsLabel(DrivingRoute route) {
        if (route == null) {
            routeFlagsLabel.setVisibility(View.INVISIBLE);
            routeFlagsLabel.setText("");
            return;
        }
        Flags flags = route.getMetadata().getFlags();
        // Without futureBlocked and deadJam flags
        StringBuilder sb = new StringBuilder();
        sb.append("Flags: '");
        if (flags.getBlocked()) { sb.append(FLAG_BLOCKED); }
        if (flags.getBuiltOffline()) { sb.append(FLAG_BUILT_OFFLINE); }
        if (flags.getCrossesBorders()) { sb.append(FLAG_CROSSES_BORDERS); }
        if (flags.getForParking()) { sb.append(FLAG_FOR_PARKING); }
        if (flags.getHasFerries()) { sb.append(FLAG_HAS_FERRIES); }
        if (flags.getHasFordCrossing()) { sb.append(FLAG_HAS_FORD_CROSSING); }
        if (flags.getHasUnpavedRoads()) { sb.append(FLAG_HAS_UNPAVED_ROADS); }
        if (flags.getHasInPoorConditionRoads()) { sb.append(FLAG_HAS_ROADS_IN_POOR_CONDITION); }
        if (flags.getHasTolls()) { sb.append(FLAG_HAS_TOLLS); }
        if (flags.getHasVehicleRestrictions()) { sb.append(FLAG_HAS_VEHICLE_RESTRICTIONS); }
        if (flags.getPredicted()) { sb.append(FLAG_PREDICTED); }
        if (flags.getRequiresAccessPass()) { sb.append(FLAG_REQUIRES_ACCESS_PASS); }
        sb.append("'");
        routeFlagsLabel.setText(sb);
        routeFlagsLabel.setVisibility(View.VISIBLE);
    }

    String vehicleTypeToString(VehicleType type) {
        switch(type) {
            case DEFAULT:
                return "Default";
            case TAXI:
                return "Taxi";
            case TRUCK:
                return "Truck";
        }
        return "Unknown";
    }

    private void updateVehicleTypeLabel(DrivingRoute route) {
        if (route == null) {
            vehicleTypeLabel.setVisibility(View.INVISIBLE);
            return;
        }
        vehicleTypeLabel.setVisibility(View.VISIBLE);
        vehicleTypeLabel.setText("Vehicle Type: " + vehicleTypeToString(route.getVehicleOptions().getVehicleType()));
    }

    private void setRouteLabels(DrivingRoute route) {
        updateRouteFlagsLabel(route);
        updateVehicleTypeLabel(route);
    }

    private void initNavigation(byte[] serializedNavigation) {
        if (navigationLayerController != null) {
            navigationLayerController.removeFromMap();
            navigationLayerController = null;
        }

        if (navigationController != null) {
            stopSimulationAndGuidance();
            navigationController.suspend();
        }

        setRouteLabels(null);

        navigationController = null;
        if (serializedNavigation != null) {
            try {
                navigationController = new NavigationController(serializedNavigation, mapObjectCollection, this, this, getApplicationContext());
            } catch (RuntimeException e) {
                Utils.showMessage(getApplicationContext(), e.getMessage(), Level.WARNING, LOGGER);
            }
        }
        if (navigationController == null) {
            navigationController = new NavigationController(null, mapObjectCollection, this, this, getApplicationContext());
        }

        standingSegmentsController = new StandingSegmentsController(mapObjectCollection);
        arrivalPointsView = new ArrivalPointsView(mapObjectCollection);
        roadObjectsView = new RoadObjectsView(mapObjectCollection, new ImageProviderFactory() {
            @NonNull
            public ImageProvider fromResource(int resourceId) {
                return ImageProvider.fromResource(getApplicationContext(), resourceId);
            }
        });

        navigation = navigationController.getNavigation();
        guidance = navigation.getGuidance();

        monitors = new ArrayList<PerformanceMonitor>() {{
            add(guidance.createPerformanceMonitor(PerformanceMonitor.MetricTag.EMIT_FRAME_DURATION));
            add(guidance.createPerformanceMonitor(PerformanceMonitor.MetricTag.LOCATION_PROCESSING_TIME));
        }};

        for (PerformanceMonitor monitor: monitors)
            monitor.start();

        navigation.addListener(this);
        guidance.addListener(this);
        onLocationChanged();
        onSpeedLimitUpdated();
        onSpeedLimitStatusUpdated();

        AnnotationLanguage initialLanguage = AnnotationLanguage.RUSSIAN;

        initNavigationLayer();

        initAnnotator(initialLanguage);

        navigationSettings = new NavigationSettings(
                navigation,
                initialLanguage,
                arrivalPointsView,
                standingSegmentsController,
                roadObjectsView);

        navigationController.setSimulationLocationSpeedProviding(navigationSettings.fillSimulationSpeedEnabled);

        updateSimulationSpeedText();

        if (serializedNavigation != null && navigationSettings.simulationEnabled) {
            startSimulation();
        }
    }

    private void initNavigationLayer() {
        DrivingRoute currentDrivingRoute = null;

        if (navigationLayerController != null) {
            if (navigationLayerController.getCurrentRoute() != null) {
                currentDrivingRoute = navigationLayerController.getCurrentRoute().getRoute();
            }
            navigationLayerController.removeFromMap();
        }

        navigation.removeListener(this);
        guidance.removeListener(this);

        navigationLayerController = new NavigationLayerController(
                this,
                navigationController,
                mapview.getMapWindow(),
                roadEventsLayer,
                navigationLayerSettings.trafficLightsEnabled,
                showRoadEventsOnRoute,
                showRoadEventsOutsideRoute,
                navigationLayerSettings.balloonsEnabled,
                navigationLayerSettings.jamsMode,
                navigationLayerSettings.showPredicted,
                visibleRoadEventInLayerTags);
        roadEventsLayer = navigationLayerController.getRoadEventsLayer();

        navigation.addListener(this);
        guidance.addListener(this);

        NavigationLayer navigationLayer = navigationLayerController.getNavigationLayer();

        camera = navigationLayer.getCamera();
        camera.addListener(this);
        onCameraModeChanged();

        camera.setSwitchModesAutomatically(navigationLayerSettings.autoCamera);
        camera.setAutoZoom(navigationLayerSettings.autoZoom, null);
        camera.setAutoRotation(navigationLayerSettings.autoRotation, null);
        onMapWindowSizeChanged(mapview.getMapWindow(), mapview.getMapWindow().width(), mapview.getMapWindow().height());

        if (currentDrivingRoute != null) {
            RouteView routeView = navigationLayer.getView(currentDrivingRoute);
            if (routeView != null)
                navigationLayerController.onRouteViewTap(routeView);
        }

        updateRoutesSourceInfo();

        navigationLayerController.setUserPlacemark(guidanceUserPlacemark());
    }

    static private boolean[] roadEventTagsToFlags(ArrayList<EventTag> visibleRoadEventTags) {
        boolean[] result = new boolean[EventTag.values().length];
        Arrays.fill(result, false);

        for (EventTag tag : visibleRoadEventTags)
            result[tag.ordinal()] = true;
        return result;
    }

    static private ArrayList<EventTag> flagsToRoadEventTags(boolean[] roadEventTagFlags) {
        ArrayList<EventTag> result = new ArrayList<>();
        for (EventTag tag : EventTag.values()) {
            if (roadEventTagFlags[tag.ordinal()])
                result.add(tag);
        }
        return result;
    }

    @Override
    public void onLocationChanged() {
        Double speed = guidance.getLocation() != null ? guidance.getLocation().getSpeed() : null;
        String localizedSpeed = speed == null ?
            "none" :
            I18nManagerFactory.getI18nManagerInstance().localizeSpeed(speed);
        currentSpeed.setText("Current speed: " + localizedSpeed);

        updateRoutesSourceInfo();
        updateRouteTime();
    }

    @Override
    public void onSpeedLimitUpdated() {
        String limit = guidance.getSpeedLimit() == null ? "none" : guidance.getSpeedLimit().getText();
        speedLimit.setText("Speed limit: " + limit);
    }

    @Override
    public void onSpeedLimitStatusUpdated() {
        switch (guidance.getSpeedLimitStatus()) {
            case BELOW_LIMIT:
                currentSpeed.setTextColor(Color.BLACK);
                break;
            case STRICT_LIMIT_EXCEEDED:
                currentSpeed.setTextColor(Color.rgb(255, 127, 0));
                break;
            case TOLERANT_LIMIT_EXCEEDED:
                currentSpeed.setTextColor(Color.RED);
                break;
            default:
                throw new AssertionError("Unknown SpeedLimitStatus");
        }
    }

    public void configureFocusRect(View view) {
        focusRectController.start();
    }

    public void configureOverviewRect(View view) {
        overviewRectController.start();
    }

    private void updateSpeedButtonsAvailability() {
        if (navigationController.isRecordedSimulationActive()) {
            int clockRate = navigationController.getRecordedSimulationClockRate();
            decreaseSpeedButton.setEnabled(clockRate > MIN_RECORDED_SIMULATION_CLOCK_RATE);
            increaseSpeedButton.setEnabled(clockRate < MAX_RECORDED_SIMULATION_CLOCK_RATE);
        } else {
            long speed = Math.round(navigationController.getSimulationSpeed() * SPEED_CONVERSION_COEFFICIENT);
            decreaseSpeedButton.setEnabled(speed > MIN_SIMULATION_SPEED);
            increaseSpeedButton.setEnabled(speed < MAX_SIMULATION_SPEED);
        }

    }

    private void updateRecordedSimulationClockRate(View view) {
        int id = view.getId();
        int clockRate = navigationController.getRecordedSimulationClockRate();
        if (id == R.id.increase_speed_button) {
            clockRate = Math.min(MAX_RECORDED_SIMULATION_CLOCK_RATE,
                        clockRate + RECORDED_SIMULATION_CLOCK_RATE_INCREMENT);
        } else {
            clockRate = Math.max(MIN_RECORDED_SIMULATION_CLOCK_RATE,
                        clockRate - RECORDED_SIMULATION_CLOCK_RATE_INCREMENT);
        }
        navigationController.setRecordedSimulationClockRate(clockRate);
        updateSimulationSpeedText();
    }

    private void updateSimulationSpeed(View view) {
        int id = view.getId();
        double speed = navigationController.getSimulationSpeed() * SPEED_CONVERSION_COEFFICIENT;
        if (id == R.id.increase_speed_button) {
            speed = Math.min(MAX_SIMULATION_SPEED, Math.round(speed + SIMULATION_SPEED_INCREMENT));
        } else {
            speed = Math.max(MIN_SIMULATION_SPEED, Math.round(speed - SIMULATION_SPEED_INCREMENT));
        }
        navigationController.setSimulationSpeed(speed / SPEED_CONVERSION_COEFFICIENT);
        updateSimulationSpeedText();
    }

    public void onSimulationSpeedClicked(View view) {
        int id = view.getId();
        if (navigationController.isRecordedSimulationActive()) {
            updateRecordedSimulationClockRate(view);
        } else {
            updateSimulationSpeed(view);
        }
        updateSpeedButtonsAvailability();
    }

    @Override
    public void onSimulationStarted() {
        setSimulationSpeedControlsVisibility(View.VISIBLE);
        updateSpeedButtonsAvailability();
        updateSimulationSpeedText();
    }

    @Override
    public void onSimulationStopped() {
        setSimulationSpeedControlsVisibility(View.GONE);
    }

    @Override
    public void onSimulationFinished() {
        setSimulationSpeedControlsVisibility(View.GONE);
        stopGuidance();
    }

    @Override
    public void onRecordedSimulationStarted() {
        setSimulationSpeedControlsVisibility(View.VISIBLE);
        updateSpeedButtonsAvailability();
        updateSimulationSpeedText();
        recordedSimulationTimeLabel.setText("");
        recordedSimulationTimeLabel.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRecordedSimulationStopped() {
        setSimulationSpeedControlsVisibility(View.GONE);
        recordedSimulationTimeLabel.setVisibility(View.GONE);
    }

    @Override
    public void onRecordedSimulationFinished() {
        setSimulationSpeedControlsVisibility(View.GONE);
        recordedSimulationTimeLabel.setVisibility(View.GONE);
        stopGuidance();
    }

    @Override
    public void onRecordedSimulationLocationUpdated() {
        String timeLeft = navigationController.recordedSimulationTimeLeft();
        recordedSimulationTimeLabel.setText(String.format("RS time: %s", timeLeft == null ? "None" : timeLeft));
    }

    private void startSimulation() {
        navigationController.startSimulation();
    }

    private void startRecordedSimulation(ReportData report){
        stopSimulationAndGuidance();
        navigationController.startRecordedSimulation(report);
    }

    private void stopRecordedSimulation() {
        navigationController.stopRecordedSimulation();
    }

    private void stopSimulationAndGuidance() {
        navigationController.stopSimulation();
        stopRecordedSimulation();
        stopGuidance();
    }

    private void stopGuidance() {
        navigationController.stopGuidance();
        navigationController.resetRoutes();

        navigationSettings.alternativesEnabled = true;
        guidance.setEnableAlternatives(true);

        updateRoutesSourceInfo();

        testCaseLauncher = null;
        recordCollector.startReport();
    }

    private void updateSimulationSpeedText() {
        if (navigationController.isRecordedSimulationActive()) {
            int clockRate = navigationController.getRecordedSimulationClockRate();
            simulationSpeedText.setText(String.format(CLOCK_RATE_TEXT_TEMPLATE, clockRate));
        } else {
            simulationSpeedText.setText(String.format(
                    SPEED_TEXT_TEMPLATE,
                    navigationController.getSimulationSpeed() * SPEED_CONVERSION_COEFFICIENT));
        }
    }

    private void setSimulationSpeedControlsVisibility(int visibility) {
        decreaseSpeedButton.setVisibility(visibility);
        increaseSpeedButton.setVisibility(visibility);
        simulationSpeedText.setVisibility(visibility);
    }

    @Override
    public void onRoutesBuilt() {
        if (navigationController.isRecordedSimulationActive()) {
            if (!navigationController.navigationRoutes().isEmpty()) {
                navigationController.startGuidance(navigationController.navigationRoutes().get(0));
            } else {
                LOGGER.warning("Failed to start guidance because navigationRoutes() is empty");
            }
            return;
        }
        for (DrivingRoute route : navigationController.navigationRoutes()) {
            if (route.getMetadata().getFlags().getForParking()) {
                navigationController.startGuidance(route);
                return;
            }
        }
    }

    // Unused callbacks

    @Override
    public void onRoutesRequested(@NonNull List<RequestPoint> list) {}

    @Override
    public void onRoutesRequestError(@NonNull Error error) { }

    @Override
    public void onAlternativesRequested(@NonNull DrivingRoute currentRoute) {}

    @Override
    public void onUriResolvingRequested(@NonNull String uri) {}

    @Override
    public void onParkingRoutesRequested() {}

    @Override
    public void onResetRoutes() {}

    @Override
    public void onRouteLost() {}

    @Override
    public void onWayPointReached() {}

    @Override
    public void onStandingStatusChanged() {
        if (navigationLayerController != null) {
            navigationLayerController.setUserPlacemark(guidanceUserPlacemark());
        }
    }

    @Override
    public void onRoadNameChanged() {
        if (guidance.getRoadName() != null) {
            roadNameLabel.setText(guidance.getRoadName());
        } else {
            roadNameLabel.setText("");
        }
    }

    @Override
    public void onRouteFinished() {}

    @Override
    public void onCurrentRouteChanged(@NonNull RouteChangeReason reason) {
        setRouteLabels(guidance.getCurrentRoute());
        notifyAboutRouteChanged(standingSegmentsController);
        notifyAboutRouteChanged(arrivalPointsView);
        notifyAboutRouteChanged(roadObjectsView);
    }

    private void notifyAboutRouteChanged(@NonNull RouteListener listener) {
        if (navigationLayerController != null) {
            RouteView routeView = navigationLayerController.getCurrentRoute();
            if (routeView != null && routeView.isValid()) {
                listener.onCurrentRouteChanged(routeView.getRoute());
                return;
            }
        }
        listener.onCurrentRouteChanged(guidance.getCurrentRoute());
    }

    @Override
    public void onReturnedToRoute() {}

    @Override
    public void onAlternativesChanged() {}

    @Override
    public void onFastestAlternativeChanged() {
        TextView fasterAlternativeLabel = findViewById(R.id.faster_alternative_label);
        fasterAlternativeLabel.setVisibility(View.GONE);

        Alternative fastestAlternative = guidance.getFastestAlternative();
        if (fastestAlternative == null) {
            return;
        }

        DrivingRoute currentRoute = guidance.getCurrentRoute();
        assert currentRoute != null;

        double timeDiff = currentRoute.getRoutePosition().timeToFinish()
                - fastestAlternative.getAlternative().getRoutePosition().timeToFinish();

        I18nManager i18nManager = I18nManagerFactory.getI18nManagerInstance();
        String localizedTimeDiff = i18nManager.localizeDuration((int) timeDiff);

        fasterAlternativeLabel.setText("Faster: -" + localizedTimeDiff);
        fasterAlternativeLabel.setVisibility(View.VISIBLE);
    }

    private void showMessage(CharSequence text) {
        Toast toast =  Toast.makeText(NavigationActivity.this, text, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 400);
        toast.show();
    }

    private void initAnnotator(AnnotationLanguage annotationLanguage) {
        speaker = new MockSpeaker(
            this,
            annotationLanguage,
                phrase -> {
                    LOGGER.info("[EC] Annotator says: '" + phrase + "'");
                    showMessage(phrase);
                }
        );

        annotator = guidance.getAnnotator();
        annotator.setSpeaker(speaker);
        annotator.addListener(
            new AnnotatorListener() {
                @Override
                public void manoeuvreAnnotated() {
                    showMessage("Manoeuvre annotated");
                }

                @Override
                public void roadEventAnnotated() {
                    showMessage("Road event annotated");
                }

                @Override
                public void fasterAlternativeAnnotated() {
                    showMessage("Faster alternative annotated");
                }

                @Override
                public void speedingAnnotated() {
                    showMessage("Speeding annotated");
                }
            }
        );
    }

    private void launchTestCase(@NonNull TestCase testCase) {
        stopSimulationAndGuidance();

        testCaseLauncher = new TestCaseLauncher(
                navigation,
                navigationController.getSimulationController(),
                testCase,
                getApplicationContext(),
                new TestCaseLauncher.TestCaseLauncherListener() {
                    @Override
                    public void onTestCaseStarted() {
                        setSimulationSpeedControlsVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onTestCaseFailed() {
                        stopSimulationAndGuidance();
                    }
                });

        testCaseLauncher.launchTestCase();
    }

}
