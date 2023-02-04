package com.yandex.maps.testapp.directions_navigation.test;

import android.content.Context;

import androidx.annotation.NonNull;

import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.directions.navigation.Guidance;
import com.yandex.mapkit.directions.navigation.Navigation;
import com.yandex.mapkit.directions.navigation.NavigationListener;
import com.yandex.maps.testapp.Utils;
import com.yandex.maps.testapp.directions_navigation.SimulationController;
import com.yandex.runtime.Error;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TestCaseLauncher implements NavigationListener {
    private static final Logger LOGGER = Logger.getLogger("yandex.maps");
    private static final DrivingRouter ROUTER = DirectionsFactory.getInstance().createDrivingRouter();

    private final Navigation navigation;
    private final SimulationController simulationController;
    private final TestCase testCase;
    private final Context context;
    private final RouteSelector routeSelector;
    private final TestCaseLauncherListener testCaseLauncherListener;
    private final TestCaseRoutes testCaseRoutes;

    public interface TestCaseLauncherListener {
        void onTestCaseStarted();
        void onTestCaseFailed();
    }

    private static class TestCaseRoutes {
        public DrivingRoute routeForGuidance;
        public DrivingRoute simulationRoute;

        public TestCaseRoutes() {
            routeForGuidance = null;
            simulationRoute = null;
        }
    }

    public TestCaseLauncher(
            Navigation navigation,
            SimulationController simulationController,
            TestCase testCase,
            Context context,
            TestCaseLauncherListener testCaseLauncherListener) {
        this.navigation = navigation;
        this.simulationController = simulationController;
        this.testCase = testCase;
        this.context = context;
        this.testCaseLauncherListener = testCaseLauncherListener;
        this.routeSelector = new RouteSelector(testCase.selectorType);
        this.testCaseRoutes = new TestCaseRoutes();
    }

    public void launchTestCase() {
        if (!testCase.hasSimulationRoute()) {
            logMessage("No simulation route for testcase");
            testCaseLauncherListener.onTestCaseFailed();
            return;
        }

        DrivingSession.DrivingRouteListener handler = new DrivingSession.DrivingRouteListener() {
            @Override
            public void onDrivingRoutes(@NonNull List<DrivingRoute> routes) {
                testCaseRoutes.simulationRoute = routeSelector.selectRoute(routes);
                if (testCase.freeDrive || !testCase.hasRoute()) {
                    startTestCaseRoutes();
                } else {
                    setUpNavigation();
                }
            }

            @Override
            public void onDrivingRoutesError(@NonNull Error error) {
                logMessage("Error while requesting simulation route for testcase: " + error.toString());
                testCaseLauncherListener.onTestCaseFailed();
            }
        };

        if (testCase.simulationRouteUri != null) {
            ROUTER.resolveUri(
                    testCase.simulationRouteUri,
                    new DrivingOptions(),
                    new VehicleOptions(),
                    handler);
        } else {
            ROUTER.requestRoutes(
                    testCase.simulationRoutePoints,
                    new DrivingOptions(),
                    new VehicleOptions(),
                    handler);
        }
    }

    private void setUpNavigation() {
        Guidance guidance = navigation.getGuidance();
        guidance.setEnableAlternatives(!testCase.disableAlternatives);

        navigation.addListener(TestCaseLauncher.this);
        if (testCase.routeUri != null) {
            navigation.resolveUri(testCase.routeUri);
        } else {
            navigation.requestRoutes(testCase.routePoints, new DrivingOptions());
        }
    }

    @Override
    public void onRoutesBuilt() {
        if (!testCase.freeDrive) {
            testCaseRoutes.routeForGuidance = routeSelector.selectRoute(navigation.getRoutes());
        }
        startTestCaseRoutes();
        navigation.removeListener(this);
    }

    @Override
    public void onRoutesRequestError(@NonNull Error error) {
        logMessage("Error while requesting route for testcase: " + error.toString());
        navigation.removeListener(this);
        testCaseLauncherListener.onTestCaseFailed();
    }

    private void logMessage(String message) {
        Utils.showMessage(context, message, Level.WARNING, LOGGER);
    }

    private void startTestCaseRoutes() {
        if (testCaseRoutes.simulationRoute == null)
        {
            logMessage("No simulation route for testcase");
            testCaseLauncherListener.onTestCaseFailed();
            return;
        }

        navigation.suspend();
        simulationController.startSimulation(testCaseRoutes.simulationRoute);
        navigation.resume();

        if (testCase.freeDrive) {
            navigation.startGuidance(null);
        } else if (testCaseRoutes.routeForGuidance != null) {
            navigation.startGuidance(testCaseRoutes.routeForGuidance);
        } else {
            logMessage("No route for testcase");
        }

        testCaseLauncherListener.onTestCaseStarted();
    }

    // Unused callbacks
    @Override
    public void onRoutesRequested(@NonNull List<RequestPoint> points) { }

    @Override
    public void onAlternativesRequested(@NonNull DrivingRoute currentRoute) { }

    @Override
    public void onUriResolvingRequested(@NonNull String uri) { }

    @Override
    public void onParkingRoutesRequested() { }

    @Override
    public void onResetRoutes() { }
}
