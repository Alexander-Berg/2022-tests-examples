package com.yandex.launcher.widget.weather

import android.location.Location
import android.location.LocationManager
import org.mockito.kotlin.*
import com.yandex.launcher.common.getOldLocationFromProvider
import com.yandex.launcher.common.initLocationProviderWithLocation
import com.yandex.launcher.common.location.LocationProvider
import com.yandex.launcher.common.location.LocationProvider.isYandexWeatherProvider
import com.yandex.launcher.BaseRobolectricTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import java.util.concurrent.atomic.AtomicReference

class YandexWeatherProviderLocationTest: BaseRobolectricTest() {

    private val locationProvider: LocationProvider = mock()
    private lateinit var networkProvidedLocation: Location
    private lateinit var passiveProvidedLocation: Location
    private lateinit var yandexWeatherProvidedLocation: Location
    private lateinit var yandexLocationProvidedLocation: Location

    @Before
    override fun setUp() {
        super.setUp()

        //init location provider's fields manually, since mock is not initialized as expected
        ReflectionHelpers.setField(locationProvider, "locationRef", AtomicReference<Location>())
        ReflectionHelpers.setField(locationProvider, "lastKnownLocationRef", AtomicReference<Location>())
        ReflectionHelpers.setField(locationProvider, "referenceLocationRef", AtomicReference<Location>())

        networkProvidedLocation = Location(LocationManager.NETWORK_PROVIDER)
        passiveProvidedLocation = Location(LocationManager.PASSIVE_PROVIDER)
        yandexWeatherProvidedLocation = Location(LocationProvider.YANDEX_WEATHER_PROVIDER)
        yandexLocationProvidedLocation = Location(LocationProvider.YANDEX_LOCATION_PROVIDER)
    }

    @Test
    fun `location is not received, isYandexWeatherProvidedLocation returns false`() {
        locationProvider.initLocationProviderWithLocation(null)

        assertThat(isYandexWeatherProvider(locationProvider.getOldLocationFromProvider()), `is`(false))
    }

    @Test
    fun `location provided by network, isYandexWeatherProvidedLocation returns false`() {
        locationProvider.initLocationProviderWithLocation(networkProvidedLocation)

        assertThat(isYandexWeatherProvider(locationProvider.getOldLocationFromProvider()), `is`(false))
    }

    @Test
    fun `location provided by passive provider, isYandexWeatherProvidedLocation returns false`() {
        locationProvider.initLocationProviderWithLocation(passiveProvidedLocation)

        assertThat(isYandexWeatherProvider(locationProvider.getOldLocationFromProvider()), `is`(false))
    }

    @Test
    fun `location provided by yandex location provider, isYandexWeatherProvidedLocation returns false`() {
        locationProvider.initLocationProviderWithLocation(yandexLocationProvidedLocation)

        assertThat(isYandexWeatherProvider(locationProvider.getOldLocationFromProvider()), `is`(false))
    }

    @Test
    fun `location provided by yandex weather provider, isYandexWeatherProvidedLocation returns true`() {
        locationProvider.initLocationProviderWithLocation(yandexWeatherProvidedLocation)

        assertThat(isYandexWeatherProvider(locationProvider.getOldLocationFromProvider()), `is`(true))
    }
}
