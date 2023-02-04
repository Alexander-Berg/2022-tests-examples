package com.yandex.maps.testapp.directions_navigation

import com.yandex.mapkit.directions.driving.DrivingRoute

interface RouteListener {
    fun onCurrentRouteChanged(route: DrivingRoute?)
}
