// Copyright 2021 Yandex LLC. All rights reserved.

package ru.yandex.weatherplugin.widgets.updaters.nowcast

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import ru.yandex.weatherlib.graphql.model.DayForecast
import ru.yandex.weatherlib.graphql.model.Forecast
import ru.yandex.weatherlib.graphql.model.Weather
import ru.yandex.weatherlib.graphql.model.enums.Cloudiness
import ru.yandex.weatherlib.graphql.model.enums.Condition
import ru.yandex.weatherlib.graphql.model.enums.DayTime
import ru.yandex.weatherlib.graphql.model.enums.PrecStrength
import ru.yandex.weatherlib.graphql.model.enums.PrecType
import ru.yandex.weatherplugin.widgets.data.WidgetWeatherDataWrapper
import java.util.Arrays
import java.util.EnumMap
import java.util.Scanner

/**
 * Unit test for WidgetWeatherDataWrapper.
 */
@RunWith(RobolectricTestRunner::class)
class WidgetWeatherDataWrapperTest {

    val expectedUrlsMap = initExpectedMapUrls()

    val forecastDayMock = mock<DayForecast> {
        on { dayTime } doAnswer { mock() }
    }
    val forecastMock = mock<Forecast> {
        on { days } doReturn Arrays.asList(forecastDayMock)
    }
    val hostProvider = TestUtils.buildWeatherHostProviderStub()

    val is24HourFormat = true

    @Test
    fun getBackgroundBitmapUrlTest() {
        var dayTimeValue: DayTime = DayTime.DAY
        var precTypeValue: PrecType = PrecType.NO_TYPE
        var cloudinessValue: Cloudiness = Cloudiness.CLEAR
        var precStrengthValue: PrecStrength = PrecStrength.ZERO
        val forecastDayMock = mock<DayForecast> {
            on { dayTime } doAnswer { dayTimeValue }
        }
        val forecastMock = mock<Forecast> {
            on { days } doReturn Arrays.asList(forecastDayMock)
        }
        val weatherData = mock<Weather> {
            on { forecast } doAnswer { forecastMock }
            on { precType } doAnswer { precTypeValue }
            on { cloudiness } doAnswer { cloudinessValue }
            on { precStrength } doAnswer { precStrengthValue }
        }

        val underTest = WidgetWeatherDataWrapper(weatherData, hostProvider, is24HourFormat)

        for (time in DayTime.values()) {
            dayTimeValue = time
            for (precType in PrecType.values()) {
                precTypeValue = precType
                for (cloudiness in Cloudiness.values()) {
                    cloudinessValue = cloudiness
                    for (precStrength in PrecStrength.values()) {
                        precStrengthValue = precStrength
                        val url = underTest.getBackgroundBitmapUrl()
                        val expectedUrl =
                                expectedUrlsMap[dayTimeValue]?.get(precTypeValue)
                                        ?.get(cloudinessValue)
                                        ?.get(precStrengthValue)
                        Assert.assertEquals(expectedUrl, url)
                    }
                }
            }
        }
    }

    @Test
    fun localityIsAccurateTest() {
        var underTest = WidgetWeatherDataWrapper(TestUtils.buildWeatherMockForLocalityTests(), hostProvider, is24HourFormat)
        Assert.assertTrue(underTest.localityIsAccurate())

        underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(isDistrictExist = false), hostProvider, is24HourFormat)
        Assert.assertFalse(underTest.localityIsAccurate())

        underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(isLocalityExist = false), hostProvider, is24HourFormat)
        Assert.assertFalse(underTest.localityIsAccurate())

        underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(
                        isDistrictExist = false,
                        isLocalityExist = false
                ), hostProvider, is24HourFormat)
        Assert.assertFalse(underTest.localityIsAccurate())
    }

    @Test
    fun getDistrictAndLocalityNameTextTest() {
        val expectedLocationName = "Екатеринбург, квартал Центральный"
        val underTest = WidgetWeatherDataWrapper(TestUtils.buildWeatherMockForLocalityTests(), hostProvider, is24HourFormat)

        Assert.assertEquals(expectedLocationName, underTest.getFullLocationNameText())
    }

    @Test
    fun getOnlyLocalityNameTextTest() {
        val expectedLocationName = "Екатеринбург"

        var underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(isDistrictExist = false), hostProvider, is24HourFormat)
        Assert.assertEquals(expectedLocationName, underTest.getFullLocationNameText())

        underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(isDistrictNameExist = false), hostProvider, is24HourFormat)
        Assert.assertEquals(expectedLocationName, underTest.getFullLocationNameText())
    }


    @Test
    fun getOnlyProvinceNameTextTest() {
        val expectedLocationName = "Свердловская область"

        var underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(
                        isLocalityExist = false,
                        isDistrictExist = false), hostProvider, is24HourFormat)
        Assert.assertEquals(expectedLocationName, underTest.getFullLocationNameText())

        underTest = WidgetWeatherDataWrapper(
                TestUtils.buildWeatherMockForLocalityTests(
                        isLocalityNameExist = false,
                        isDistrictNameExist = false), hostProvider, is24HourFormat)
        Assert.assertEquals(expectedLocationName, underTest.getFullLocationNameText())
    }

    @Test
    fun getTemperatureTest() {
        var temperatureValue = 10
        val weatherData = mock<Weather> {
            on { forecast } doReturn forecastMock
            on { temperature } doAnswer { temperatureValue }
        }

        var underTest = WidgetWeatherDataWrapper(weatherData, hostProvider, is24HourFormat)

        Assert.assertEquals("+", underTest.getTemperatureSign())
        Assert.assertEquals("10", underTest.getTemperatureValue())

        temperatureValue = 0
        underTest = WidgetWeatherDataWrapper(weatherData, hostProvider, is24HourFormat)
        Assert.assertEquals("", underTest.getTemperatureSign())
        Assert.assertEquals("0", underTest.getTemperatureValue())

        temperatureValue = -20
        underTest = WidgetWeatherDataWrapper(weatherData, hostProvider, is24HourFormat)
        Assert.assertEquals("-", underTest.getTemperatureSign())
        Assert.assertEquals("20", underTest.getTemperatureValue())
    }

    @Test
    fun getConditionTest() {
        val expectedCondition = "Облачно с прояснениями"
        val l10nMock = mock<Map<String, String>> {
            on { get(org.mockito.kotlin.any()) } doReturn "облачно с прояснениями"
        }
        val weatherData = mock<Weather> {
            on { forecast } doReturn forecastMock
            on { l10n } doReturn l10nMock
            on { condition } doReturn Condition.CLOUDY
        }

        val underTest = WidgetWeatherDataWrapper(weatherData, hostProvider, is24HourFormat)

        Assert.assertEquals(expectedCondition, underTest.getWeatherCondition())
    }

    private fun initExpectedMapUrls(): HashMap<DayTime, Map<PrecType, Map<Cloudiness, Map<PrecStrength, String>>>> {
        val resultMap =
                HashMap<DayTime, Map<PrecType, Map<Cloudiness, Map<PrecStrength, String>>>>()
        val path = "/background_urls_combinations.txt"
        val urlsFile = Scanner(TestUtils.readFromResources(this, path))
        while (urlsFile.hasNext()) {
            val line = urlsFile.nextLine()
            if (line.isBlank()) {
                break
            }
            val data = line.split("|")
            val time = DayTime.valueOf(data[0])
            val precType = PrecType.valueOf(data[1])
            val cloudiness = Cloudiness.valueOf(data[2])
            val precStrength = PrecStrength.valueOf(data[3])
            val expectedUrl = data[4]
            val precTypeMap =
                    resultMap.getOrDefault(time, EnumMap(PrecType::class.java)).toMutableMap()
            val cloudinessMap = precTypeMap.getOrDefault(precType, EnumMap(Cloudiness::class.java))
                    .toMutableMap()
            val precStrengthMap =
                    cloudinessMap.getOrDefault(cloudiness, EnumMap(PrecStrength::class.java))
                            .toMutableMap()
            precStrengthMap[precStrength] = expectedUrl
            cloudinessMap[cloudiness] = precStrengthMap
            precTypeMap[precType] = cloudinessMap
            resultMap[time] = precTypeMap
        }
        return resultMap
    }
}
