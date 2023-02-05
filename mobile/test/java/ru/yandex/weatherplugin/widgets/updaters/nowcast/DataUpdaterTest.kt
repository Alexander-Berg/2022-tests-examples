// Copyright 2021 Yandex LLC. All rights reserved.
package ru.yandex.weatherplugin.widgets.updaters.nowcast

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.test.core.app.ApplicationProvider
import com.yandex.android.weather.widgets.R
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.weatherplugin.widgets.data.WeatherWidgetConfig
import ru.yandex.weatherplugin.widgets.data.WeatherWidgetCostants
import ru.yandex.weatherplugin.widgets.data.WidgetBackgroundMode
import ru.yandex.weatherplugin.widgets.data.WidgetForecastMode
import ru.yandex.weatherplugin.widgets.data.WidgetWeatherDataWrapper
import ru.yandex.weatherplugin.widgets.providers.ImageLoader
import ru.yandex.weatherplugin.widgets.providers.ImagePromise
import ru.yandex.weatherplugin.widgets.updaters.UpdateViewsStrategy
import ru.yandex.weatherplugin.widgets.updaters.WeatherWidgetBuildingListener
import ru.yandex.weatherplugin.widgets.updaters.WidgetUpdateController
import java.util.concurrent.ExecutorService

/**
 * Unit tests for [ru.yandex.weatherplugin.widgets.updaters.nowcast.WidgetViewDataUpdater].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml")
class DataUpdaterTest {

    private var widgetBackgroundMode = WidgetBackgroundMode.IMAGE
    private var widgetForecastMode = WidgetForecastMode.HOURLY
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val strategy = mock<UpdateViewsStrategy>()
    private var imagePromise = mock<ImagePromise> {
        var thisMock: ImagePromise? = null
        on {
            thisMock = this
            preProcess(any())
        } doReturn thisMock
    }
    private val imageLoader = mock<ImageLoader> {
        on { load(any()) } doReturn imagePromise
    }
    private val config = mock<WeatherWidgetConfig> {
        on { backgroundMode } doAnswer { widgetBackgroundMode }
        on { widthPx } doReturn 1
        on { heightPx } doReturn 1
        on { forecastMode } doAnswer { widgetForecastMode }
    }
    private val service = mock<ExecutorService>()
    private val controller = mock<WidgetUpdateController>()
    private val listener = mock<WeatherWidgetBuildingListener>()
    private val is24HourFormat = DateFormat.is24HourFormat(context)

    @Before
    fun initTests() {
        context.resources.configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        widgetBackgroundMode = WidgetBackgroundMode.IMAGE
    }

    @Test
    fun testNullWeatherWithoutThrottling() {
        val underTest = createUnderTest(null)
        underTest.updateWidget(false)

        verify(controller).updateDataCollectionError(config)
        Assert.assertEquals(underTest.calculateRequireBuildStepsCount(), 1)
        verify(imageLoader, never()).load(any())
    }

    @Test
    fun testFactState() {
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = false, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITHOUT_MAP,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITHOUT_MAP + 2
        )
    }

    @Test
    fun testForecastModeHourly() {
        widgetForecastMode = WidgetForecastMode.HOURLY
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = false, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verify(weather, times(1)).forecastHours
    }

    @Test
    fun testForecastModeDaily() {
        widgetForecastMode = WidgetForecastMode.DAILY
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = false, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verify(weather, times(1)).forecastDays
    }

    @Test
    fun testNowcastState() {
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = true, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITH_MAP,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITH_MAP + 3
        )
    }

    @Test
    fun testEmercomState() {
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = true, hasNowcast = true, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITHOUT_MAP,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITHOUT_MAP + 2
        )
    }

    @Test
    fun testLandscapeFactState() {
        context.resources.configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = false, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest, 0, 2)
    }

    @Test
    fun testLandscapeNowcastState() {
        context.resources.configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = true, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest, 0, 3)
    }

    @Test
    fun testWithoutBackgroundFactState() {
        widgetBackgroundMode = WidgetBackgroundMode.DARK

        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = false, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITHOUT_MAP,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITHOUT_MAP + 1
        )
    }

    @Test
    fun testWithoutBackgroundNowcastState() {
        widgetBackgroundMode = WidgetBackgroundMode.DARK

        val weather = TestUtils.buildWeatherWrapperMock(hasEmercom = false, hasNowcast = true, is24HourFormat = is24HourFormat)
        val underTest = createUnderTest(weather)
        underTest.updateWidget(false)

        verifyCallbacksListener(underTest,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITH_MAP,
            WeatherWidgetCostants.FORECAST_ITEMS_TO_SHOW_WITH_MAP + 2
        )
    }

    private fun verifyCallbacksListener(underTest: WidgetViewDataUpdater,
                                        expectedForecastCount: Int,
                                        requireImageCount: Int
    ) {
        // all images + build RemoteView finish
        val totalCallbacksCount = requireImageCount + 1

        verify(underTest, times(expectedForecastCount)).createRemoteViews(
            eq(R.layout.widget_weather_nowcast_forecast_item))
        Assert.assertEquals(totalCallbacksCount,
            underTest.calculateRequireBuildStepsCount())
    }

    private fun verifyForecastItemsListener(underTest: WidgetViewDataUpdater,
                                            weather: WidgetWeatherDataWrapper,
                                            expectedForecastCount: Int,
                                            requireImageCount: Int
    ) {
        // all images + build RemoteView finish
        val totalCallbacksCount = requireImageCount + 1

        verify(underTest, times(expectedForecastCount)).createRemoteViews(
            eq(R.layout.widget_weather_nowcast_forecast_item))


    }

    private fun createUnderTest(weather: WidgetWeatherDataWrapper?): WidgetViewDataUpdater =
        spy(WidgetViewDataUpdater(context, strategy, service,
            imageLoader, controller, config, weather)) {
            on { createRemoteViews(eq(R.layout.widget_weather_nowcast_forecast_item)) } doReturn mock()
            on { getOrCreateWidgetBuildingListener(any()) } doReturn listener
            on { createRemoteViews(any()) } doReturn mock()
        }
}
