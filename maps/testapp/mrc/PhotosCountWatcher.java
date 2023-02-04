package com.yandex.maps.testapp.mrc;


import androidx.annotation.NonNull;

import com.yandex.mrc.ride.MRCFactory;
import com.yandex.mrc.LocalRide;
import com.yandex.mrc.LocalRideListener;
import com.yandex.mrc.RideManager;
import com.yandex.mrc.LocalRidesListener;
import com.yandex.mrc.walk.WalkListener;
import com.yandex.mrc.walk.WalkManager;
import com.yandex.runtime.Error;
import com.yandex.runtime.logging.Logger;

public class PhotosCountWatcher {
    private RideManager rideManager;
    private WalkManager walkManager;
    private PhotosCountChangedListener listener;
    private long ridePhotosCount = 0;
    private long placemarksCount = 0;
    private long placemarkPhotosCount = 0;

    public interface PhotosCountChangedListener {
        void onPhotosCountChanged();
    }

    private final LocalRidesListener ridesListener = () -> {
        subscribeRides();
        recalculatePhotosCount();
    };

    private final WalkListener walkListener = new WalkListener() {
        @Override
        public void onPlacemarksUpdated() {
            recalculatePhotosCount();
        }

        @Override
        public void onError(@NonNull Error error) {

        }
    };

    private final LocalRideListener rideListener = new LocalRideListener() {
        @Override
        public void onRideChanged(@NonNull LocalRide ride) {
            recalculatePhotosCount();
        }

        @Override
        public void onRideError(@NonNull LocalRide ride, @NonNull Error error) {
            Logger.error("Ride error: " + error);
        }
    };

    public PhotosCountWatcher() {
        rideManager = MRCFactory.getInstance().getRideManager();
        walkManager = MRCFactory.getInstance().getWalkManager();
    }

    public void subscribe(PhotosCountChangedListener listener) {
        this.listener = listener;

        rideManager.subscribe(ridesListener);
        walkManager.subscribe(walkListener);
        subscribeRides();
        recalculatePhotosCount();
    }

    public void unsubscribe() {
        this.listener = null;

        unsubscribeRides();
        rideManager.unsubscribe(ridesListener);
    }

    public long getRidePhotosCount() {
        return ridePhotosCount;
    }
    public long getPlacemarksCount() {
        return placemarksCount;
    }
    public long getPlacemarkPhotosCount() {
        return placemarkPhotosCount;
    }

    private void subscribeRides() {
        for (LocalRide ride : rideManager.getLocalRides()) {
            ride.subscribe(rideListener);
        }
    }

    private void unsubscribeRides() {
        for (LocalRide ride : rideManager.getLocalRides()) {
            ride.unsubscribe(rideListener);
        }
    }

    private void recalculatePhotosCount() {
        long ridePhotosCount = 0;
        for (LocalRide ride : rideManager.getLocalRides()) {
            ridePhotosCount += ride.getLocalPhotosCount();
        }
        this.ridePhotosCount = ridePhotosCount;

        placemarksCount = walkManager.getPlacemarksCount();
        placemarkPhotosCount = walkManager.getPlacemarkImagesCount();

        if (listener != null) {
            listener.onPhotosCountChanged();
        }
    }
}
