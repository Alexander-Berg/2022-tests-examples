package com.yandex.maps.testapp.fitness_navigation;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.mapkit.transport.masstransit.WayPoint;
import com.yandex.mapkit.transport.navigation.ArrivalTime;
import com.yandex.mapkit.transport.navigation.FitnessManoeuvre;
import com.yandex.mapkit.transport.navigation.GetOffTransport;
import com.yandex.mapkit.transport.navigation.GetOnTransport;
import com.yandex.mapkit.transport.navigation.MasstransitManoeuvre;
import com.yandex.mapkit.transport.navigation.RouteManoeuvre;
import com.yandex.mapkit.transport.navigation.TransportOptions;
import com.yandex.mapkit.transport.navigation_layer.RequestPointViewListener;
import com.yandex.mapkit.transport.simulation.RecordedSimulator;
import com.yandex.mapkit.transport.simulation.RecordedSimulatorListener;
import com.yandex.mapkit.geometry.geo.PolylineUtils;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.mapkit.transport.Transport;
import com.yandex.mapkit.transport.TransportFactory;


import com.yandex.mapkit.transport.masstransit.ActionID;
import com.yandex.mapkit.transport.masstransit.LandmarkID;
import com.yandex.mapkit.transport.masstransit.Section;
import com.yandex.mapkit.transport.masstransit.TransitOptions;
import com.yandex.mapkit.transport.masstransit.Fitness;
import com.yandex.mapkit.transport.masstransit.Annotation;
import com.yandex.mapkit.transport.masstransit.Weight;
import com.yandex.mapkit.transport.navigation.Annotator;
import com.yandex.mapkit.transport.navigation.AnnotatorListener;
import com.yandex.mapkit.transport.navigation.GuidanceListener;
import com.yandex.mapkit.transport.navigation.Navigation;
import com.yandex.mapkit.transport.navigation.NavigationListener;
import com.yandex.mapkit.transport.navigation.RouteChangeReason;
import com.yandex.mapkit.transport.navigation.Type;
import com.yandex.mapkit.transport.navigation_layer.NavigationLayer;
import com.yandex.mapkit.transport.navigation_layer.RouteView;
import com.yandex.mapkit.transport.navigation_layer.RouteViewListener;
import com.yandex.maps.recording.RecordCollector;
import com.yandex.maps.recording.RecordingFactory;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.common_routing.SpeedConvertor;
import com.yandex.maps.testapp.common_routing.RoutePointsController;
import com.yandex.maps.testapp.common_routing.RouteEditor;
import com.yandex.maps.testapp.common_routing.MockSpeaker;
import com.yandex.maps.testapp.common_routing.BaseNavigationActivity;
import com.yandex.maps.testapp.auth.AuthUtil;
import com.yandex.maps.testapp.datacollect.requests.DatacollectRequestsFragment;
import com.yandex.maps.testapp.datacollect.requests.LastRequestController;
import com.yandex.maps.testapp.guidance.BackgroundService;
import com.yandex.mapkit.styling.transportnavigation.TransportNavigationStyleProvider;
import com.yandex.runtime.i18n.I18nManagerFactory;
import com.yandex.runtime.Runtime;
import com.yandex.runtime.recording.ReportData;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Date;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.TimeZone;

public class FitnessNavigationActivity extends BaseNavigationActivity implements
        NavigationListener,
        GuidanceListener,
        RouteViewListener,
        AnnotatorListener,
        RequestPointViewListener,
        InputListener {

    enum LocationSource {
        GPS,
        Simulator
    }

    enum StartButtonText {
        Idle("Start track record"),
        RecordingTrack("Stop track recording"),
        RoutesPresent("Start navigation"),
        Navigating("Stop navigation"),
        PlayingReport("Cancel report mode");

        final private String text;

        StartButtonText(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    static final int FETCH_REPORT_REQUEST_CODE = 2;
    private static int ROUTES_OVERVIEW_INDENT_DP = 20;
    private static int TOP_BUTTONS_SIZE_DP = 50;
    private static int BOTTOM_BUTTONS_SIZE_DP = 50;

    private static final String SETTINGS_FRAGMENT_TAG = "FITNESS_NAVIGATION_SETTINGS_FRAGMENT_TAG";
    private static final String DATACOLLECT_REQUESTS_FRAGMENT_TAG = "DATACOLLECT_REQUESTS_FRAGMENT_TAG";

    private static final boolean INITIAL_AUTO_CAMERA = true;

    private TextView speedInfo;
    private TextView routeInfo;
    private Button startNavigationBtn;
    private Button resetRoutesBtn;
    private Button roseBtn;
    private ToggleButton editRouteBtn;
    private Location currentLocation;
    private LocationSource locationSource = LocationSource.GPS;
    private Navigation navigation;
    private NavigationLayer navigationLayer;
    private MockSpeaker speaker;

    private Polyline simulationRoute;
    private SimulationController simulationController;

    private RecordCollector recordCollector;
    private RecordedSimulator recordedSimulator;
    private RecordedSimulatorListener recordedSimulatorListener;
    private boolean recordRunning = false;
    private LinearLayout reportLayout;
    private CheckBox useReportRerouteCheckBox;
    boolean firstRouteSet;
    private Button reportBtn;
    private EditText reportTimeEdit;
    private TextView reportEndTimeView;
    private long reportStartTime;

    private StartButtonText state = StartButtonText.Idle;
    private Type navigationType = Type.PEDESTRIAN;
    private boolean isBackgroundMode = true;

    private boolean isPaused = false;
    private boolean isSuspend = false;
    private boolean isRotate = true;
    private boolean isAutoZoom = true;
    private List<TextView> annotationsViews;
    private List<Double> annotationsPositions;

    private RoutePointsController controller;
    private RouteEditor routeEditor;

    private int transportAvoidTypes;

    private int maxAlternativeCount = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_fitness_navigation);
        super.onCreate(savedInstanceState);

        annotationsViews = new ArrayList<>();
        annotationsViews.add(findViewById(R.id.annotation1));
        annotationsViews.add(findViewById(R.id.annotation2));
        annotationsPositions = new ArrayList<>();

        startNavigationBtn = findViewById(R.id.startNavigationBtn);
        startNavigationBtn.setOnClickListener(v -> switchNavigation());

        resetRoutesBtn = findViewById(R.id.resetRoutesBtn);
        resetRoutesBtn.setOnClickListener(v -> {
            if (navigation != null) {
                navigation.resetRoutes();
                if (state == StartButtonText.RoutesPresent) {
                    state = StartButtonText.Idle;
                    startNavigationBtn.setText(state.getText());
                }
            }
        });

        roseBtn = findViewById(R.id.findMeButton);
        speedInfo = findViewById(R.id.speedInfo);
        routeInfo = findViewById(R.id.routeInfo);
        editRouteBtn = findViewById(R.id.edit_route_btn);
        editRouteBtn.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                if (navigationLayer != null && navigationLayer.selectedRoute() != null && state != StartButtonText.Navigating) {
                    routeEditor.startEdit(simulationRoute == null ?
                            navigationLayer.selectedRoute().getRoute().getGeometry().getPoints() :
                            simulationRoute.getPoints());
                }
            } else {
                simulationRoute = routeEditor.endEdit();
            }
            blockInterface(isChecked);
        });

        freeModeButton = findViewById(R.id.camera_enable_free_mode);
        overviewModeButton = findViewById(R.id.camera_enable_overview_mode);
        followingModeButton = findViewById(R.id.camera_enable_following_mode);

        routesOverviewIndentDp = ROUTES_OVERVIEW_INDENT_DP;
        topButtonsSizeDp = TOP_BUTTONS_SIZE_DP;
        bottomButtonsSizeDp = BOTTOM_BUTTONS_SIZE_DP;

        simulationController = new SimulationController(this, mapview.getMap().getMapObjects());

        initNavigation(locationSource, null);

        routeEditor = new RouteEditor(mapview.getMap().getMapObjects());

        Button btn = findViewById(R.id.overviewRectBtn);
        btn.setOnClickListener(this::configureOverviewRect);

        btn = findViewById(R.id.focusRectBtn);
        btn.setOnClickListener(this::configureFocusRect);

        onMapWindowSizeChanged(mapview.getMapWindow(), mapview.getMapWindow().width(), mapview.getMapWindow().height());

        recordCollector = RecordingFactory.getInstance().recordCollector();

        reportLayout = findViewById(R.id.reportLayout);
        useReportRerouteCheckBox = findViewById(R.id.useReportRerouteCheckBox);
        reportBtn = findViewById(R.id.reportBtn);
        reportTimeEdit = findViewById(R.id.reportTimeEdit);
        reportEndTimeView = findViewById(R.id.reportEndTime);

        reportBtn.setOnClickListener(v ->  {onReportButtonClicked();});
        useReportRerouteCheckBox.setOnClickListener(v -> {onUseReportRerouteCheckBoxChanged();});
    }

    public LocationSource getLocationSource() {
        return locationSource;
    }

    private String toString(GetOnTransport getOnTransport) {
        String result = "get on " + getOnTransport.getLineName();
        ArrivalTime arrivalTime = getOnTransport.getArrivalTime();
        if (arrivalTime != null) {
            if (arrivalTime.getTime() != null) {
                result = result + " at " +
                        arrivalTime.getTime().getText();
            } else if (arrivalTime.getFrequency() != null) {
                result = result + " every " +
                        arrivalTime.getFrequency().getText();
            }
        }

        return result;
    }

    private String toString(GetOffTransport getOff) {
        String result = "get off at " + getOff.getStopName();
        String exitName = getOff.getExitName();
        if (exitName != null) {
            result = result + " through " + exitName;
        }
        Long arrivalTime = getOff.getArrivalTime();
        if (arrivalTime != null) {
            Date date = new Date(arrivalTime);
            DateFormat formatter = new SimpleDateFormat("HH:mm");
            formatter.setTimeZone(TimeZone.getDefault());
            result = result + " at " + formatter.format(date);
        }

        return result;
    }

    @Override
    protected void onDestroy() {
        if (navigation != null && isBackgroundMode())
            stopBackgroundService();

        removeLayer();
        removeNavigation();
        super.onDestroy();
    }

    public void initNavigation(LocationSource source, @Nullable byte[] serializedNavigation) {
        setIdleState();
        isSuspend = false;
        locationSource = source;

        switch (source) {
            case GPS:
                editRouteBtn.setEnabled(false);
                MapKitFactory.getInstance().setLocationManager(MapKitFactory.getInstance().createLocationManager());
                simulationController.stopSimulation();
                break;
            case Simulator:
                editRouteBtn.setEnabled(true);
                break;
        }

        removeLayer();
        removeNavigation();

        Transport transport = TransportFactory.getInstance();

        if (serializedNavigation != null) {
            navigation = transport.deserializeNavigation(serializedNavigation);
        } else {
            navigation = transport.createNavigation(navigationType);
            navigation.resume();
        }

        speaker = new MockSpeaker(
            this,
            AnnotationLanguage.RUSSIAN,
            new MockSpeaker.SayCallback() {
                @Override
                public void onSay(final String phrase) {
                    Logger.getLogger("yandex.maps").info("[EC] Annotator says: '" + phrase + "'");
                    Toast.makeText(FitnessNavigationActivity.this, phrase, Toast.LENGTH_LONG).show();
                }
            }
        );

        navigation.getGuidance().getAnnotator().setSpeaker(speaker);

        navigation.addListener(this);
        navigation.getGuidance().addListener(this);
        navigation.getGuidance().getAnnotator().addListener(this);
        mapview.getMap().addInputListener(this);
        controller = new RoutePointsController(mapview.getMap().getMapObjects(), navigation);

        createLayer();
    }


    int getTransportAvoidTypes() { return transportAvoidTypes;}
    void setTransportAvoidTypes(int avoidTypes) { transportAvoidTypes = avoidTypes;}

    int getMaxAlternativeCount() { return maxAlternativeCount;}
    void setMaxAlternativeCount(int count) { maxAlternativeCount = count;}

    public void createLayer() {
        isAutoZoom = true;
        isRotate = true;
        Transport transport = TransportFactory.getInstance();
        navigationLayer = transport.createNavigationLayer(mapview.getMapWindow(), new TransportNavigationStyleProvider(this), navigation);

        if (!navigationLayer.getRoutes().isEmpty()) {
            onRouteViewsChanged();
        }

        navigationLayer.addRouteListener(this);
        navigationLayer.addRequestPointViewListener(this);

        camera = navigationLayer.getCamera();
        camera.addListener(this);
        onCameraModeChanged();
        camera.setSwitchModesAutomatically(INITIAL_AUTO_CAMERA);
        camera.setAutoZoom(isAutoZoom, null);
        camera.setAutoRotation(isRotate, null);
    }

    public void removeNavigation() {
        simulationController.stopSimulation();

        if (navigation == null) {
            return;
        }

        navigation.stopGuidance();

        controller.clear();
        controller = null;
        navigation.getGuidance().getAnnotator().setSpeaker(null);
        navigation.getGuidance().getAnnotator().removeListener(this);
        navigation.getGuidance().removeListener(this);
        navigation.removeListener(this);
        navigation = null;
        speaker.shutdown();
        speaker = null;
    }

    public void removeLayer() {
        if (navigationLayer != null) {
            if (controller != null)
                controller.clearPlacemarks();
            camera.removeListener(this);
            navigationLayer.removeRouteListener(this);
            navigationLayer.removeFromMap();
            camera = null;
            navigationLayer = null;
        }
    }

    public void switchNavigation() {
        switch (state) {
            case Idle:
                navigation.startGuidance(null);
                state = StartButtonText.RecordingTrack;
                break;
            case RoutesPresent:
                if (navigationLayer == null || navigationLayer.selectedRoute() == null)
                    return;
                Route selectedRoute = navigationLayer.selectedRoute().getRoute();
                if (locationSource == LocationSource.Simulator)
                    simulationController.startSimulation(simulationRoute != null ? simulationRoute : selectedRoute.getGeometry());
                navigation.startGuidance(selectedRoute);
                state = StartButtonText.Navigating;
                editRouteBtn.setEnabled(false);
                break;
            case RecordingTrack:
            case Navigating:
                navigation.stopGuidance();
                setIdleState();
                simulationRoute = null;
                if (locationSource == LocationSource.Simulator) {
                    simulationController.stopSimulation();
                    editRouteBtn.setEnabled(true);
                }
                break;
            case PlayingReport:
                stopRecordedSimulation();
                state = StartButtonText.Idle;
                break;
        }

        startNavigationBtn.setText(state.getText());
    }

    @Override
    public void onRoutingError(@NonNull com.yandex.runtime.Error error) {
        Toast.makeText(this, "onRoutingError: " + error.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRoutesBuilt() {
        simulationRoute = null;
        if (navigation.getRoutes().isEmpty()) {
            Toast.makeText(this, "Couldn't find routes ", Toast.LENGTH_SHORT).show();
            controller.clear();
        } else {
            startNavigationBtn.setText(state.getText());
            Toast.makeText(this, "onRoutesBuilt: Size = " + navigation.getRoutes().size(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResetRoutes() {
        if (navigation.getGuidance().getCurrentRoute() == null) {
            simulationRoute = null;
            controller.clear();
        }
    }

    @Override
    public void onRequestPointViewsChanged() {
    }

    @Override
    public void onRequestPointTap(int requestPointIndex) {
        navigationLayer.selectRequestPoint(requestPointIndex);
    }

    public void onLocationChanged() {
        if (isPaused)
            return;

        currentLocation = navigation.getGuidance().getLocation();
        if (currentLocation != null) {
            if (currentLocation.getHeading() != null)
                roseBtn.setRotation(360.0f - currentLocation.getHeading().floatValue());

            if (currentLocation.getSpeed() != null)
                speedInfo.setText(String.format("Speed: %.2f km/h", SpeedConvertor.toKmh(currentLocation.getSpeed())));
            else
                speedInfo.setText("Speed: n/a km/h");
        }

        Annotator annotator = navigation.getGuidance().getAnnotator();
        PolylinePosition currentPosition =
            navigation.getGuidance().getRoutePosition() != null
                ? navigation.getGuidance().getRoutePosition()
                : new PolylinePosition(0, 0);
        for (int i = 0; i < annotator.getManoeuvres().size() && i < annotationsViews.size(); ++i) {
            com.yandex.mapkit.transport.navigation.UpcomingManoeuvre annotation = annotator.getManoeuvres().get(i);

            String annotationName = "INVALID ANNOTATION";
            FitnessManoeuvre fitness = annotation.getDetails().getFitness();
            if(fitness != null) {
                annotationName =
                    fitness.getAction() != null
                        ? fitness.getLandmark() != null
                            ? fitness.getAction().name() + " + " + fitness.getLandmark().name()
                            : fitness.getAction().name()
                        : fitness.getLandmark() != null
                            ? fitness.getLandmark().name()
                            : "INVALID ANNOTATION";
            }

            RouteManoeuvre route = annotation.getDetails().getRoute();
            if(route != null) {
                annotationName = route.getAction().name();
            }

            MasstransitManoeuvre masstransit = annotation.getDetails().getMasstransit();
            if(masstransit != null) {
                if (masstransit.getGetOff() != null) {
                    annotationName = toString(masstransit.getGetOff());
                } else if (masstransit.getGetOn() != null) {
                    annotationName = toString(masstransit.getGetOn());
                }
            }

            annotationsViews.get(i).setText(String.format(
                "%s %s",
                annotationName,
                I18nManagerFactory.getI18nManagerInstance().localizeDistance((int) Math.abs(
                    Math.round(
                        navigation.getGuidance().getCurrentRoute().distanceBetweenPolylinePositions(
                            currentPosition,
                            annotation.getPosition()))))));
        }

    }

    @Override
    public void onCurrentRouteLost() {
        Log.w("Navigation", "Current route lost");
    }

    @Override
    public void onReturnedToRoute() {
        Log.w("Navigation", "Returned to route");
    }

    @Override
    public void onCurrentRouteFinished() {
        setIdleState();
        simulationRoute = null;
        if (locationSource == LocationSource.Simulator) {
            simulationController.stopSimulation();
            editRouteBtn.setEnabled(true);
        }
    }

    @Override
    public void onCurrentRouteChanged(@NonNull RouteChangeReason reason) {
        Log.w("Navigation", "Current route changed");
    }

    @Override
    public void onAnnotationsChanged() {
        for (TextView view : annotationsViews)
            view.setText("");

        Annotator annotator = navigation.getGuidance().getAnnotator();
        PolylinePosition currentPosition =
            navigation.getGuidance().getRoutePosition() != null
                ? navigation.getGuidance().getRoutePosition()
                : new PolylinePosition(0, 0);
        for (int i = 0; i < annotator.getManoeuvres().size() && i < annotationsViews.size(); ++i) {
            com.yandex.mapkit.transport.navigation.UpcomingManoeuvre annotation = annotator.getManoeuvres().get(i);
            String annotationName = "INVALID ANNOTATION";
            FitnessManoeuvre fitness = annotation.getDetails().getFitness();
            if(fitness != null) {
                annotationName =
                        fitness.getAction() != null
                                ? fitness.getLandmark() != null
                                ? fitness.getAction().name() + " + " + fitness.getLandmark().name()
                                : fitness.getAction().name()
                                : fitness.getLandmark() != null
                                ? fitness.getLandmark().name()
                                : "INVALID ANNOTATION";
            }

            RouteManoeuvre route = annotation.getDetails().getRoute();
            if(route != null) {
                annotationName = route.getAction().name();
            }

            MasstransitManoeuvre masstransit = annotation.getDetails().getMasstransit();
            if(masstransit != null) {
                if (masstransit.getGetOff() != null) {
                    annotationName = toString(masstransit.getGetOff());
                } else if (masstransit.getGetOn() != null) {
                    annotationName = toString(masstransit.getGetOn());
                }
            }
            annotationsViews.get(i).setText(String.format(
                "%s %s",
                annotationName,
                I18nManagerFactory.getI18nManagerInstance().localizeDistance((int) Math.abs(
                    Math.round(
                        navigation.getGuidance().getCurrentRoute().distanceBetweenPolylinePositions(
                            currentPosition,
                            annotation.getPosition()))))));
            annotationsPositions.remove(annotation.getPosition().getSegmentIndex() + annotation.getPosition().getSegmentPosition());
        }

        for (Double position : annotationsPositions)
            controller.removeManeuver(position);

        for (int i = 0; i < annotator.getManoeuvres().size(); ++i) {
            PolylinePosition position = annotator.getManoeuvres().get(i).getPosition();
            annotationsPositions.add(position.getSegmentIndex() + position.getSegmentPosition());
        }
    }

    @Override
    public void onReachedRequestPoint() {
        Log.w("Navigation", "Reached way point");
    }

    public void moveToLocation(View sender) {
        currentLocation = navigation.getGuidance().getLocation();
        if (currentLocation != null) {
            float zoom = mapview.getMap().getMaxZoom() - 1.0f;
            Double heading = currentLocation.getHeading() == null ? Double.valueOf(0) : currentLocation.getHeading();
            CameraPosition position = new CameraPosition(currentLocation.getPosition(), zoom, heading.floatValue(), 0f);
            mapview.getMap().move(position, new Animation(Animation.Type.LINEAR, 0.3f), b -> {});
        }
    }

    @Override
    public void onMapTap(@NonNull Map map, @NonNull Point point) {
    }

    @Override
    public void onMapLongTap(@NonNull Map map, @NonNull Point point) {
        if (state == StartButtonText.RecordingTrack)
            return;

        if (navigation.getGuidance().getCurrentRoute() != null) {
            if (locationSource == LocationSource.Simulator && navigationLayer != null && navigation.getGuidance().getLocation() != null) {
                List<RequestPoint> requestPoints = new ArrayList<>();
                requestPoints.add(new RequestPoint(navigation.getGuidance().getLocation().getPosition(), RequestPointType.WAYPOINT, null));
                requestPoints.add(new RequestPoint(point, RequestPointType.WAYPOINT, null));
                requestPoints.addAll(remainingWaypoints(false));
                simulationController.startSimulation(requestPoints, getNavigationType());
            }
        } else {
            state = StartButtonText.RoutesPresent;
            startNavigationBtn.setText(state.getText());

            controller.addPoint(new RequestPoint(point, RequestPointType.WAYPOINT, null));
            List<RequestPoint> points = controller.getRequestPoints();
            if (points.size() >= 2) {
                TransitOptions transitOptions = new TransitOptions(transportAvoidTypes, new TimeOptions());
                TransportOptions transportOptions = new TransportOptions(transitOptions, maxAlternativeCount);
                navigation.requestRoutes(points, transportOptions);
            }
        }
    }

    double getRoutePositionIndex() {
        if (navigation != null && navigation.getGuidance().getRoutePosition() != null) {
            return navigation.getGuidance().getRoutePosition().getSegmentIndex();
        }
        return 0.0;
    }

    @Override
    public void onRouteViewsChanged() {
        routeInfo.setText("");
        controller.clearPlacemarks();

        EnumMap<ActionID, Character> ActionToCharacter = new EnumMap<ActionID, Character>(ActionID.class);
        ActionToCharacter.put(ActionID.STRAIGHT, 'S');
        ActionToCharacter.put(ActionID.LEFT, 'L');
        ActionToCharacter.put(ActionID.RIGHT, 'R');
        ActionToCharacter.put(ActionID.DISMOUNT, 'D');

        EnumMap<LandmarkID, Character> LandmarkToCharacter = new EnumMap<LandmarkID, Character>(LandmarkID.class);
        LandmarkToCharacter.put(LandmarkID.CROSSWALK, 'C');
        LandmarkToCharacter.put(LandmarkID.STAIRS_TO_UNDERPASS, 'U');
        LandmarkToCharacter.put(LandmarkID.STAIRS_TO_OVERPASS, 'O');
        LandmarkToCharacter.put(LandmarkID.STAIRS_UP, 'u');
        LandmarkToCharacter.put(LandmarkID.STAIRS_DOWN, 'd');
        LandmarkToCharacter.put(LandmarkID.STAIRS, 's');

        double routePositionIndex = getRoutePositionIndex();

        if (navigationLayer.getRoutes().isEmpty())
            return;

        RouteView routeView = navigationLayer.getRoutes().get(0);

        for (Section section : routeView.getRoute().getSections()) {
            Fitness fitness = section.getMetadata().getData().getFitness();
            if (fitness != null) {
                int sectionIndex = section.getGeometry().getBegin().getSegmentIndex();
                double sectionPosition = section.getGeometry().getBegin().getSegmentPosition();

                for (Annotation annotation : fitness.getAnnotations()) {
                    int annotationIndex = annotation.getPosition().getSegmentIndex() + sectionIndex;
                    double annotationPosition = annotation.getPosition().getSegmentPosition() + sectionPosition;
                    double annotationAbsolutePosition = annotationIndex + annotationPosition;
                    Log.w("Street ", annotation.getToponym() != null ? annotation.getToponym().getToponym() : " null ");
                    if (annotationAbsolutePosition <= routePositionIndex)
                        continue;

                    PolylinePosition position = new PolylinePosition(annotationIndex, annotationPosition);
                    Point p = PolylineUtils.pointByPolylinePosition(routeView.getRoute().getGeometry(), position);

                    if (annotation.getAction() != null
                            && ActionToCharacter.containsKey(annotation.getAction())) {
                        controller.addManeuver(annotationAbsolutePosition, ActionToCharacter.get(annotation.getAction()), p);
                    }

                    if (annotation.getLandmark() != null
                            && LandmarkToCharacter.containsKey(annotation.getLandmark())) {
                        controller.addManeuver(annotationAbsolutePosition, LandmarkToCharacter.get(annotation.getLandmark()), p);
                    }
                }
            }
        }

        onRouteTap(routeView);
    }

    @Override
    public void onRouteTap(@NonNull RouteView routeView) {
        navigationLayer.selectRoute(routeView);
        Weight weight = routeView.getRoute().getMetadata().getWeight();
        routeInfo.setText(String.format("%s %s", weight.getWalkingDistance().getText(), weight.getTime().getText()));
    }

    public void onSettingsTap(View view) {
        showFragment(SETTINGS_FRAGMENT_TAG);
    }

    public void onDatacollectTap(View view) {
        showFragment(DATACOLLECT_REQUESTS_FRAGMENT_TAG);
    }

    private void showFragment(@NonNull String fragmentTag) {
        final String backStackName = "FITNESS_NAVIGATION_BACK_STACK";

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
        if (fragmentTag.equals(SETTINGS_FRAGMENT_TAG)) {
            return new FitnessSettingsFragment();
        } else if (fragmentTag.equals(DATACOLLECT_REQUESTS_FRAGMENT_TAG)) {
            return new DatacollectRequestsFragment();
        }
        throw new RuntimeException("Unknown tag");
    }

    public boolean isSuspend() {
        return isSuspend;
    }

    public void setSuspend(boolean suspend) {
        isSuspend = suspend;
        if (suspend)
            navigation.suspend();
        else
            navigation.resume();
    }

    public boolean isRotate() {
        return isRotate;
    }

    public void setAutoZoom(boolean enabled) {
        if (navigationLayer != null) {
            isAutoZoom = enabled;
            camera.setAutoZoom(enabled, null);
        }
    }

    public boolean isAutoZoom() {
        return isAutoZoom;
    }

    public void setRotate(boolean rotate) {
        if (navigationLayer != null) {
            isRotate = rotate;
            camera.setAutoRotation(rotate, null);
        }
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public NavigationLayer getLayer() {
        return navigationLayer;
    }

    private void setIdleState() {
        routeInfo.setText("");
        state = StartButtonText.Idle;
        startNavigationBtn.setText(state.getText());
        if (controller != null)
            controller.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navigation != null) {
            if (isBackgroundMode()) {
                stopBackgroundService();
                isPaused = false;
                onLocationChanged();
            } else {
                navigation.resume();
            }
        }
    }

    @Override
    protected void onPause() {
        if (navigation != null) {
            if (isBackgroundMode()) {
                startBackgroundService();
                isPaused = true;
            } else {
                navigation.suspend();
            }
        }
        super.onPause();
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

    private String getSerializedNavigationPath() {
        return getExternalFilesDir(null).getAbsolutePath() + "/serializedNavigation.txt";
    }

    public void serializeNavigation(View view) {
        if (navigation != null) {
            try {
                byte[] bytes = TransportFactory.getInstance().serializeNavigation(navigation);
                try (FileOutputStream stream = new FileOutputStream(getSerializedNavigationPath())) {
                    stream.write(bytes);
                }
            } catch (IOException e) {
                Toast.makeText(this, "Can not write to file " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean equal(Point a, Point b) {
        final double e = 0.000001;

        return Math.abs(a.getLatitude() - a.getLatitude()) < e &&
                Math.abs(a.getLongitude() - b.getLongitude()) < e;
    }

    private List<RequestPoint> remainingWaypoints(boolean isAddLastReachedWayPoint) {
        List<RequestPoint> requestPoints = new ArrayList<>();

        if (navigation != null && navigation.getGuidance().getCurrentRoute() != null) {
            boolean findLastReachedWayPoint = false;
            RequestPoint lastReachedWaypoint = navigation.getGuidance().getLastReachedRequestPoint();

            for (WayPoint waypoint : navigation.getGuidance().getCurrentRoute().getWayPoints()) {
                if (lastReachedWaypoint == null || findLastReachedWayPoint) {
                    requestPoints.add(new RequestPoint(waypoint.getPosition(), RequestPointType.WAYPOINT, null));
                } else if (equal(lastReachedWaypoint.getPoint(), waypoint.getPosition())) {
                    if (isAddLastReachedWayPoint)
                        requestPoints.add(lastReachedWaypoint);
                    findLastReachedWayPoint = true;
                }
            }
        }

        return requestPoints;
    }

    public void deserializeNavigation(View view) {
        try {
            String filename = getSerializedNavigationPath();
            byte[] bytes = new byte[(int) new File(filename).length()];
            try (FileInputStream stream = new FileInputStream(filename)) {
                stream.read(bytes);
            }

            if (locationSource == LocationSource.Simulator)
                simulationController.stopSimulation();

            initNavigation(locationSource, bytes);

            if (navigation.getGuidance().getCurrentRoute() != null) {
                state = StartButtonText.Navigating;
                if (locationSource == LocationSource.Simulator) {
                    simulationController.startSimulation(remainingWaypoints(true), navigation.getType());
                }
            } else if (!navigation.getRoutes().isEmpty()) {
                state = StartButtonText.RoutesPresent;
            }

            navigation.resume();
            startNavigationBtn.setText(state.getText());
        } catch (IOException e) {
            Toast.makeText(this, "Can not read from file " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void configureFocusRect(View view) {
        focusRectController.start();
    }

    public void configureOverviewRect(View view) {
        overviewRectController.start();
    }

    public Type getNavigationType() {
        return navigationType;
    }

    public void setNavigationType(Type navigationType) {
        this.navigationType = navigationType;
    }

    public boolean isBackgroundMode() { return isBackgroundMode; }

    public void setBackgroundMode(boolean enable) { isBackgroundMode = enable; }

    private void blockInterface(boolean blocked) {
        if (blocked)
            mapview.getMap().removeInputListener(this);
        else
            mapview.getMap().addInputListener(this);
        findViewById(R.id.findMeButton).setEnabled(!blocked);
        findViewById(R.id.openSettings).setEnabled(!blocked);
        findViewById(R.id.startNavigationBtn).setEnabled(!blocked);
        findViewById(R.id.focusRectBtn).setEnabled(!blocked);
        findViewById(R.id.overviewRectBtn).setEnabled(!blocked);
        findViewById(R.id.resetRoutesBtn).setEnabled(!blocked);
    }

    public void setSimulatorSpeed(int value) {
        simulationController.setSpeed(value);
    }

    public int getSimulatorSpeed() {
        return simulationController.getSpeed();
    }

    public void selectReport(View view)
    {
        if (AuthUtil.getCurrentAccount() == null) {
            Toast.makeText(this, "You should login to download a report", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ReportFetchActivity.class);
        startActivityForResult(intent, FETCH_REPORT_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        else if (requestCode == FETCH_REPORT_REQUEST_CODE) {
            byte[] reportBytes = ReportFetchActivity.getDownloadedReport();
            ReportData report = TransportFactory.getInstance().createReportFactory().createReportData(reportBytes);
            runRecordedSimulation(report);
        }
    }

    private void stopRecordedSimulation() {
        recordedSimulator.suspend();
        recordRunning = false;
        reportLayout.setVisibility(View.INVISIBLE);
        navigation.getGuidance().enableAutoRerouting();

        initNavigation(locationSource, null);
    }


    private void onReportButtonClicked() {
        if (state != StartButtonText.PlayingReport) return;

        if (recordRunning) {
            recordedSimulator.suspend();
            reportTimeEdit.setEnabled(true);
            reportBtn.setText("Start");
        } else {
            firstRouteSet = false;
            recordedSimulator.setTimestamp(Long.parseLong(reportTimeEdit.getText().toString()) * 1000 + reportStartTime);
            recordedSimulator.resume();
            reportTimeEdit.setEnabled(false);
            reportBtn.setText("Stop");
        }

        recordRunning = !recordRunning;
    }

    private void onUseReportRerouteCheckBoxChanged() {
        if (state != StartButtonText.PlayingReport) return;

        if (useReportRerouteCheckBox.isChecked()) {
            navigation.getGuidance().disableAutoRerouting();
        } else {
            navigation.getGuidance().enableAutoRerouting();
        }
    }


    private void runRecordedSimulation(ReportData report) {
        Toast.makeText(FitnessNavigationActivity.this, "Simulation started", Toast.LENGTH_SHORT).show();
        reportLayout.setVisibility(View.VISIBLE);

        reportStartTime = report.getStartTime();
        reportEndTimeView.setText(Long.toString((report.getEndTime() - reportStartTime) / 1000));

        recordedSimulator = TransportFactory.getInstance().createRecordedSimulator(report);
        recordedSimulatorListener = new com.yandex.mapkit.transport.simulation.RecordedSimulatorListener() {
            @Override
            public void onLocationUpdated() {
                updateRecordSimulatorTimeView();
            }

            @Override
            public void onRouteUpdated() {
                updateRecordSimulatorTimeView();

                if (!firstRouteSet) {
                    recordedSimulator.forceStartGuidanceCurrentRoute(navigation);
                    if (recordedSimulator.getRoute() != null)
                        firstRouteSet = true;
                } else if(useReportRerouteCheckBox.isChecked()) {
                    recordedSimulator.forceStartGuidanceCurrentRoute(navigation);
                }
            }

            @Override
            public void onProblemMark() {
                updateRecordSimulatorTimeView();
            }

            @Override
            public void onFinish() {
                onReportButtonClicked();
            }
        };

        reportTimeEdit.setText("0");

        recordedSimulator.subscribeForSimulatorEvents(recordedSimulatorListener);
        MapKitFactory.getInstance().setLocationManager(recordedSimulator);

        state = StartButtonText.PlayingReport;
        startNavigationBtn.setText(state.text);
    }

    public void updateRecordSimulatorTimeView() {
        if (state == StartButtonText.PlayingReport && recordRunning)
            reportTimeEdit.setText(Long.toString((recordedSimulator.getTimestamp() - reportStartTime) / 1000));
    }

    public void onBugTap(View view) {
        recordCollector.markProblem();
        Toast.makeText(FitnessNavigationActivity.this, "Problem has been marked", Toast.LENGTH_SHORT).show();
    }

}
