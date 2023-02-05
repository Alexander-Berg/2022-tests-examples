package ru.yandex.yandexbus.inhouse.extensions.mapkit

import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Point

object GeoPlaces {

    object Europe {
        val BALTIC_SEA_BLACK_SEA_BOUNDS = BoundingBox(Point(43.512068, 20.839894), Point(55.038030, 30.644220))
    }

    object Minsk {
        val CENTER = Point(53.902496, 27.561481)
        val YANDEX = Point(53.890991, 27.526205)
        val BOUNDS = BoundingBox(Point(53.813046, 27.349294), Point(53.992361, 27.762971))
    }

    object Kiev {
        val CENTER = Point(50.450458, 30.523460)
        val BOUNDS = BoundingBox(Point(50.286599, 30.221033), Point(50.600295, 30.861297))
    }

    object SaintPetersburg {
        val HERMITAGE = Point(59.940752, 30.312633)
        val KOTLIN = Point(59.990316, 29.771872)
    }

    object Moscow {

        val YANDEX = Point(55.733969, 37.587093)
        val GALLERY = Point(55.741567, 37.620289)
    }

    object Yekaterinburg {
        val THEATER = Point(56.838870, 60.616633)
    }

}