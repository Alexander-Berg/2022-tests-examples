package ru.yandex.yandexbus.inhouse.ui.main.unsupportedcity

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.geometry.Point
import ru.yandex.yandexbus.inhouse.geometry.toMapkitPoint
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo
import ru.yandex.yandexbus.inhouse.service.location.LocationService
import ru.yandex.yandexbus.inhouse.service.location.UserLocation
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.ui.main.unsupportedcity.UnsupportedCityDetector.CityInfo
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable
import rx.observers.AssertableSubscriber

class UnsupportedCityDetectorTest : BaseTest() {

    companion object {
        private val LOCATION_POINT = Point(1.0, 1.0)
        private val LOCATION = UserLocation(position = LOCATION_POINT.toMapkitPoint())

        private val CITY_INCLUDING_LOCATION = LOCATION_POINT
        private val CITY_NOT_INCLUDING_LOCATION = Point(10.0, 10.0)
        private val CITY_SPAN = Point(1.0, 1.0)
    }

    @Mock
    lateinit var locationService: LocationService

    @Mock
    lateinit var regionSettings: RegionSettings

    private val regionsList: MutableList<CityLocationInfo> = mutableListOf()
    private lateinit var detector: UnsupportedCityDetector


    @Before
    override fun setUp() {
        super.setUp()

        val locationObservable = Observable.just(LOCATION)
        whenever(locationService.dangerousLocations).thenReturn(locationObservable)

        whenever(regionSettings.regionsList()).thenReturn(Observable.just(regionsList))

        detector = UnsupportedCityDetector(locationService, regionSettings)
    }

    @Test
    fun unknownCity() {
        regionsList.add(city(enclosesLocation = false))

        assertCityInfo(false, "", CityLocationInfo.UNKNOWN_REGION_ID, detector.start().test())
    }

    @Test
    fun supportedCity() {
        regionsList.add(city(enclosesLocation = true, supported = true, name = "city", id = 1))

        assertCityInfo(true, "city", 1, detector.start().test())
    }

    @Test
    fun unsupportedCity() {
        regionsList.add(city(enclosesLocation = true, supported = false, name = "city", id = 1))

        assertCityInfo(false, "city", 1, detector.start().test())
    }

    @Test
    fun cityWithSubregionsSupported() {
        val city = city(enclosesLocation = false, name = "city", id = 1,
            subregions = listOf(city(enclosesLocation = true, supported = true, name = "sub_city", id = 2)))
        regionsList.add(city)

        assertCityInfo(true, "sub_city", 2, detector.start().test())
    }

    private fun assertCityInfo(supported: Boolean, name: String, id: Int, subscriber: AssertableSubscriber<CityInfo>) {
        subscriber.assertValueCount(1)

        val actual = subscriber.onNextEvents.first()
        assertEquals(supported, actual.supported)
        assertEquals(name, actual.name)
        assertEquals(name, actual.name)
        assertEquals(id, actual.id)

        subscriber.assertCompleted()
    }

    private fun city(
        id: Int = CityLocationInfo.UNKNOWN_REGION_ID,
        name: String = "city_name",
        supported: Boolean = false,
        enclosesLocation: Boolean = false,
        subregions: List<CityLocationInfo> = emptyList()
    ): CityLocationInfo {
        return CityLocationInfo(
            id,
            name,
            geoId = "",
            center = if (enclosesLocation) CITY_INCLUDING_LOCATION else CITY_NOT_INCLUDING_LOCATION,
            span = CITY_SPAN,
            zoom = 0,
            showOnMap = supported,
            hideTransport = false,
            hasVelobike = false,
            newsfeed = false,
            bridges = false,
            partners = emptyList(),
            transportTypes = emptyList(),
            subRegions = subregions,
            poiSuggests = emptyList()
        )
    }
}
