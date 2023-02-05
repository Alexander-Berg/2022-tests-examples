// Copyright 2021 Yandex LLC. All rights reserved.

package ru.yandex.weatherplugin.widgets.updaters.nowcast

import android.net.Uri
import org.junit.Assert
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import ru.yandex.weatherlib.graphql.model.DayForecast
import ru.yandex.weatherlib.graphql.model.DayForecastHour
import ru.yandex.weatherlib.graphql.model.Forecast
import ru.yandex.weatherlib.graphql.model.Weather
import ru.yandex.weatherlib.graphql.model.alert.EmercomAlert
import ru.yandex.weatherlib.graphql.model.alert.NowcastAlert
import ru.yandex.weatherlib.graphql.model.enums.Condition
import ru.yandex.weatherlib.graphql.model.location.GeoHierarchy
import ru.yandex.weatherlib.graphql.model.location.GeoObject
import ru.yandex.weatherlib.graphql.model.location.Location
import ru.yandex.weatherplugin.widgets.data.WidgetWeatherDataWrapper
import ru.yandex.weatherplugin.widgets.providers.WeatherHostProvider
import java.io.InputStream
import java.net.URI

/**
 * Utils for weather widget unit tests.
 */
class TestUtils {
    companion object {
        const val STUB_DATA = "STUB_DATA!"
        const val STUB_STRING_TIME = "2022-04-04T23:00:00+03:00"
        const val STUB_STRING_DATE = "Mon, 4"
        val STUB_URL = URI.create("http://stub.inc/stub")
        val STUB_SHORT_LOCATION_NAME = "Yekaterinburg"

        fun buildWeatherWrapperMock(
                hasEmercom: Boolean,
                hasNowcast: Boolean,
                is24HourFormat: Boolean
        ): WidgetWeatherDataWrapper {
            val wrapper = spy(
                WidgetWeatherDataWrapper(
                    buildWeatherMockForUpdatersTests(hasEmercom, hasNowcast),
                    buildWeatherHostProviderStub(),
                    is24HourFormat
                )
            )
            doReturn(STUB_SHORT_LOCATION_NAME).whenever(wrapper).getShortLocationName()
            doReturn(STUB_URL.toString()).whenever(wrapper).getBackgroundBitmapUrl()
            doReturn(true).whenever(wrapper).localityIsAccurate()
            doReturn(STUB_DATA).whenever(wrapper).getFullLocationNameText()
            return wrapper
        }

        fun buildWeatherMockForUpdatersTests(hasEmercom: Boolean, hasNowcast: Boolean): Weather {
            val emercomAlertMock = mock<EmercomAlert> {
                on { message } doReturn STUB_DATA
            }
            val nowcastAlertMock = mock<NowcastAlert> {
                on { message } doReturn STUB_DATA
                on { staticMapURI } doReturn STUB_URL
            }
            val l10nMock = mock<Map<String, String>> {
                on { get(org.mockito.kotlin.any()) } doReturn STUB_DATA
            }
            val hoursList = ArrayList<DayForecastHour>()
            for (i in 0..10) {
                hoursList.add(mock {
                    on { lightIcon } doReturn STUB_URL
                    on { darkIcon } doReturn STUB_URL
                    on { time } doReturn STUB_STRING_TIME
                })
            }

            val daysList = ArrayList<DayForecast>()
            for (i in 0..10) {
                daysList.add(mock {
                    on { lightIcon } doReturn STUB_URL
                    on { darkIcon } doReturn STUB_URL
                    on { rawTime } doReturn STUB_STRING_TIME
                })
            }
            val forecastMock = mock<Forecast> {
                on { hours } doReturn hoursList
                on { days } doReturn daysList
            }
            return mock<Weather> {
                on { temperature } doReturn 0
                on { emercomAlert } doReturn if (hasEmercom) emercomAlertMock else null
                on { nowcastAlert } doReturn if (hasNowcast) nowcastAlertMock else null
                on { feelsLike } doReturn 0
                on { darkIcon } doReturn STUB_URL
                on { lightIcon } doReturn STUB_URL
                on { l10n } doReturn l10nMock
                on { forecast } doReturn forecastMock
                on { condition } doReturn Condition.CLEAR
                on { hasNowcast() } doReturn hasNowcast
            }
        }

        fun buildWeatherMockForLocalityTests(
                isDistrictExist: Boolean = true,
                isLocalityExist: Boolean = true,
                isProvinceExist: Boolean = true,
                isDistrictNameExist: Boolean = true,
                isLocalityNameExist: Boolean = true,
                isProvinceNameExist: Boolean = true
        ): Weather {
            val districtMock = mock<GeoObject> {
                on { name } doReturn if (isDistrictNameExist) "квартал Центральный" else null
            }
            val localityMock = mock<GeoObject> {
                on { name } doReturn if (isLocalityNameExist) "Екатеринбург" else null
            }
            val provinceMock = mock<GeoObject> {
                on { name } doReturn if (isProvinceNameExist) "Свердловская область" else null
            }
            val geoHierarchyMock = mock<GeoHierarchy> {
                on { locality } doReturn if (isLocalityExist) localityMock else null
                on { district } doReturn if (isDistrictExist) districtMock else null
                on { province } doReturn if (isProvinceExist) provinceMock else null
            }
            val locationMock = mock<Location> {
                on { geoHierarchy } doReturn geoHierarchyMock
            }
            return mock<Weather> {
                on { forecast } doReturn mock()
                on { location } doAnswer { locationMock }
            }
        }

        fun buildWeatherHostProviderStub(): WeatherHostProvider {
            val uriMock = mock<Uri> {
                on { toString() } doReturn ""
            }
            return WeatherHostProvider { uriMock }
        }

        fun readFromResources(obj: Any, resourceName: String): InputStream {
            val stream = obj.javaClass.getResourceAsStream(resourceName)
            Assert.assertNotNull("Resource is not found", stream)
            return stream!!
        }
    }
}
