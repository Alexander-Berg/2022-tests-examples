package com.yandex.launcher.common.location

import android.location.Location
import android.location.LocationManager
import org.mockito.kotlin.*
import com.yandex.launcher.common.getOldLocationFromProvider
import com.yandex.launcher.common.initLocationProviderWithLocation
import com.yandex.launcher.BaseRobolectricTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.atomic.AtomicReference

class CanUpdateCoordinatesTest: BaseRobolectricTest() {

    private val locationProvider: LocationProvider = mock()
    private lateinit var networkProvidedLocation: Location
    private lateinit var passiveProvidedLocation: Location
    private lateinit var yandexWeatherProvidedLocation: Location
    private lateinit var yandexLocationProvidedLocation: Location
    private lateinit var expiredNetworkLocation: Location
    private lateinit var expiredPassiveLocation: Location
    private lateinit var expiredYandexWeatherLocation: Location
    private lateinit var expiredYandexLocationLocation: Location

    @Before
    override fun setUp() {
        super.setUp()
        doCallRealMethod().`when`(locationProvider).canUpdateCoordinates(any())
        //init location provider's fields manually, since mock is not initialized as expected
        ReflectionHelpers.setField(locationProvider, "locationRef", AtomicReference<Location>())
        ReflectionHelpers.setField(locationProvider, "lastKnownLocationRef", AtomicReference<Location>())
        ReflectionHelpers.setField(locationProvider, "referenceLocationRef", AtomicReference<Location>())

        val validTime = System.currentTimeMillis()
        networkProvidedLocation = Location(LocationManager.NETWORK_PROVIDER)
        networkProvidedLocation.time = validTime

        passiveProvidedLocation = Location(LocationManager.PASSIVE_PROVIDER)
        passiveProvidedLocation.time = validTime

        yandexWeatherProvidedLocation = Location(LocationProvider.YANDEX_WEATHER_PROVIDER)
        yandexWeatherProvidedLocation.time = validTime

        yandexLocationProvidedLocation = Location(LocationProvider.YANDEX_LOCATION_PROVIDER)
        yandexLocationProvidedLocation.time = validTime

        val invalidTime = validTime - LocationProvider.LOCATION_VALIDITY_TIME - 1
        expiredNetworkLocation = Location(LocationManager.NETWORK_PROVIDER)
        expiredNetworkLocation.time = invalidTime

        expiredPassiveLocation = Location(LocationManager.NETWORK_PROVIDER)
        expiredPassiveLocation.time = invalidTime

        expiredYandexWeatherLocation = Location(LocationProvider.YANDEX_WEATHER_PROVIDER)
        expiredYandexWeatherLocation.time = invalidTime

        expiredYandexLocationLocation = Location(LocationProvider.YANDEX_LOCATION_PROVIDER)
        expiredYandexLocationLocation.time = invalidTime
    }

    @Test
    fun `old location is null, location can be updated`() {
        Assume.assumeThat(locationProvider.getOldLocationFromProvider(), nullValue())

        assertThat(locationProvider.canUpdateCoordinates(Location("")), `is`(true))
    }

    @Test
    fun `old location provided by network provider, can be updated to network provided location`() {
        locationProvider.initLocationProviderWithLocation(networkProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(networkProvidedLocation), `is`(true))
    }

    @Test
    fun `old location provided by network provider, can be updated to passive provided location`() {
        locationProvider.initLocationProviderWithLocation(networkProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(passiveProvidedLocation), `is`(true))
    }

    @Test
    fun `old location provided by passive provider, can be updated to network provided location`() {
        locationProvider.initLocationProviderWithLocation(passiveProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(networkProvidedLocation), `is`(true))
    }

    @Test
    fun `old location provided by passive provider, can be updated to passive provided location`() {
        locationProvider.initLocationProviderWithLocation(passiveProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(passiveProvidedLocation), `is`(true))
    }

    @Test
    fun `old location provided by network provider, can not be updated to yandex weather provided location`() {
        locationProvider.initLocationProviderWithLocation(networkProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexWeatherProvidedLocation), `is`(false))
    }

    @Test
    fun `old location provided by passive provider, can not be updated to yandex weather provided location`() {
        locationProvider.initLocationProviderWithLocation(passiveProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexWeatherProvidedLocation), `is`(false))
    }

    @Test
    fun `yandex weather provided location can be updated to network provided location`() {
        locationProvider.initLocationProviderWithLocation(yandexWeatherProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(networkProvidedLocation), `is`(true))
    }

    @Test
    fun `yandex weather provided location can be updated to passive provided location`() {
        locationProvider.initLocationProviderWithLocation(yandexWeatherProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(passiveProvidedLocation), `is`(true))
    }

    @Test
    fun `null location can be updated to yandex weather provided location`() {
        locationProvider.initLocationProviderWithLocation(null)

        assertThat(locationProvider.canUpdateCoordinates(yandexWeatherProvidedLocation), `is`(true))
    }

    @Test
    fun `expired network location can be updated to up to date yandex weather provider location`() {
        locationProvider.initLocationProviderWithLocation(expiredNetworkLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexWeatherProvidedLocation), `is`(true))
    }

    @Test
    fun `expired passive location can be updated to up to date yandex weather provider location`() {
        locationProvider.initLocationProviderWithLocation(expiredPassiveLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexWeatherProvidedLocation), `is`(true))
    }

    @Test
    fun `expired yandex weather location can be updated to up to date yandex weather provider location`() {
        locationProvider.initLocationProviderWithLocation(expiredYandexWeatherLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexWeatherProvidedLocation), `is`(true))
    }


    @Test
    fun `old location provided by network provider, can be updated to yandex location provided location`() {
        locationProvider.initLocationProviderWithLocation(networkProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }

    @Test
    fun `old location provided by passive provider, can be updated to yandex location provided location`() {
        locationProvider.initLocationProviderWithLocation(passiveProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }

    @Test
    fun `yandex weather provided location can be updated to yandex location provided location`() {
        locationProvider.initLocationProviderWithLocation(yandexWeatherProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }

    @Test
    fun `yandex location provided location can be updated to passive provided location`() {
        locationProvider.initLocationProviderWithLocation(yandexLocationProvidedLocation)

        assertThat(locationProvider.canUpdateCoordinates(passiveProvidedLocation), `is`(true))
    }

    @Test
    fun `null location can be updated to yandex location provided location`() {
        locationProvider.initLocationProviderWithLocation(null)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }

    @Test
    fun `expired network location can be updated to up to date yandex location provider location`() {
        locationProvider.initLocationProviderWithLocation(expiredNetworkLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }

    @Test
    fun `expired passive location can be updated to up to date yandex location provider location`() {
        locationProvider.initLocationProviderWithLocation(expiredPassiveLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }

    @Test
    fun `expired yandex weather location can be updated to up to date yandex location provider location`() {
        locationProvider.initLocationProviderWithLocation(expiredYandexWeatherLocation)

        assertThat(locationProvider.canUpdateCoordinates(yandexLocationProvidedLocation), `is`(true))
    }
}
