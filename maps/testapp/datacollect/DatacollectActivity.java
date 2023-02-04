package com.yandex.maps.testapp.datacollect;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationStatus;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.maps.testapp.R;
import com.yandex.maps.testapp.datacollect.location_subscription.LocationSubscription;
import com.yandex.maps.testapp.datacollect.location_subscription.MapKitLocationSubscription;
import com.yandex.maps.testapp.datacollect.location_subscription.SystemLocationSubscription;
import com.yandex.maps.testapp.datacollect.requests.DatacollectRequestsFragment;
import com.yandex.maps.testapp.map.MapBaseActivity;
import com.yandex.runtime.image.ImageProvider;


public class DatacollectActivity
        extends MapBaseActivity
        implements DatacollectSettings.DockContext,
        LocationListener {

    private final String requestsFragmentTag = "DATACOLLECT_REQUESTS_FRAGMENT_TAG";
    private final String settingsFragmentTag = "DATACOLLECT_SETTINGS_FRAGMENT_TAG";

    private boolean usingCustomLM = false;
    private LocationSubscription locationSubscription;

    private PlacemarkMapObject userPosition;
    private MapObjectCollection mapObjects;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.datacollect_activity_layout);
        super.onCreate(savedInstanceState);

        Button requestsButton = findViewById(R.id.datacollect_requests);
        requestsButton.setOnClickListener(this::onRequestsClicked);

        Button settingsButton = findViewById(R.id.datacollect_settings);
        settingsButton.setOnClickListener(this::onSettingsClicked);

        locationSubscription = new MapKitLocationSubscription(this);
        locationSubscription.startUpdates();

        mapObjects = mapview.getMap().getMapObjects();
        createPlacemark();
    }

    private void onRequestsClicked(@NonNull View view) {
        showFragment(requestsFragmentTag);
    }

    private void onSettingsClicked(@NonNull View view) {
        showFragment(settingsFragmentTag);
    }

    private void showFragment(@NonNull final String tag) {
        final String fragmentsBackStackName = "DATACOLLECT_FRAGMENTS_BACK_STACK";

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag(tag);

        fragmentManager.popBackStack(fragmentsBackStackName, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        if (fragment != null) {
            return;
        }

        fragment = createFragment(tag);
        fragmentManager.beginTransaction()
                .add(R.id.datacollect_submenu, fragment, tag)
                .addToBackStack(fragmentsBackStackName)
                .commit();
    }

    private Fragment createFragment(@NonNull final String tag) {
        if (tag.equals(requestsFragmentTag)) {
            return new DatacollectRequestsFragment();
        } else if (tag.equals(settingsFragmentTag)) {
            return new DatacollectSettings();
        }
        throw new RuntimeException("Unknown fragment tag");
    }

    @Override
    public boolean isUsingCustomLM() {
        return usingCustomLM;
    }

    @Override
    public void setUsingCustomLM(boolean usingCustomLM) {
        this.usingCustomLM = usingCustomLM;
        locationSubscription.stopUpdates();
        locationSubscription = usingCustomLM
                ? new SystemLocationSubscription(this, this)
                : new MapKitLocationSubscription(this);
        MapKitFactory.getInstance().setLocationManager(locationSubscription.getLocationManager());
        locationSubscription.startUpdates();
    }

    @Override
    public void onLocationUpdated(@NonNull Location location) {
        userPosition.setGeometry(location.getPosition());
        userPosition.setVisible(true);
        centerMap(location.getPosition());
    }

    @Override
    public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {
        userPosition.setVisible(locationStatus == LocationStatus.AVAILABLE);
    }

    private void createPlacemark() {
        userPosition = mapObjects.addEmptyPlacemark(new Point(0, 0));
        userPosition.setVisible(false);
        userPosition.setIcon(ImageProvider.fromResource(getApplicationContext(), R.drawable.ya_point));
    }

    private void centerMap(@NonNull final Point position) {
        final Map map = mapview.getMap();
        final CameraPosition prevCameraPosition = map.getCameraPosition();
        final CameraPosition cameraPosition = new CameraPosition(
                position,
                prevCameraPosition.getZoom(),
                prevCameraPosition.getAzimuth(),
                prevCameraPosition.getTilt());
        final float userLocationMoveTime = 0.5f;
        final Animation animation = new Animation(Animation.Type.SMOOTH, userLocationMoveTime);
        map.move(cameraPosition, animation, /* cameraCallback */ null);
    }
}
