package com.yandex.maps.testapp.guidance.test;

import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCaseData {
    public String title;
    public String description;
    public RouteSelector selector;
    List<RequestPoint> routePoints;
    List<RequestPoint> simulationRoutePoints;
    public VehicleOptions vehicleOptions;
    Integer routeIdentifier;
    Integer simulationRouteIdentifier;
    boolean useParkingRoutes = false;
    boolean disableAlternatives = false;

    public TestCaseData(
            String title,
            String description,
            List<RequestPoint> routePoints,
            List<RequestPoint> simulationRoutePoints,
            VehicleOptions vehicleOptions,
            RouteSelector selector,
            boolean useParkingRoutes,
            boolean disableAlternatives) {
        this.title = title;
        this.description = description;
        this.routePoints = routePoints;
        this.simulationRoutePoints = simulationRoutePoints;
        this.selector = selector;
        this.useParkingRoutes = useParkingRoutes;
        this.disableAlternatives = disableAlternatives;
        this.vehicleOptions = vehicleOptions;
    }

    public TestCaseData(
            String title,
            String description,
            RequestPoint[] routePoints,
            Point[] simulationRoutePoints,
            VehicleOptions vehicleOptions,
            RouteSelector selector,
            boolean useParkingRoutes) {
        this(title,
                description,
                Arrays.asList(routePoints),
                TestCaseData.convertToRequestPoints(Arrays.asList(simulationRoutePoints)),
                vehicleOptions,
                selector,
                useParkingRoutes,
                false);
    }

    public TestCaseData(
            String title,
            String description,
            Point[] routePoints,
            Point[] simulationRoutePoints,
            VehicleOptions vehicleOptions,
            RouteSelector selector,
            boolean useParkingRoutes,
            boolean disableAlternatives) {
        this(title,
                description,
                TestCaseData.convertToRequestPoints(Arrays.asList(routePoints)),
                TestCaseData.convertToRequestPoints(Arrays.asList(simulationRoutePoints)),
                vehicleOptions,
                selector,
                useParkingRoutes,
                disableAlternatives);
    }

    public TestCaseData(
            String title,
            String description,
            Point[] routePoints,
            Point[] simulationRoutePoints,
            VehicleOptions vehicleOptions,
            RouteSelector selector,
            boolean useParkingRoutes) {
        this(title,
                description,
                TestCaseData.convertToRequestPoints(Arrays.asList(routePoints)),
                TestCaseData.convertToRequestPoints(Arrays.asList(simulationRoutePoints)),
                vehicleOptions,
                selector,
                useParkingRoutes,
                false);
    }

    public TestCaseData(
            String title,
            String description,
            Point[] routePoints,
            Point[] simulationRoutePoints,
            VehicleOptions vehicleOptions,
            RouteSelector selector) {
        this(title, description, routePoints, simulationRoutePoints, vehicleOptions, selector, false, false);
    }

    public TestCaseData(
            String title,
            String description,
            Point[] routePoints,
            Point[] simulationRoutePoints,
            VehicleOptions vehicleOptions) {
        this(title, description, routePoints, simulationRoutePoints, vehicleOptions,null, false, false);
    }

    public TestCaseData(
            String title,
            String description,
            int routeIdentifier,
            int simulationRouteIdentifier,
            RouteSelector selector) {
        this.title = title;
        this.description = description;
        this.routeIdentifier = routeIdentifier;
        this.simulationRouteIdentifier = simulationRouteIdentifier;
        this.selector = selector;
    }

    public TestCaseData(
            String title,
            String description,
            int routeIdentifier,
            int simulationRouteIdentifier) {
        this(title, description, routeIdentifier, simulationRouteIdentifier, null);
    }

    static List<RequestPoint> convertToRequestPoints(List<Point> routePoints) {
        if (routePoints.size() == 0) {
            return new ArrayList<RequestPoint>();
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

