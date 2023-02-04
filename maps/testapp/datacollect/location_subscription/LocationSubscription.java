package com.yandex.maps.testapp.datacollect.location_subscription;

import com.yandex.mapkit.location.LocationManager;

public interface LocationSubscription {
    void startUpdates();

    void stopUpdates();

    LocationManager getLocationManager();
}
