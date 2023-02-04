package com.yandex.maps.testapp.datacollect.location_subscription;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.location.DummyLocationManager;
import com.yandex.mapkit.location.LocationStatus;

public class SystemLocationSubscription implements LocationListener, LocationSubscription {
    private final LocationManager locationManager;
    private final DummyLocationManager dummyLocationManager;
    private final com.yandex.mapkit.location.LocationListener locationListener;

    public SystemLocationSubscription(Context context, com.yandex.mapkit.location.LocationListener listener) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        dummyLocationManager = MapKitFactory.getInstance().createDummyLocationManager();
        locationListener = listener;
    }

    @Override
    public com.yandex.mapkit.location.LocationManager getLocationManager() {
        return dummyLocationManager;
    }

    @Override
    public void startUpdates() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, SystemLocationSubscription.this));
    }

    @Override
    public void stopUpdates() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> locationManager.removeUpdates(SystemLocationSubscription.this));
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        com.yandex.mapkit.location.Location mapKitLocation = toMapKitLocation(location);
        dummyLocationManager.setLocation(mapKitLocation);
        locationListener.onLocationUpdated(mapKitLocation);
    }

    /*
     *  This method was deprecated in API level 29. This callback will never be invoked on Android Q and above.
     *  Implemented only for compatibility with older Android versions.
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        locationListener.onLocationStatusUpdated(status == LocationProvider.AVAILABLE
            ? LocationStatus.AVAILABLE
            : LocationStatus.NOT_AVAILABLE);
    }

    @Override
    public void onProviderEnabled(String provider) {
        locationListener.onLocationStatusUpdated(LocationStatus.AVAILABLE);
    }

    @Override
    public void onProviderDisabled(String provider) {
        locationListener.onLocationStatusUpdated(LocationStatus.NOT_AVAILABLE);
    }

    static private com.yandex.mapkit.location.Location toMapKitLocation(@NonNull Location location) {
        return new com.yandex.mapkit.location.Location(
                new Point(location.getLatitude(), location.getLongitude()),
                location.hasAccuracy() ? (double) location.getAccuracy() : 0,
                location.hasAltitude() ? location.getAltitude() : 0,
                null,
                location.hasBearing() ? (double) location.getBearing() : 0,
                location.hasSpeed() ? (double) location.getSpeed() : 0,
                null,
                0,
                0
        );
    }
}
