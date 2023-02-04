package com.yandex.maps.testapp.search.test

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.geometry.Geo
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.geometry.Polyline
import com.yandex.mapkit.search.*
import com.yandex.maps.testapp.search.metadata

const val VIA_BANNER_PAGE_ID = "maps_search_test"
const val ZERO_SPEED_BANNER_PAGE_ID = "maps_search_test"

const val MIN_DISTANCE = 100.0

fun makeBannerTests(): List<TestCase> {
    val list = mutableListOf<TestCase>()

    // Via Banner tests

    list.add(ViaBannerTest("ViaBanner: empty route",
            listOf(),
            Expect()))

    list.add(ViaBannerTest("ViaBanner simple route", listOf(
            Point(55.750, 37.626),
            Point(55.770, 37.596)),
            Expect(Point(55.759, 37.611), "advplace:test-1")))

    list.add(ViaBannerTest("ViaBanner another place", listOf(
            Point(59.939, 30.297),
            Point(59.931, 30.361)),
            Expect("advplace:test-2")))

    list.add(ViaBannerTest("ViaBanner at the end", listOf(
            Point(55.750, 37.626),
            Point(55.759, 37.611)),
            Expect()))

    list.add(ViaBannerTest("ViaBanner long route", listOf(
            Point(55.750, 37.626),
            Point(55.770, 37.596),
            Point(55.773, 37.574),
            Point(55.780, 37.577),
            Point(56.001, 37.248),
            Point(57.624, 34.557)),
            Expect("advplace:test-1")))

    // Zero Speed Banner tests

    list.add(ZeroSpeedBannerTest("ZeroSpeed simple point",
            Point(55.736, 37.591), null,
            Expect("polygon:test-1")))

    list.add(ZeroSpeedBannerTest("ZeroSpeed point with route",
            Point(55.776, 37.600), listOf(
            Point(55.776, 37.600),
            Point(55.777, 37.625)),
            Expect(Point(55.776, 37.600))))

    list.add(ZeroSpeedBannerTest("ZeroSpeed no point",
            Point(0.0, 0.0), null,
            Expect()))

    return list
}

class Expect {
    var point: Point? = null
    var id: String? = null

    val present by lazy { point != null || id != null }

    constructor()

    constructor(point: Point, id: String? = null) {
        this.point = point
        this.id = id
    }

    constructor(id: String) {
        this.id = id
    }
}

open class BannerTest(name: String, private val expected: Expect) : TestCase(name, false) {

    fun checkExpected(banner: GeoObject?) {
        checkThat(banner, if (expected.present) IsNotNull() else IsNull(), "banner")
        if (expected.point != null) {
            // Check distance to expected point
            checkThat(banner?.geometry?.size, EqualTo(1), "banner.geometry.size")

            val point = banner?.geometry?.get(0)?.point
            checkThat(point, IsNotNull(), "banner.geometry[0].point")
            if (point != null && expected.point != null) {
                val distance = Geo.distance(point, expected.point)
                checkThat(distance, LessThan(MIN_DISTANCE), "banner..point distance")
            }
        }

        if (expected.id != null) {
            checkThat(banner?.metadata<BillboardObjectMetadata>()?.placeId,
                    EqualTo(expected.id), "banner id")
        }

        finish()
    }
}

class ViaBannerTest(name: String, val route: List<Point>,
                    expected: Expect) : BannerTest(name, expected) {

    var session: ViaBannerSession? = null

    private val manager: ViaBannerManager by lazy {
        SearchFactory.getInstance().createViaBannerManager(VIA_BANNER_PAGE_ID)
    }

    override fun doTest() {
        session = manager.requestViaBanner(Polyline(route)) { banner ->
            session = null
            checkExpected(banner)
        }
    }
}

class ZeroSpeedBannerTest(name: String, val point: Point, val route: List<Point>?,
                    expected: Expect) : BannerTest(name, expected) {

    var session: ZeroSpeedBannerSession? = null

    private val manager: ZeroSpeedBannerManager by lazy {
        SearchFactory.getInstance().createZeroSpeedBannerManager(ZERO_SPEED_BANNER_PAGE_ID)
    }

    override fun doTest() {
        var polyline: Polyline? = null
        if (route != null) {
            polyline = Polyline(route)
        }

        session = manager.requestZeroSpeedBanner(point, polyline) { banner ->
            session = null
            checkExpected(banner)
        }
    }
}
