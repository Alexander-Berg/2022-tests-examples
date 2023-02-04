package com.yandex.maps.testapp.guidance.test;

import android.content.Context;

import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.annotations.AnnotationLanguage;
import com.yandex.maps.testapp.Utils;
import com.yandex.runtime.Error;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class TestCaseFactory {
    private DrivingRouter router = DirectionsFactory.getInstance().createDrivingRouter();
    private Context ctx;
    private DrivingOptions drivingOptions = new DrivingOptions();

    public TestCaseFactory(Context ctx, AnnotationLanguage language) {
        this.ctx = ctx;
        drivingOptions.setAnnotationLanguage(language);
    }

    private DrivingRoute deserializeRoute(int routeIdentifier) throws IOException {
        InputStream is = ctx.getResources().openRawResource(routeIdentifier);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead = 0;
        byte[] data = new byte[256];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return router.routeSerializer().load(buffer.toByteArray());
    }

    public interface TestCaseFactoryCallback {
        void onTestCaseCreated(TestCase testCase);
    }

    private class RoutesHandler {
        DrivingRoute route;
        DrivingRoute simulationRoute;
    }

    private DrivingRoute selectRoute(List<DrivingRoute> routes, RouteSelector selector) {
        if (selector != null) {
            return selector.selectRoute(routes);
        } else if (routes.size() > 0) {
            return routes.get(0);
        }
        return null;
    };

    public void createTestCase(final TestCaseData testCaseData, final TestCaseFactoryCallback callback) throws IOException {
        assert testCaseData.routeIdentifier != null || testCaseData.routePoints != null;
        assert testCaseData.simulationRouteIdentifier != null || testCaseData.simulationRoutePoints != null;
        assert (testCaseData.routeIdentifier != null) == (testCaseData.simulationRouteIdentifier != null);
        assert (testCaseData.routePoints != null) == (testCaseData.simulationRoutePoints != null);
        assert testCaseData.simulationRoutePoints == null || testCaseData.simulationRoutePoints.size() > 1;

        final RoutesHandler routesHandler = new RoutesHandler();

        if (testCaseData.routeIdentifier != null) {
            routesHandler.route = deserializeRoute(testCaseData.routeIdentifier);
        }
        if (testCaseData.simulationRouteIdentifier != null) {
            routesHandler.simulationRoute = deserializeRoute(testCaseData.simulationRouteIdentifier);
        }

        if (testCaseData.simulationRoutePoints != null) {
            router.requestRoutes(
                    testCaseData.simulationRoutePoints,
                    drivingOptions, testCaseData.vehicleOptions, new DrivingSession.DrivingRouteListener() {
                        @Override
                        public void onDrivingRoutes(List<DrivingRoute> routes) {
                            routesHandler.simulationRoute = selectRoute(routes, testCaseData.selector);
                            if (routesHandler.simulationRoute == null) {
                                Utils.showMessage(ctx, "Cannot find route");
                                callback.onTestCaseCreated(null);
                                return;
                            }
                            if (testCaseData.routePoints != null && testCaseData.routePoints.size() > 0) {
                                router.requestRoutes(
                                        testCaseData.routePoints,
                                        drivingOptions,
                                        testCaseData.vehicleOptions,
                                        new DrivingSession.DrivingRouteListener() {
                                            @Override
                                            public void onDrivingRoutes(List<DrivingRoute> routes) {
                                                routesHandler.route = selectRoute(routes, testCaseData.selector);
                                                if (routesHandler.route == null) {
                                                    Utils.showMessage(ctx, "Cannot find route");
                                                    callback.onTestCaseCreated(null);
                                                    return;
                                                }
                                                callback.onTestCaseCreated(new TestCase(
                                                        routesHandler.route,
                                                        testCaseData.routePoints,
                                                        routesHandler.simulationRoute,
                                                        testCaseData.vehicleOptions,
                                                        testCaseData.useParkingRoutes,
                                                        testCaseData.disableAlternatives));
                                            }

                                            @Override
                                            public void onDrivingRoutesError(Error error) {
                                                Utils.showError(ctx, error);
                                                callback.onTestCaseCreated(null);
                                            }
                                        }
                                );
                            } else {
                                callback.onTestCaseCreated(new TestCase(
                                        routesHandler.route,
                                        testCaseData.routePoints,
                                        routesHandler.simulationRoute,
                                        testCaseData.vehicleOptions,
                                        testCaseData.useParkingRoutes,
                                        testCaseData.disableAlternatives));
                            }
                        }

                        @Override
                        public void onDrivingRoutesError(Error error) {
                            Utils.showError(ctx, error);
                            callback.onTestCaseCreated(null);
                        }
                    });
        } else {
            callback.onTestCaseCreated(new TestCase(
                    routesHandler.route,
                    testCaseData.routePoints,
                    routesHandler.simulationRoute,
                    testCaseData.vehicleOptions,
                    testCaseData.useParkingRoutes,
                    testCaseData.disableAlternatives));
        }
    }
}
