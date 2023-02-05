package ru.yandex.weatherplugin.widgets.updaters.square

import android.content.Context
import android.content.res.Configuration
import android.text.format.DateFormat
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.mockito.kotlin.spy
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.notNull
import org.robolectric.annotation.Config
import ru.yandex.weatherplugin.widgets.data.WeatherWidgetConfig
import ru.yandex.weatherplugin.widgets.data.WidgetBackgroundMode
import ru.yandex.weatherplugin.widgets.data.WidgetForecastMode
import ru.yandex.weatherplugin.widgets.data.WidgetWeatherDataWrapper
import ru.yandex.weatherplugin.widgets.providers.ImageLoader
import ru.yandex.weatherplugin.widgets.providers.ImagePromise
import ru.yandex.weatherplugin.widgets.updaters.UpdateViewsStrategy
import ru.yandex.weatherplugin.widgets.updaters.WeatherWidgetBuildingListener
import ru.yandex.weatherplugin.widgets.updaters.WidgetUpdateController
import ru.yandex.weatherplugin.widgets.updaters.nowcast.TestUtils
import java.util.concurrent.ExecutorService

/**
 * Unit tests for [ru.yandex.weatherplugin.widgets.updaters.square.DataUpdater].
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = "src/main/AndroidManifest.xml")
class SquareDataUpdaterTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var backgroundMode = WidgetBackgroundMode.IMAGE
    private val is24HourFormat = DateFormat.is24HourFormat(context)
    private val strategy = mock<UpdateViewsStrategy>()
    private val service = mock<ExecutorService>()
    private val controller = mock<WidgetUpdateController>()
    private val listener = mock<WeatherWidgetBuildingListener>()
    private val imagePromise = mock<ImagePromise> {
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
        on { backgroundMode } doAnswer { backgroundMode }
        on { forecastMode } doAnswer { WidgetForecastMode.HOURLY }
        on { widthPx } doReturn 1
        on { heightPx } doReturn 1
    }

    @Before
    fun initialize() {
        context.resources.configuration.orientation = Configuration.ORIENTATION_PORTRAIT
    }

    @Test
    fun testBuildingListenerWithImageBackgroundWithNowcast() {
        backgroundMode = WidgetBackgroundMode.IMAGE

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = true,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather)
        underTest.updateWidget(false)

        Assert.assertEquals(4, underTest.calculateRequireBuildStepsCount())
    }

    @Test
    fun testBuildingListenerWithImageBackgroundWithoutNowcast() {
        backgroundMode = WidgetBackgroundMode.IMAGE

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = false,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather)
        underTest.updateWidget(false)

        Assert.assertEquals(7, underTest.calculateRequireBuildStepsCount())
    }

    @Test
    fun testBuildingListenerWithoutImageBackgroundWithNowcast() {
        backgroundMode = WidgetBackgroundMode.DARK

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = true,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather)
        underTest.updateWidget(false)

        Assert.assertEquals(3, underTest.calculateRequireBuildStepsCount())
    }

    @Test
    fun testBuildingListenerWithoutImageBackgroundWithoutNowcast() {
        backgroundMode = WidgetBackgroundMode.DARK

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = false,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather)
        underTest.updateWidget(false)

        Assert.assertEquals(6, underTest.calculateRequireBuildStepsCount())
    }

    @Test
    fun testBuildingListenerWithoutImageBackgroundWithoutNowcastWithDegradation() {
        backgroundMode = WidgetBackgroundMode.DARK

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = false,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather, degradationParams = mock())
        underTest.updateWidget(false)

        Assert.assertEquals(2, underTest.calculateRequireBuildStepsCount())
    }

    @Test
    fun testBuildingListenerWithImageBackgroundWithoutNowcastWithDegradation() {
        backgroundMode = WidgetBackgroundMode.IMAGE

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = false,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather, degradationParams = mock())
        underTest.updateWidget(false)

        Assert.assertEquals(3, underTest.calculateRequireBuildStepsCount())
    }

    @Test
    fun testBuildingListenerWithImageBackgroundWithNowcastWithDegradation() {
        backgroundMode = WidgetBackgroundMode.IMAGE

        val weather = TestUtils.buildWeatherWrapperMock(
            hasEmercom = false,
            hasNowcast = true,
            is24HourFormat = is24HourFormat
        )

        val underTest = createUnderTest(weather = weather, degradationParams = mock())
        underTest.updateWidget(false)

        Assert.assertEquals(3, underTest.calculateRequireBuildStepsCount())
    }

    private fun createUnderTest(
        weather: WidgetWeatherDataWrapper,
        degradationParams: DegradationParams? = null
    ): DataUpdater =
        spy(
            DataUpdater(
                context = context,
                strategy = strategy,
                service = service,
                config = config,
                imageLoader = imageLoader,
                updater = controller,
                weather = weather,
                degradationParams = degradationParams
            )
        ) {
            on { getOrCreateWidgetBuildingListener(mock()) } doReturn listener
            on { createRemoteViews(any()) } doReturn mock()
        }
}
