package com.yandex.maps.testapp.driving;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.DrivingSummarySession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.PolylinePosition;
import com.yandex.mapkit.transport.masstransit.Vehicle;
import com.yandex.maps.testapp.common.internal.point_context.PointContextKt;
import com.yandex.runtime.Error;

import java.util.ArrayList;
import java.util.List;

public class Router {
    private DrivingRouter router;
    private DrivingSession session;
    private DrivingSummarySession summarySession;

    private List<RequestPoint> requestPoints = new ArrayList<>();
    private List<List<Point>> arrivalPoints = new ArrayList<>();

    public Router() {
        router = DirectionsFactory.getInstance().createDrivingRouter();
    }

    public int waypointsCount() {
        return requestPoints.size();
    }

    public int addWaypoint(Point point) {
        return addRequestPoint(new RequestPoint(
                point, RequestPointType.WAYPOINT, /*context*/ null));
    }

    public int addRequestPoint(RequestPoint point) {
        requestPoints.add(point);
        arrivalPoints.add(new ArrayList<>());
        return requestPoints.size() - 1;
    }

    public int addAlternativeWaypoint(Point point) {
        int index = requestPoints.size() - 1;
        RequestPoint original = requestPoints.get(index);
        List<Point> alternatives = arrivalPoints.get(index);
        alternatives.add(point);
        requestPoints.set(index,
                new RequestPoint(
                        original.getPoint(),
                        original.getType(),
                        PointContextKt.encode(alternatives)));
        return index;
    }

    public void setWaypoint(int index, Point point) {
        if (0 <= index && index < requestPoints.size()) {
            RequestPointType requestPointType = requestPoints.get(index).getType();
            requestPoints.set(index, new RequestPoint(
                    point,
                    requestPointType,
                    null /* pointContext */));
        }
    }

    private class DrivingListenerWrapper implements DrivingSession.DrivingRouteListener {
        private DrivingSession.DrivingRouteListener listener;

        DrivingListenerWrapper(DrivingSession.DrivingRouteListener listener) {
            this.listener = listener;
        }

        @Override
        public void onDrivingRoutes(List<DrivingRoute> routes) {
            session = null;
            listener.onDrivingRoutes(routes);
        }

        @Override
        public void onDrivingRoutesError(Error error) {
            cancel();
            listener.onDrivingRoutesError(error);
        }
    }

    public void requestRoute(DrivingOptions drivingOptions,
                             VehicleOptions vehicleOptions,
                             DrivingSession.DrivingRouteListener listener) {
        cancel();
        session = router.requestRoutes(
            requestPoints, drivingOptions, vehicleOptions, new DrivingListenerWrapper(listener));
    }

    public void requestAlternativesForRoute(DrivingRoute route,
                                            PolylinePosition inroutePosition,
                                            DrivingOptions drivingOptions,
                                            VehicleOptions vehicleOptions,
                                            DrivingSession.DrivingRouteListener listener) {
        cancel();
        session = router.requestAlternativesForRoute(
            route, inroutePosition, drivingOptions, vehicleOptions, new DrivingListenerWrapper(listener));
    }

    public void requestRouteSummary(DrivingOptions drivingOptions,
                                    VehicleOptions vehicleOptions,
                                    DrivingSummarySession.DrivingSummaryListener listener) {
        cancel();
        summarySession = router.requestRoutesSummary(
            requestPoints, drivingOptions, vehicleOptions, listener);
    }

    public void resolveUri(String uri,
                           DrivingOptions drivingOptions,
                           VehicleOptions vehicleOptions,
                           DrivingSession.DrivingRouteListener listener) {
        cancel();
        session = router.resolveUri(
                uri, drivingOptions, vehicleOptions, new DrivingListenerWrapper(listener));
    }

    public void cancel() {
        if (session != null) {
            session.cancel();
            session = null;
        }
        if (summarySession != null) {
            summarySession.cancel();
            summarySession = null;
        }
    }

    public void clear() {
        cancel();
        requestPoints.clear();
        arrivalPoints.clear();
    }
}
