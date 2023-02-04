package com.yandex.maps.testapp.mrcphoto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.PlacesFactory;
import com.yandex.mapkit.places.mrc.MrcPhotoTrack;
import com.yandex.mapkit.places.mrc.MrcPhotoTrackService;
import com.yandex.runtime.Error;

import java.util.List;
import java.util.logging.Logger;

abstract class MrcPhotoTrackProvider implements MapObjectTapListener {
    interface PhotoTrackListener {
        // This method is called once a user taps MRC photo track.
        // Position is null if there is no photo at this point on the track.
        void onMrcPhotoTrackTap(@NonNull MrcPhotoTrack mrcPhotoTrack,
                                @Nullable PolylinePosition position);

        void onMrcPhotoTrackError(@NonNull Error error);
    }

    protected static Logger LOGGER = Logger.getLogger("com.yandex.maps.testapp.mrcphoto.MrcPhotoTrackProvider");

    private MapView mapview = null;
    private MrcPhotoTrackService mrcPhotoTrackService = null;
    private MrcPhotoTrack mrcPhotoTrack = null;
    private PhotoTrackListener photoTrackListener = null;
    private RoutePolyline routePolyline = null;

    public MrcPhotoTrackProvider(@NonNull MapView mapview,
                                 @NonNull PhotoTrackListener photoTrackListener) {
        this.mapview = mapview;
        this.routePolyline = new RoutePolyline(mapview);
        this.photoTrackListener = photoTrackListener;

        mrcPhotoTrackService = PlacesFactory.getInstance().createMrcPhotoTrackService();
    }

    @Override
    public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
        if (routePolyline == null || mrcPhotoTrack == null) {
            return false;
        }

        PolylinePosition position = mrcPhotoTrack.snapToCoverage(point);
        if (position == null) {
            LOGGER.info("No MRC photo at this point");
        }

        photoTrackListener.onMrcPhotoTrackTap(mrcPhotoTrack, position);
        return true;
    }

    // This method must be implemented in child classes.
    protected abstract void submitRoutingRequestImpl(@NonNull List<Point> points);

    public void submitRoutingRequest(@NonNull List<Point> points) {
        if (points.size() < 2) {
            LOGGER.severe("Unable to request a route without having at least two points");
            assert(false);
            return;
        }
        submitRoutingRequestImpl(points);
    }

    protected PolylineMapObject getRouteMapObject() {
        return routePolyline.getRouteMapObject();
    }

    protected void resetRoute() {
        routePolyline.unsubsribeFromTaps(this);
        routePolyline.clear();
        mrcPhotoTrack = null;
    }

    protected void setRoutePolyline(@NonNull Polyline polyline) {
        routePolyline.setGeometry(polyline);
        routePolyline.subscribeForTaps(this);
    }

    protected void receiveResponseError(Error error) {
        LOGGER.severe("Got routing error: " + error.toString());
        resetRoute();
    }

    protected void submitMrcTrackRequest(MrcPhotoTrack.TrackType trackType) {
        if (routePolyline == null) {
            LOGGER.severe("Trying to submit MRC track request without having a route polyline");
            resetRoute();
            return;
        }

        LOGGER.info("Requesting MRC photo track");
        MrcPhotoTrackService.MrcPhotoTrackListener mrcPhotoTrackListener = new MrcPhotoTrackService.MrcPhotoTrackListener() {
            @Override
            public void onPhotoTrackResult(@NonNull MrcPhotoTrack mrcPhotoTrack) {
                MrcPhotoTrackProvider.this.mrcPhotoTrack = mrcPhotoTrack;
                routePolyline.setVisible(true);
                routePolyline.focusOnRoute();
            }

            @Override
            public void onPhotoTrackError(@NonNull Error error) {
                LOGGER.warning("Got MRC track service error: " + error.toString());
                photoTrackListener.onMrcPhotoTrackError(error);
            }
        };

        mrcPhotoTrackService.getMrcPhotoTrack(
                trackType,
                routePolyline.getGeometry(),
                mrcPhotoTrackListener);

        // Hide route polyline until MRC track is not available to deprive a user from possibility
        // to tap it.
        routePolyline.setVisible(false);
    }

    public void onDestroy() {
        resetRoute();
        routePolyline.onDestroy();
    }
}
