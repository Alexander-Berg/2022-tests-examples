package com.yandex.maps.testapp.datacollect.location_subscription;

import androidx.annotation.NonNull;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.location.FilteringMode;
import com.yandex.mapkit.location.Location;
import com.yandex.mapkit.location.LocationListener;
import com.yandex.mapkit.location.LocationManager;
import com.yandex.mapkit.location.LocationStatus;

public class MapKitLocationSubscription implements LocationSubscription, LocationListener {
    private final LocationManager locationManager;
    private final LocationListener locationListener;

    public MapKitLocationSubscription(LocationListener listener) {
        locationManager = MapKitFactory.getInstance().createLocationManager();
        locationListener = listener;
    }

    @Override
    public void onLocationUpdated(@NonNull Location location) {
        locationListener.onLocationUpdated(location);
    }

    @Override
    public void onLocationStatusUpdated(@NonNull LocationStatus locationStatus) {
        locationListener.onLocationStatusUpdated(locationStatus);
    }

    @Override
    public void startUpdates() {
        locationManager.subscribeForLocationUpdates(0, 0, 0, true, FilteringMode.OFF, this);
    }

    @Override
    public void stopUpdates() {
        locationManager.unsubscribe(this);
    }

    @Override
    public LocationManager getLocationManager() {
        return locationManager;
    }
}
