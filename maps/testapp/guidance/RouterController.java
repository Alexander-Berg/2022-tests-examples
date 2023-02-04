package com.yandex.maps.testapp.guidance;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.runtime.Error;

import java.util.List;
import java.util.logging.Logger;

public class RouterController {
    public interface RequestRouteCallback {
        void onSuccess(DrivingRoute route);
        void onError(com.yandex.runtime.Error error);
    }


    private static final DrivingRouter router = DirectionsFactory.getInstance().createDrivingRouter();

    private DrivingSession session;

    private static Logger LOGGER = Logger.getLogger("yandex.maps.guidance");

    public RouterController() {
    }

    public void cancelSession() {
        if (session != null) {
            session.cancel();
        }
    }

    public void requestRoute(List<RequestPoint> routePoints,
                             DrivingOptions drivingOptions,
                             VehicleOptions vehicleOptions,
                             final RequestRouteCallback callback) {
        cancelSession();
        drivingOptions.setRoutesCount(1);
        session = router.requestRoutes(routePoints, drivingOptions, vehicleOptions, new DrivingSession.DrivingRouteListener() {
            @Override
            public void onDrivingRoutes(List<DrivingRoute> routes) {
                DrivingRoute route = null;
                if (!routes.isEmpty()) {
                    route = routes.get(0);
                }
                callback.onSuccess(route);
            }

            @Override
            public void onDrivingRoutesError(Error error) {
                callback.onError(error);
            }
        });
    }

}
