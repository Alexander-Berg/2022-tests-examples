package ru.yandex.yandexbus.inhouse.model.route

import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.search.Address
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.runtime.any.Collection
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.extensions.mapkit.GeoPlaces
import ru.yandex.yandexbus.inhouse.model.route.TestRoutePoints.Minsk
import ru.yandex.yandexbus.inhouse.service.search.SearchResults
import ru.yandex.yandexbus.inhouse.service.search.SearchService
import ru.yandex.yandexbus.inhouse.whenever
import rx.Single

class RoutePointResolverTest : BaseTest() {

    @Mock
    private lateinit var searchService: SearchService
    @Mock
    private lateinit var searchSession: SearchService.SearchSession
    @Mock
    private lateinit var searchResults: SearchResults

    private lateinit var routePointResolver: RoutePointResolver

    override fun setUp() {
        super.setUp()

        whenever(searchSession.search(any(), any())).thenReturn(searchResults)
        whenever(searchService.pointResolveSession(GeoPlaces.Minsk.CENTER)).thenReturn(searchSession)

        routePointResolver = RoutePointResolver(searchService)
    }

    @Test
    fun `resolveAddress returns route point without address on error`() {
        whenever(searchResults.firstResult()).thenReturn(Single.error(Exception()))

        routePointResolver.resolveAddress(GeoPlaces.Minsk.CENTER)
            .test()
            .assertValue(RoutePoint(GeoPlaces.Minsk.CENTER, null))
    }

    @Test
    fun `resolveAddress returns route point with correct address`() {

        mockAddressResponse(MINSK_CENTER_ADDRESS_SHORT, Minsk.CENTER_ROUTE_POINT.address!!)

        routePointResolver.resolveAddress(GeoPlaces.Minsk.CENTER)
            .test()
            .assertValue(MINSK_CENTER_POINT_SHORT)
    }

    @Test
    fun `resolveFormattedAddress returns route point without address on error`() {
        whenever(searchResults.firstResult()).thenReturn(Single.error(Exception()))

        routePointResolver.resolveFormattedAddress(GeoPlaces.Minsk.CENTER)
            .test()
            .assertValue(RoutePoint(GeoPlaces.Minsk.CENTER, null))
    }

    @Test
    fun `resolveFormattedAddress returns route point with correct address`() {

        mockAddressResponse(MINSK_CENTER_ADDRESS_SHORT, Minsk.CENTER_ROUTE_POINT.address!!)

        routePointResolver.resolveFormattedAddress(GeoPlaces.Minsk.CENTER)
            .test()
            .assertValue(TestRoutePoints.Minsk.CENTER_ROUTE_POINT)
    }

    @Test
    fun `resolveAddressRaw calls onError`() {
        val exception = Exception()
        whenever(searchResults.firstResult()).thenReturn(Single.error(exception))

        routePointResolver.resolveAddressRaw(GeoPlaces.Minsk.CENTER)
            .test()
            .assertError(exception)
    }

    @Test
    fun `resolveAddressRaw returns proper response`() {

        val geoObject = mockAddressResponse(MINSK_CENTER_ADDRESS_SHORT, Minsk.CENTER_ROUTE_POINT.address!!)

        routePointResolver.resolveAddressRaw(GeoPlaces.Minsk.CENTER)
            .test()
            .assertValue(MINSK_CENTER_POINT_SHORT to geoObject)
    }

    private fun mockAddressResponse(shortAddress: String, formattedAddress: String): GeoObject {
        val address = mock(Address::class.java)
        whenever(address.formattedAddress).thenReturn(formattedAddress)

        val businessMetadata = mock(BusinessObjectMetadata::class.java)
        whenever(businessMetadata.address).thenReturn(address)

        val collection = mock(Collection::class.java)
        whenever(collection.getItem(BusinessObjectMetadata::class.java)).thenReturn(businessMetadata)

        val geoObject = mock(GeoObject::class.java)
        whenever(geoObject.name).thenReturn(shortAddress)
        whenever(geoObject.metadataContainer).thenReturn(collection)

        whenever(searchResults.firstResult()).thenReturn(Single.just(geoObject))
        return geoObject
    }

    companion object {
        val MINSK_CENTER_ADDRESS_SHORT = "Niamiha"
        val MINSK_CENTER_POINT_SHORT = RoutePoint(GeoPlaces.Minsk.CENTER, MINSK_CENTER_ADDRESS_SHORT)
    }
}