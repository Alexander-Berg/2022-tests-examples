package com.yandex.maps.testapp.directions_navigation;

import android.os.Bundle;
import android.view.View;
import android.widget.ToggleButton;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.directions.navigation_layer.RouteView;
import com.yandex.mapkit.directions.navigation_layer.RoutesSource;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.guidance_camera.Camera;
import com.yandex.mapkit.guidance_camera.CameraMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.InputListener;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.TestAppActivity;

import java.util.logging.Logger;

import androidx.annotation.Nullable;

public class MultiNavigationActivity extends TestAppActivity implements InputListener, SimulationController.Listener, RecordedSimulationController.Listener {
    private NavigationController navigationController;
    private NavigationLayerController freeLayerController, overviewLayerController, followingLayerController;
    private MapView freeMapView, overviewMapView, followingMapView;
    private ToggleButton simulationToggle;
    private static Logger LOGGER = Logger.getLogger("yandex.maps");
    private static final Point CAMERA_TARGET = new Point(55.753215, 37.622504);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.multiple_directions_navigation);
        simulationToggle = findViewById(R.id.switch_simulation);
        freeMapView = findViewById(R.id.mapview_free);
        overviewMapView = findViewById(R.id.mapview_overview);
        followingMapView = findViewById(R.id.mapview_following);

        freeMapView.getMap().addInputListener(this);
        overviewMapView.getMap().addInputListener(this);
        followingMapView.getMap().addInputListener(this);

        CameraPosition initCameraPosition = new CameraPosition(CAMERA_TARGET, 15.0f, 0.0f, 0.0f);
        freeMapView.getMap().move(initCameraPosition);
        overviewMapView.getMap().move(initCameraPosition);
        followingMapView.getMap().move(initCameraPosition);

        initNavigation();
    }

    @Override
    protected void onStopImpl() {
        freeMapView.onStop();
        overviewMapView.onStop();
        followingMapView.onStop();
    }

    @Override
    protected void onStartImpl() {
        ((MapView)findViewById(R.id.mapview_free)).onStart();
        ((MapView)findViewById(R.id.mapview_overview)).onStart();
        ((MapView)findViewById(R.id.mapview_following)).onStart();
    }

    private NavigationLayerController createNavigationLayerController(
            MapView mapView, CameraMode cameraMode) {
        NavigationLayerController controller = new NavigationLayerController(
                this,
                navigationController,
                mapView.getMapWindow(),
                null,
                false,
                false,
                false,
                true,
                NavigationLayerController.JamsMode.ENABLED_FOR_CURRENT_ROUTE,
                false,
                null);

        Camera camera = controller.getNavigationLayer().getCamera();

        camera.setSwitchModesAutomatically(false);
        camera.setCameraMode(cameraMode, null);

        return controller;
    }

    private void initNavigation() {
        remove(freeLayerController);
        remove(overviewLayerController);
        remove(followingLayerController);
        if (navigationController != null)
            navigationController.suspend();

        navigationController = new NavigationController(
                null,
                freeMapView.getMap().getMapObjects().addCollection(),
                this,
                this,
                getApplicationContext());

        freeLayerController = createNavigationLayerController(freeMapView, CameraMode.FREE);
        overviewLayerController = createNavigationLayerController(overviewMapView, CameraMode.OVERVIEW);
        followingLayerController = createNavigationLayerController(followingMapView, CameraMode.FOLLOWING);
        followingMapView.getMap().setZoomGesturesEnabled(false);
        followingMapView.getMap().setScrollGesturesEnabled(false);
        followingMapView.getMap().setTiltGesturesEnabled(false);
        followingMapView.getMap().setRotateGesturesEnabled(false);
    }

    RoutesSource routesSource() {
        RoutesSource source = freeLayerController.routesSource();
        assert source == overviewLayerController.routesSource();
        assert source == followingLayerController.routesSource();
        return source;
    }

    private void remove(@Nullable NavigationLayerController controller) {
        if (controller != null)
            controller.removeFromMap();
    }

    @Override
    public void onMapTap(Map map, Point point) {
    }

    @Override
    public void onMapLongTap(Map map, Point point) {
        navigationController.addRoutePoint(point, routesSource());
    }

    public void startRouteFromFreeView(View view) {
        start(freeLayerController.getCurrentRoute());
    }

    public void startRouteFromOverviewView(View view) {
        start(overviewLayerController.getCurrentRoute());
    }

    public void startRouteFromFollowingView(View view) {
        start(followingLayerController.getCurrentRoute());
    }

    private void start(@Nullable RouteView routeView) {
        if (routesSource() == RoutesSource.NAVIGATION) {
            if (routeView != null && routeView.isValid()) {
                navigationController.startGuidance(routeView.getRoute());
                if (simulationToggle.isChecked()) {
                    navigationController.startSimulation();
                }
            }
        }
    }

    public void switchSimulation(View view) {
        if (simulationToggle.isChecked()) {
            if (routesSource() == RoutesSource.GUIDANCE) {
                navigationController.startSimulation();
            }
        } else {
            navigationController.stopSimulation();
        }
    }

    private void onFindMeClick(MapView mapView) {
        Location currentLocation = navigationController.getNavigation().getGuidance().getLocation();
        if (currentLocation == null)
            return;

        CameraPosition cameraPosition = mapView.getMap().getCameraPosition();
        mapView.getMap().move(new CameraPosition(currentLocation.getPosition(), cameraPosition.getZoom(),
                        cameraPosition.getAzimuth(), cameraPosition.getTilt()),
                new Animation(Animation.Type.SMOOTH, 0.5f), null);
    }

    public void onFindMeInFreeModeClick(View view) {
        onFindMeClick(freeMapView);
    }

    public void onFindMeInOverviewModeClick(View view) {
        onFindMeClick(overviewMapView);
    }

    @Override
    public void onSimulationStarted() { }

    @Override
    public void onSimulationStopped() {
        initNavigation();
    }

    @Override
    public void onSimulationFinished() {
        initNavigation();
    }

    @Override
    public void onRecordedSimulationStarted() { }

    @Override
    public void onRecordedSimulationStopped() { }

    @Override
    public void onRecordedSimulationFinished() { }

    @Override
    public void onRecordedSimulationLocationUpdated() { }

}
