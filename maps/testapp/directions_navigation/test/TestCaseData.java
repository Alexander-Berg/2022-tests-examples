package com.yandex.maps.testapp.directions_navigation.test;

import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.RequestPoint;

import java.util.List;

public class TestCaseData {
    public String title = null;
    public String description = null;
    public RouteSelector.SelectorType selectorType = RouteSelector.SelectorType.FIRST_ROUTE_SELECTOR;
    public List<RequestPoint> routePoints = null;
    public List<RequestPoint> simulationRoutePoints = null;
    public String routeUri = null;
    public String simulationRouteUri = null;
    public VehicleOptions vehicleOptions = null;
    public boolean useParkingRoutes = false;
    public boolean disableAlternatives = false;
    public boolean freeDrive = false;
}
