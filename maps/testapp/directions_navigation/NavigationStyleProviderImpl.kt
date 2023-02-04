package com.yandex.maps.testapp.directions_navigation

import android.content.Context
import com.yandex.mapkit.directions.driving.Flags
import com.yandex.mapkit.directions.navigation_layer.styling.*
import com.yandex.mapkit.map.ModelStyle
import com.yandex.mapkit.road_events_layer.StyleProvider
import com.yandex.mapkit.styling.*
import com.yandex.mapkit.styling.carnavigation.CarNavigationStyleProvider
import com.yandex.maps.testapp.R
import com.yandex.maps.testapp.directions_navigation.NavigationLayerController.JamsMode
import com.yandex.maps.testapp.directions_navigation.NavigationLayerController.UserPlacemarkType
import com.yandex.runtime.image.ImageProvider
import com.yandex.runtime.model.ModelProvider

class NavigationStyleProviderImpl(context: Context) : NavigationStyleProvider {
    private inner class RouteViewStyleProviderImpl : RouteViewStyleProvider {
        override fun provideJamStyle(flags: Flags, isSelected: Boolean, isNightMode: Boolean, jamStyle: JamStyle) {
            carNavigationStyleProvider.routeViewStyleProvider().provideJamStyle(flags, isSelected, isNightMode, jamStyle)
        }

        override fun providePolylineStyle(flags: Flags, isSelected: Boolean, isNightMode: Boolean, polylineStyle: PolylineStyle) {
            carNavigationStyleProvider.routeViewStyleProvider().providePolylineStyle(flags, isSelected, isNightMode, polylineStyle)
        }

        override fun provideManeuverStyle(flags: Flags, isSelected: Boolean, isNightMode: Boolean, arrowStyle: ArrowStyle) {
            carNavigationStyleProvider.routeViewStyleProvider().provideManeuverStyle(flags, isSelected, isNightMode, arrowStyle)
        }

        override fun provideRouteStyle(flags: Flags, isSelected: Boolean, isNightMode: Boolean, routeStyle: RouteStyle) {
            carNavigationStyleProvider.routeViewStyleProvider().provideRouteStyle(flags, isSelected, isNightMode, routeStyle)
            val showJams = when (currentJamsMode) {
                JamsMode.DISABLED -> false
                JamsMode.ENABLED_FOR_CURRENT_ROUTE -> isSelected
                JamsMode.ENABLED -> true
            }
            routeStyle.setShowJams(showJams)
            if (!flags.predicted) {
                routeStyle.setShowRoute(true)
                routeStyle.setShowTrafficLights(showTrafficLights && isSelected)
                routeStyle.setShowRoadEvents(showRoadEventsOnRoute && isSelected)
                routeStyle.setShowBalloons(showBalloons)
                routeStyle.setShowManeuvers(isSelected)
            } else {
                routeStyle.setShowRoute(showPredicted)
                routeStyle.setShowTrafficLights(false)
                routeStyle.setShowRoadEvents(showRoadEventsOnRoute)
                routeStyle.setShowBalloons(false)
                routeStyle.setShowManeuvers(false)
            }
        }
    }

    private inner class UserPlacemarkStyleProviderImpl : UserPlacemarkStyleProvider {
        override fun provideStyle(scaleFactor: Float, isNightMode: Boolean, style: PlacemarkStyle) {
            val model = when (userPlacemarkType) {
                UserPlacemarkType.GENERAL -> placemarkModelGeneral
                UserPlacemarkType.STANDING -> placemarkModelStanding
            }
            style.setModel(
                model, ModelStyle(75.0f, ModelStyle.UnitType.NORMALIZED, ModelStyle.RenderMode.USER_MODEL))
        }
    }

    var showTrafficLights = true
    var showRoadEventsOnRoute = true
    var showBalloons = true
    var showPredicted = false
    var currentJamsMode = JamsMode.ENABLED_FOR_CURRENT_ROUTE
    var userPlacemarkType = UserPlacemarkType.GENERAL
    override fun routeViewStyleProvider(): RouteViewStyleProvider {
        return routeViewStyleProvider
    }

    override fun balloonImageProvider(): BalloonImageProvider {
        return carNavigationStyleProvider.balloonImageProvider()
    }

    override fun requestPointStyleProvider(): RequestPointStyleProvider {
        return carNavigationStyleProvider.requestPointStyleProvider()
    }

    override fun userPlacemarkStyleProvider(): UserPlacemarkStyleProvider {
        return userPlacemarkStyleProvider
    }

    override fun trafficLightStyleProvider(): TrafficLightStyleProvider {
        return carNavigationStyleProvider.trafficLightStyleProvider()
    }

    private val carNavigationStyleProvider: CarNavigationStyleProvider
    private val routeViewStyleProvider: RouteViewStyleProvider = RouteViewStyleProviderImpl()
    private val userPlacemarkStyleProvider: UserPlacemarkStyleProvider = UserPlacemarkStyleProviderImpl()
    private val placemarkModelGeneral: ModelProvider
    private val placemarkModelStanding: ModelProvider

    init {
        carNavigationStyleProvider = CarNavigationStyleProvider(context)
        val placemarkTextureGeneral = ImageProvider.fromResource(context, R.drawable.user_placemark)
        val placemarkTextureStanding = ImageProvider.fromResource(context, R.drawable.user_placemark_standing)
        placemarkModelGeneral = ModelProvider.fromResource(context, R.raw.user_placemark, placemarkTextureGeneral)
        placemarkModelStanding = ModelProvider.fromResource(context, R.raw.user_placemark, placemarkTextureStanding)
    }
}
