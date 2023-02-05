package ru.yandex.yandexbus.inhouse.model.route

import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces

object TestRoutePoints {

    object Minsk {
        val CENTER_ROUTE_POINT = RoutePoint(GeoPlaces.Minsk.CENTER, "Belarus, Minsk, Niamiha")
    }

    object SaintPetersburg {
        val HERMITAGE_ROUTE_POINT = RoutePoint(GeoPlaces.SaintPetersburg.HERMITAGE, "Saint Petersburg, Hermitage")
        val KOTLIN_ROUTE_POINT = RoutePoint(GeoPlaces.SaintPetersburg.KOTLIN, "Saint Petersburg, Kotlin")
    }

    object Moscow {
        val YANDEX_ROUTE_POINT = RoutePoint(GeoPlaces.Moscow.YANDEX, "Moscow, Lva Tolstogo 16, Yandex")
        val GALLERY_ROUTE_POINT = RoutePoint(GeoPlaces.Moscow.GALLERY, "Moscow, Lavrushinsky Lane, 119017, Tretyakov Gallery")
    }

    object Yekaterinburg {
        val THEATER_ROUTE_POINT = RoutePoint(GeoPlaces.Yekaterinburg.THEATER, "Yekaterinburg, Theater")
    }

}