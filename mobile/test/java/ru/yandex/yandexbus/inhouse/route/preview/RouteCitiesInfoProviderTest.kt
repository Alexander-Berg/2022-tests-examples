package ru.yandex.yandexbus.inhouse.route.preview

import com.yandex.mapkit.geometry.Polyline
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.Moscow.GALLERY
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.Moscow.YANDEX
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.SaintPetersburg.HERMITAGE
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.SaintPetersburg.KOTLIN
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces.Yekaterinburg.THEATER
import ru.yandex.yandexbus.inhouse.model.Cities
import ru.yandex.yandexbus.inhouse.model.CityLocationInfo
import ru.yandex.yandexbus.inhouse.model.TestCityLocationInfo
import ru.yandex.yandexbus.inhouse.model.VehicleType
import ru.yandex.yandexbus.inhouse.model.VehicleType.UNDERGROUND
import ru.yandex.yandexbus.inhouse.model.route.TestRoutePoints
import ru.yandex.yandexbus.inhouse.route.routesetup.testMasstransitRouteModel
import ru.yandex.yandexbus.inhouse.route.routesetup.testRouteSection
import ru.yandex.yandexbus.inhouse.route.routesetup.testTransport
import ru.yandex.yandexbus.inhouse.service.settings.RegionSettings
import ru.yandex.yandexbus.inhouse.whenever
import rx.Observable

class RouteCitiesInfoProviderTest {

    @Mock
    private lateinit var regionSettings: RegionSettings
    @Mock
    private lateinit var allCities: Cities
    @Mock
    private lateinit var citiesWithUnderground: Cities
    @Mock
    private lateinit var mskPolyline: Polyline
    @Mock
    private lateinit var spbPolyline: Polyline

    private lateinit var routeCitiesInfoProvider: RouteCitiesInfoProvider

    @Before
    fun before() {
        MockitoAnnotations.initMocks(this)

        whenever(citiesWithUnderground.findByLocation(HERMITAGE, exactSearch = true)).thenReturn(TestCityLocationInfo.SAINT_PETERSBURG)
        whenever(citiesWithUnderground.findByLocation(KOTLIN, exactSearch = true)).thenReturn(TestCityLocationInfo.SAINT_PETERSBURG)
        whenever(citiesWithUnderground.findByLocation(THEATER, exactSearch = true)).thenReturn(TestCityLocationInfo.YEKATERINBURG)
        whenever(citiesWithUnderground.findByLocation(YANDEX, exactSearch = true)).thenReturn(TestCityLocationInfo.MOSCOW)
        whenever(citiesWithUnderground.findByLocation(GALLERY, exactSearch = true)).thenReturn(TestCityLocationInfo.MOSCOW)

        whenever(regionSettings.cities()).thenReturn(Observable.just(allCities))
        whenever(allCities.filter(any())).thenReturn(citiesWithUnderground)

        whenever(mskPolyline.points).thenReturn(listOf(GeoPlaces.Moscow.YANDEX, GeoPlaces.Moscow.GALLERY))
        whenever(spbPolyline.points).thenReturn(listOf(KOTLIN, HERMITAGE))

        routeCitiesInfoProvider = RouteCitiesInfoProvider(regionSettings)
    }

    @Test
    fun `returns result with empty routes`() {
        routeCitiesInfoProvider.routeRegionsInfo(emptyList())
            .test()
            .assertCompleted()
            .assertValueCount(1)
    }

    @Test
    fun `required points from route exist in result`() {
        val transport = listOf(testTransport(type = VehicleType.UNDERGROUND, isRecommended = true))

        val mskSection = testRouteSection(polyline = mskPolyline, transports = transport)
        val spbSection = testRouteSection(polyline = spbPolyline, transports = transport)

        val route = testMasstransitRouteModel(
            departurePoint = TestRoutePoints.Moscow.YANDEX_ROUTE_POINT,
            destinationPoint = TestRoutePoints.SaintPetersburg.KOTLIN_ROUTE_POINT,
            sections = listOf(mskSection, spbSection)
        )

        val result = routeCitiesInfoProvider.routeRegionsInfo(route)
            .test()
            .assertCompleted()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(CityLocationInfo.MOSCOW_ID, result.cityInfo(YANDEX).id)
        assertEquals(CityLocationInfo.PETERSBURG_ID, result.cityInfo(KOTLIN).id)

        // MOSCOW_POINT_GALLERY and SPB_POINT_HERMITAGE are not beginning points of any section so region is unknown
        assertEquals(CityLocationInfo.UNKNOWN_REGION_ID, result.cityInfo(HERMITAGE).id)
        assertEquals(CityLocationInfo.UNKNOWN_REGION_ID, result.cityInfo(GALLERY).id)
    }

    @Test
    fun `does not contain points for not underground sections`() {
        val transport = listOf(testTransport(type = VehicleType.BUS, isRecommended = true))
        val spbSection = testRouteSection(polyline = spbPolyline, transports = transport)

        val route = testMasstransitRouteModel(
            departurePoint = TestRoutePoints.Moscow.YANDEX_ROUTE_POINT,
            destinationPoint = TestRoutePoints.Yekaterinburg.THEATER_ROUTE_POINT,
            sections = listOf(spbSection)
        )

        val result = routeCitiesInfoProvider.routeRegionsInfo(route)
            .test()
            .assertCompleted()
            .assertValueCount(1)
            .onNextEvents.first()

        assertEquals(CityLocationInfo.UNKNOWN_REGION_ID, result.cityInfo(KOTLIN).id)
        assertEquals(CityLocationInfo.UNKNOWN_REGION_ID, result.cityInfo(HERMITAGE).id)
    }

    @Test
    fun `does not request sections city info for single city route`() {
        val transport = listOf(
            testTransport(
                type = UNDERGROUND,
                isRecommended = true
            )
        )

        val mskSection = testRouteSection(polyline = mskPolyline, transports = transport)

        val route = testMasstransitRouteModel(
            departurePoint = TestRoutePoints.Moscow.YANDEX_ROUTE_POINT,
            destinationPoint = TestRoutePoints.Moscow.GALLERY_ROUTE_POINT,
            sections = listOf(mskSection)
        )

        val result = routeCitiesInfoProvider.routeRegionsInfo(route)
            .test()
            .assertCompleted()
            .assertValueCount(1)
            .onNextEvents.first()

        verify(citiesWithUnderground).findByLocation(YANDEX, exactSearch = true)
        verify(citiesWithUnderground).findByLocation(GALLERY, exactSearch = true)
        verifyNoMoreInteractions(citiesWithUnderground)

        assertEquals(CityLocationInfo.MOSCOW_ID, result.cityInfo(YANDEX).id)
        assertEquals(CityLocationInfo.MOSCOW_ID, result.cityInfo(GALLERY).id)
    }
}