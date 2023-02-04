package com.yandex.maps.testapp.mrcphoto;

import androidx.annotation.NonNull;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.mrc.MrcPhotoTrack;
import com.yandex.mapkit.transport.TransportFactory;
import com.yandex.mapkit.transport.masstransit.PedestrianRouter;
import com.yandex.mapkit.transport.masstransit.Route;
import com.yandex.mapkit.transport.masstransit.Session;
import com.yandex.mapkit.transport.masstransit.TimeOptions;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;

class PedestrianMrcPhotoTrackProvider extends MrcPhotoTrackProvider {
    private final TimeOptions timeOptions = new TimeOptions();
    private final PedestrianRouter router =
            TransportFactory.getInstance().createPedestrianRouter();
    private com.yandex.mapkit.transport.masstransit.Session session = null;

    public PedestrianMrcPhotoTrackProvider(@NonNull MapView mapview,
                                           @NonNull MrcPhotoTrackProvider.PhotoTrackListener photoTrackListener) {
        super(mapview, photoTrackListener);
    }

    @Override
    public void submitRoutingRequestImpl(@NonNull List<Point> points) {
        assert (points.size() > 1);

        if (session != null) {
            session.cancel();
            session = null;
        }

        ArrayList<RequestPoint> requestPoints = new ArrayList<>();
        for (Point point : points) {
            requestPoints.add(new RequestPoint(point,
                    RequestPointType.WAYPOINT,
                    null /* pointContext */));
        }

        LOGGER.info("Requesting a pedestrian route");
        session = router.requestRoutes(
                requestPoints, timeOptions, new Session.RouteListener() {
                    @Override
                    public void onMasstransitRoutes(@NonNull List<Route> routes) {
                        if (routes.isEmpty()) {
                            LOGGER.warning("Unable to build a requested pedestrian route");
                        } else {
                            final Route route = routes.get(0);
                            setRoutePolyline(route.getGeometry());
                            submitMrcTrackRequest(MrcPhotoTrack.TrackType.PEDESTRIAN);
                        }
                    }

                    @Override
                    public void onMasstransitRoutesError(@NonNull Error error) {
                        receiveResponseError(error);
                    }
                });
    }

    @Override
    protected void resetRoute() {
        super.resetRoute();

        if (session != null) {
            session.cancel();
            session = null;
        }
    }
}
