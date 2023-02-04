package com.yandex.maps.testapp.directions_navigation.test;

import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCaseDataBuilder {

    private TestCaseData data = new TestCaseData();

    TestCaseDataBuilder(String title, String description) {
        data.title = title;
        data.description = description;
    }

    public static TestCaseDataBuilder getInstance(String title, String description) {
        return new TestCaseDataBuilder(title, description);
    }

    public TestCaseData build() {
        return data;
    }

    public TestCaseDataBuilder setTitle(String title) {
        data.title = title;
        return this;
    }

    public TestCaseDataBuilder setDescription(String description) {
        data.description = description;
        return this;
    }

    public TestCaseDataBuilder setSelectorType(RouteSelector.SelectorType selectorType) {
        data.selectorType = selectorType;
        return this;
    }

    public TestCaseDataBuilder setRoutePoints(List<RequestPoint> routePoints) {
        assert data.routeUri == null;
        data.routePoints = routePoints;
        return this;
    }

    public TestCaseDataBuilder setRoutePoints(Point[] routePoints) {
        assert data.routeUri == null;
        data.routePoints = convertToRequestPoints(Arrays.asList(routePoints));
        return this;
    }

    public TestCaseDataBuilder setSimulationRoutePoints(List<RequestPoint> simulationRoutePoints) {
        assert data.simulationRouteUri == null;
        data.simulationRoutePoints = simulationRoutePoints;
        return this;
    }

    public TestCaseDataBuilder setSimulationRoutePoints(Point[] simulationRoutePoints) {
        assert data.simulationRouteUri == null;
        data.simulationRoutePoints = convertToRequestPoints(Arrays.asList(simulationRoutePoints));
        return this;
    }

    public TestCaseDataBuilder setRouteUri(String routeUri) {
        assert data.routePoints == null;
        data.routeUri = routeUri;
        return this;
    }

    public TestCaseDataBuilder setSimulationRouteUri(String simulationRouteUri) {
        assert data.simulationRoutePoints == null;
        data.simulationRouteUri = simulationRouteUri;
        return this;
    }

    public TestCaseDataBuilder setVehicleOptions(VehicleOptions vehicleOptions) {
        data.vehicleOptions = vehicleOptions;
        return this;
    }

    public TestCaseDataBuilder setUseParkingRoutes(boolean useParkingRoutes) {
        data.useParkingRoutes = useParkingRoutes;
        return this;
    }

    public TestCaseDataBuilder setDisableAlternatives(boolean disableAlternatives) {
        data.disableAlternatives = disableAlternatives;
        return this;
    }

    public TestCaseDataBuilder setFreeDrive(boolean freeDrive) {
        data.freeDrive = freeDrive;
        return this;
    }

    static List<RequestPoint> convertToRequestPoints(List<Point> routePoints) {
        if (routePoints.size() == 0) {
            return new ArrayList<>();
        }

        assert routePoints.size() > 1;

        ArrayList<RequestPoint> requestPoints = new ArrayList<>();
        requestPoints.add(new RequestPoint(
                routePoints.get(0),
                RequestPointType.WAYPOINT,
                null /* pointContext */));
        for (int i = 1; i < routePoints.size() - 1; ++i) {
            requestPoints.add(new RequestPoint(
                    routePoints.get(i),
                    RequestPointType.VIAPOINT,
                    null /* pointContext */));
        }
        requestPoints.add(new RequestPoint(
                routePoints.get(routePoints.size() - 1),
                RequestPointType.WAYPOINT,
                null /* pointContext */));
        return requestPoints;
    }
}
