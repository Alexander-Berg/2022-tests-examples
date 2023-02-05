package ru.yandex.supercheck.model.domain.shops

import org.junit.Test
import ru.yandex.supercheck.model.domain.common.GeoLocation

class GeoLocationTest {

    @Test
    fun testDistance() {
        val eps = 0.0000001

        assert(
            GeoLocation(
                60.0,
                60.0
            ).distanceMetres(GeoLocation(61.0, 60.0)) -
                    GeoLocation(
                        0.0,
                        0.0
                    ).distanceMetres(GeoLocation(0.0, -1.0)) < eps
        )

        assert(
            GeoLocation(
                -90.0,
                50.0
            ).distanceMetres(GeoLocation(90.0, 50.0)) -
                    GeoLocation(
                        0.0,
                        -180.0
                    ).distanceMetres(GeoLocation(0.0, 0.0)) < eps
        )

        assert(
            GeoLocation(
                -90.0,
                50.0
            ).distanceMetres(GeoLocation(90.0, 50.0)) -
                    GeoLocation(
                        10.0,
                        -180.0
                    ).distanceMetres(GeoLocation(10.0, 0.0)) > eps
        )

        //от Зубовского до Морозова меньше 1 км и больше 200 м
        val distance = GeoLocation(55.736525, 37.590073)
            .distanceMetres(GeoLocation(55.734040, 37.587669))
        println(distance)
        assert(distance < 1000 && distance > 200)
    }
}