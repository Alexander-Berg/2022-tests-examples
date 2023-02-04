package com.yandex.maps.testapp.mrcphoto;

import androidx.annotation.NonNull;

import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.JamStyle;
import com.yandex.mapkit.directions.driving.RouteHelper;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.places.mrc.MrcPhotoTrack;
import com.yandex.maps.testapp.driving.Router;
import com.yandex.runtime.Error;

import java.util.List;

class AutomotiveMrcPhotoTrackProvider extends MrcPhotoTrackProvider {
    private final Router router = new Router();

    public AutomotiveMrcPhotoTrackProvider(@NonNull MapView mapview,
                                           @NonNull MrcPhotoTrackProvider.PhotoTrackListener photoTrackListener) {
        super(mapview, photoTrackListener);
    }

    private DrivingOptions makeDrivingOptions() {
        DrivingOptions drivingOptions = new DrivingOptions();
        drivingOptions.setRoutesCount(1);
        return drivingOptions;
    }

    @Override
    public  void submitRoutingRequestImpl(@NonNull List<Point> points){
        assert (points.size() > 1);
        router.cancel();

        for (Point point : points) {
            router.addWaypoint(point);
        }

        LOGGER.info("Requesting an automotive route");
        final DrivingSession.DrivingRouteListener listener = new DrivingSession.DrivingRouteListener() {
            @Override
            public void onDrivingRoutes(List<DrivingRoute> routes) {
                if (routes.isEmpty()) {
                    LOGGER.warning("Unable to build a requested automotive route");
                } else {
                    final DrivingRoute route = routes.get(0);
                    setRoutePolyline(route.getGeometry());
                    submitMrcTrackRequest(MrcPhotoTrack.TrackType.AUTOMOTIVE);
                    paintRoutePolyline(route);
                }
                router.clear();
            }

            @Override
            public void onDrivingRoutesError(Error error) {
                receiveResponseError(error);
            }
        };

        router.requestRoute(makeDrivingOptions(), new VehicleOptions(), listener);
    }

    protected void paintRoutePolyline(@NonNull DrivingRoute route) {
        if (getRouteMapObject() == null) {
            return;
        }

        RouteHelper.updatePolyline(
                getRouteMapObject(), route,
                new JamStyle(RouteHelper.createDefaultJamStyle().getColors()),
                true);
    }

    @Override
    protected void resetRoute() {
        super.resetRoute();
        router.cancel();
    }
}
