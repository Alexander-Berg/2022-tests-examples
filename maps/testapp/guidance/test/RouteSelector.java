package com.yandex.maps.testapp.guidance.test;

import com.yandex.mapkit.directions.driving.DrivingRoute;

import java.util.List;

public interface RouteSelector {
    DrivingRoute selectRoute(List<DrivingRoute> routes);
}
