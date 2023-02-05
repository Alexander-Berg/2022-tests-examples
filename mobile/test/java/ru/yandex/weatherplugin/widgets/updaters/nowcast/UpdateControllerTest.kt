// Copyright 2021 Yandex LLC. All rights reserved.
package ru.yandex.weatherplugin.widgets.updaters.nowcast

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.capture
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.weatherlib.graphql.api.GraphQlWeatherApiService
import ru.yandex.weatherlib.graphql.api.RequestParams
import ru.yandex.weatherlib.graphql.api.WeatherRequest
import ru.yandex.weatherplugin.widgets.WeatherWidgetType
import ru.yandex.weatherplugin.widgets.data.WeatherWidgetConfig
import ru.yandex.weatherplugin.widgets.data.WeatherWidgetCostants
import ru.yandex.weatherplugin.widgets.data.WidgetState
import ru.yandex.weatherplugin.widgets.providers.GeoProvider
import ru.yandex.weatherplugin.widgets.providers.LocationInfo
import ru.yandex.weatherplugin.widgets.updaters.WidgetUpdateController
import ru.yandex.weatherplugin.widgets.updaters.WidgetViewUpdatersFactory
import java.util.concurrent.ExecutorService
import kotlin.test.assertEquals

/**
 * Unit tests for [ru.yandex.weatherplugin.widgets.updaters.WidgetUpdateController].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N, Build.VERSION_CODES.M])
class UpdateControllerTest {
    private val EMPTY_LOCATION_KIND = ""
    private val DEFAULT_GEOID = -1
    private var shouldApiReturnSuccessfulAnswer = true
    private var shouldReturnSuccessfulLocation = true
    private var isRegionDetectedAutomaticallyForGlobal = true
    private var isRegionDetectedAutomaticallyForWidget = true
    private var isWeatherKindLocationForWidget = true
    private var shouldGeoProviderFailed = false
    private var regionIdByGlobalSettings = 54
    private var latitudeByGlobalSettings = 22.22
    private var longitudeByGlobalSettings = 33.33
    private var regionIdByConfigSettings = 213
    private var widgetConfigState = WidgetState.DATA
    private val weatherData =
            TestUtils.buildWeatherMockForUpdatersTests(hasEmercom = false, hasNowcast = false)

    private val locationInfo = mock<LocationInfo>() {
        on { latitude } doReturn latitudeByGlobalSettings
        on { longitude } doReturn longitudeByGlobalSettings
    }
    private val geoProvider = object : GeoProvider {
        override fun isDetectingLocationAutomatically()  = isRegionDetectedAutomaticallyForGlobal

        override fun getOverriddenCityId() = regionIdByGlobalSettings

        override fun getLocation(): LocationInfo? {
            if (shouldGeoProviderFailed) {
                throw InterruptedException()
            }
            return if (shouldReturnSuccessfulLocation) locationInfo else null
        }
    }

    private val dataUpdater = mock<WidgetViewDataUpdater>()
    private val locationErrorUpdater = mock<WidgetViewLocationErrorUpdater>()
    private val dataErrorUpdater = mock<WidgetViewCollectDataErrorUpdater>()
    private val placeholderUpdater = mock<WidgetViewPlaceholdersUpdater>()

    private val factory = mock<WidgetViewUpdatersFactory> {
        on { createDataUpdater(any(), any(), anyOrNull()) } doReturn dataUpdater
        on { createPlaceholdersUpdater(any()) } doReturn placeholderUpdater
        on { createCollectDataErrorUpdater(any()) } doReturn dataErrorUpdater
        on { createLocationErrorUpdater(any()) } doReturn locationErrorUpdater
    }
    private val serviceApi = mock<GraphQlWeatherApiService> {
        on { getWeatherWidgetDataByPoint(any()) } doAnswer { createWeatherRequest() }
        on { getWeatherWidgetDataById(any()) } doAnswer { createWeatherRequest() }
    }

    private fun createWeatherRequest(): WeatherRequest {
        return object : WeatherRequest() {
            override fun execute() {
                callbackList.forEach {
                    if (shouldApiReturnSuccessfulAnswer) {
                        it.onSuccess(weatherData)
                    } else {
                        it.onError(RuntimeException())
                    }
                }
            }
        }
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val executorService = mock<ExecutorService> {
        on { execute(any()) } doAnswer { (it.arguments[0] as Runnable).run() }
    }
    private val config = mock<WeatherWidgetConfig> {
        on { isRegionDetectingAutomatically } doAnswer { isRegionDetectedAutomaticallyForWidget }
        on { regionId } doAnswer { regionIdByConfigSettings }
        on { regionKind } doAnswer {
            if (isWeatherKindLocationForWidget) {
                WeatherWidgetCostants.KIND_WEATHER
            } else {
                EMPTY_LOCATION_KIND
            }
        }
        on { state } doAnswer { widgetConfigState }
        on { language } doAnswer { "DE" }
    }
    private val underTest = WidgetUpdateController(
            WeatherWidgetType.NOWCAST,
            factory,
            geoProvider,
            executorService,
            context,
            serviceApi,
            TestUtils.buildWeatherHostProviderStub()
    )

    @Test
    fun successStateGlobalAutoWidgetAutoTest() {
        setupStateAndCallUpdate()

        val captor: ArgumentCaptor<RequestParams> =
                ArgumentCaptor.forClass(RequestParams::class.java)
        verify(serviceApi).getWeatherWidgetDataByPoint(capture(captor))
        val requestParams = captor.value
        assertEquals(requestParams.geoId, RequestParams.DEFAULT_GEOID)
        assertEquals(requestParams.lat, latitudeByGlobalSettings)
        assertEquals(requestParams.lon, longitudeByGlobalSettings)
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun successStateGlobalRegionWidgetAutoTest() {
        setupStateAndCallUpdate(
                isLocationSuccess = false,
                isGlobalRegionAutoDetect = false,
        )

        val captor: ArgumentCaptor<RequestParams> =
                ArgumentCaptor.forClass(RequestParams::class.java)
        verify(serviceApi).getWeatherWidgetDataById(capture(captor))
        val requestParams = captor.value
        assertEquals(requestParams.geoId, regionIdByGlobalSettings)
        assertEquals(requestParams.lat, RequestParams.DEFAULT_LAT)
        assertEquals(requestParams.lon, RequestParams.DEFAULT_LON)
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun successStateGlobalAutoWidgetRegionTest() {
        setupStateAndCallUpdate(
            isWidgetConfigRegionAutoDetection = false,
        )

        val captor: ArgumentCaptor<RequestParams> =
            ArgumentCaptor.forClass(RequestParams::class.java)
        verify(serviceApi).getWeatherWidgetDataById(capture(captor))
        val requestParams = captor.value
        assertEquals(requestParams.geoId, regionIdByConfigSettings)
        assertEquals(requestParams.lat, RequestParams.DEFAULT_LAT)
        assertEquals(requestParams.lon, RequestParams.DEFAULT_LON)
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun successStateGlobalAutoWidgetRegionNoWeatherKindTest() {
        setupStateAndCallUpdate(
            isWidgetConfigRegionAutoDetection = false,
            isWeatherKindWidgetLocation = false
        )

        val captor: ArgumentCaptor<RequestParams> =
            ArgumentCaptor.forClass(RequestParams::class.java)
        verify(serviceApi).getWeatherWidgetDataByPoint(capture(captor))
        val requestParams = captor.value
        assertEquals(requestParams.geoId, DEFAULT_GEOID)
        assertEquals(requestParams.lat, RequestParams.DEFAULT_LAT)
        assertEquals(requestParams.lon, RequestParams.DEFAULT_LON)
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun successStateWhenGlobalRegionWidgetRegionTest() {
        setupStateAndCallUpdate(
            isLocationSuccess = false,
            isGlobalRegionAutoDetect = false,
            isWidgetConfigRegionAutoDetection = false,
        )

        val captor: ArgumentCaptor<RequestParams> =
            ArgumentCaptor.forClass(RequestParams::class.java)
        verify(serviceApi).getWeatherWidgetDataById(capture(captor))
        val requestParams = captor.value
        assertEquals(requestParams.geoId, regionIdByConfigSettings)
        assertEquals(requestParams.lat, RequestParams.DEFAULT_LAT)
        assertEquals(requestParams.lon, RequestParams.DEFAULT_LON)
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun errorDataStateTest() {
        setupStateAndCallUpdate(
                isApiSuccess = false,
                isGlobalRegionAutoDetect = false,
        )

        verify(serviceApi, times(4)).getWeatherWidgetDataById(any())
        verify(placeholderUpdater, times(4)).updateWidget(eq(true))
        verify(factory, times(4)).createPlaceholdersUpdater(any())
        verify(factory).createCollectDataErrorUpdater(any())
    }

    @Test
    fun receivedLocationIsNullStateTest() {
        setupStateAndCallUpdate(
                isLocationSuccess = false,
        )

        verify(serviceApi, never()).getWeatherWidgetDataByPoint(any())
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createLocationErrorUpdater(any())
    }

    @Test(expected = InterruptedException::class)
    fun gettingLocationErrorStateTest() {
        setupStateAndCallUpdate(
                isLocationSuccess = false,
                isGeoProviderIsInterrupted = true,
        )

        verify(serviceApi, never()).getWeatherWidgetDataByPoint(any())
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createLocationErrorUpdater(any())
    }

    @Test
    fun updateWidgetFromErrorState() {
        setupStateAndCallUpdate(widgetState = WidgetState.ERROR)

        verify(serviceApi).getWeatherWidgetDataByPoint(any())
        verify(placeholderUpdater).updateWidget(eq(false))
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun updateWidgetFromLoadingState() {
        setupStateAndCallUpdate(widgetState = WidgetState.LOADING)

        verify(serviceApi).getWeatherWidgetDataByPoint(any())
        verify(placeholderUpdater).updateWidget(eq(true))
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun updateWidgetFromEmptyState() {
        setupStateAndCallUpdate(widgetState = WidgetState.EMPTY)

        verify(serviceApi).getWeatherWidgetDataByPoint(any())
        verify(placeholderUpdater).updateWidget(eq(false))
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    @Test
    fun updateWidgetFromSuccessState() {
        setupStateAndCallUpdate(widgetState = WidgetState.DATA)

        verify(serviceApi).getWeatherWidgetDataByPoint(any())
        verify(placeholderUpdater).updateWidget(eq(true))
        verify(factory).createPlaceholdersUpdater(any())
        verify(factory).createDataUpdater(any(), any(), any())
    }

    private fun setupStateAndCallUpdate(
            isApiSuccess: Boolean = true,
            isLocationSuccess: Boolean = true,
            isGlobalRegionAutoDetect: Boolean = true,
            isWidgetConfigRegionAutoDetection: Boolean = true,
            isGeoProviderIsInterrupted: Boolean = false,
            widgetState: WidgetState =  WidgetState.DATA,
            isWeatherKindWidgetLocation: Boolean =  true
    ) {

        shouldApiReturnSuccessfulAnswer = isApiSuccess
        shouldReturnSuccessfulLocation = isLocationSuccess
        isRegionDetectedAutomaticallyForGlobal = isGlobalRegionAutoDetect
        isRegionDetectedAutomaticallyForWidget = isWidgetConfigRegionAutoDetection
        isWeatherKindLocationForWidget = isWeatherKindWidgetLocation
        shouldGeoProviderFailed = isGeoProviderIsInterrupted
        widgetConfigState = widgetState

        underTest.updateWidget(config)
    }
}
