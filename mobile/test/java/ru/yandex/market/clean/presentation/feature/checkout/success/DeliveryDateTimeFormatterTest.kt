package ru.yandex.market.clean.presentation.feature.checkout.success

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.test.extensions.nullAny
import ru.yandex.market.utils.createDate
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.common.LocalTime
import ru.yandex.market.clean.presentation.feature.sku.DeliveryConditionsFormatter
import ru.yandex.market.di.TestScope
import java.util.Date
import javax.inject.Inject

private fun dateFrom() = createDate(2018, 4, 29)

private fun dateTo() = createDate(2018, 4, 31)

private fun timeFrom() = LocalTime.builder()
    .hours(10)
    .minutes(0)
    .seconds(0)
    .build()

private fun timeTo() = LocalTime.builder()
    .hours(18)
    .minutes(0)
    .seconds(0)
    .build()

@RunWith(Enclosed::class)
class DeliveryDateTimeFormatterTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FormatDateTimeTest(
        private val dateFrom: Date?,
        private val dateTo: Date?,
        private val timeFrom: LocalTime?,
        private val timeTo: LocalTime?,
        private val expected: String
    ) : BaseTest() {

        @Test
        fun testFormatDateTime() {
            val formatted = formatter.formatDateTimeAtAccusative(dateFrom, dateTo, timeFrom, timeTo)
            assertThat(formatted).isEqualTo(expected)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}, {1}, {2}, {3} -> {4}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(nullAny, nullAny, nullAny, nullAny, ""),
                arrayOf(nullAny, nullAny, nullAny, timeTo(), "до 18:00"),
                arrayOf(nullAny, nullAny, timeFrom(), nullAny, "с 10:00"),
                arrayOf(nullAny, nullAny, timeFrom(), timeTo(), "с 10:00 до 18:00"),
                arrayOf(nullAny, dateTo(), nullAny, nullAny, "в четверг, 31 мая"),
                arrayOf(nullAny, dateTo(), timeFrom(), nullAny, "в четверг, 31 мая, с 10:00"),
                arrayOf(
                    nullAny,
                    dateTo(),
                    timeFrom(),
                    timeTo(),
                    "в четверг, 31 мая, с 10:00 до 18:00"
                ),
                arrayOf(dateFrom(), nullAny, nullAny, nullAny, "во вторник, 29 мая"),
                arrayOf(dateFrom(), nullAny, nullAny, timeTo(), "во вторник, 29 мая, до 18:00"),
                arrayOf(
                    dateFrom(),
                    nullAny,
                    timeFrom(),
                    timeTo(),
                    "во вторник, 29 мая, с 10:00 до 18:00"
                ),
                arrayOf(dateFrom(), nullAny, timeFrom(), nullAny, "во вторник, 29 мая, с 10:00"),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    nullAny,
                    nullAny,
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая"
                ),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    nullAny,
                    timeTo(),
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая, до 18:00"
                ),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    timeFrom(),
                    nullAny,
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая, с 10:00"
                ),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    timeFrom(),
                    timeTo(),
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая, с 10:00 до 18:00"
                )
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FormatDateTimeWithoutDayOfWeekTest(
        private val dateFrom: Date?,
        private val dateTo: Date?,
        private val timeFrom: LocalTime?,
        private val timeTo: LocalTime?,
        private val expected: String
    ) : BaseTest() {

        @Test
        fun testFormatDateTimeWithoutDayOfWeek() {
            val formatted = formatter.formatDateTimeWithoutDayOfWeek(dateFrom, dateTo, timeFrom, timeTo)
            assertThat(formatted).isEqualTo(expected)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}, {1}, {2}, {3} -> {4}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(nullAny, nullAny, nullAny, nullAny, ""),
                arrayOf(nullAny, nullAny, nullAny, timeTo(), "до 18:00"),
                arrayOf(nullAny, nullAny, timeFrom(), nullAny, "с 10:00"),
                arrayOf(nullAny, nullAny, timeFrom(), timeTo(), "с 10:00 до 18:00"),
                arrayOf(nullAny, dateTo(), nullAny, nullAny, "31\u00A0мая"),
                arrayOf(nullAny, dateTo(), timeFrom(), nullAny, "31\u00A0мая, с 10:00"),
                arrayOf(
                    nullAny,
                    dateTo(),
                    timeFrom(),
                    timeTo(),
                    "31\u00A0мая, с 10:00 до 18:00"
                ),
                arrayOf(dateFrom(), nullAny, nullAny, nullAny, "29\u00A0мая"),
                arrayOf(dateFrom(), nullAny, nullAny, timeTo(), "29\u00A0мая, до 18:00"),
                arrayOf(
                    dateFrom(),
                    nullAny,
                    timeFrom(),
                    timeTo(),
                    "29\u00A0мая, с 10:00 до 18:00"
                ),
                arrayOf(dateFrom(), nullAny, timeFrom(), nullAny, "29\u00A0мая, с 10:00"),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    nullAny,
                    nullAny,
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая"
                ),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    nullAny,
                    timeTo(),
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая, до 18:00"
                ),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    timeFrom(),
                    nullAny,
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая, с 10:00"
                ),
                arrayOf(
                    dateFrom(),
                    dateTo(),
                    timeFrom(),
                    timeTo(),
                    "29${DeliveryConditionsFormatter.SEPARATOR}31\u00A0мая, с 10:00 до 18:00"
                )
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FormatTimeFromUntilTest(
        private val timeFrom: LocalTime?,
        private val timeTo: LocalTime?,
        private val expected: String
    ) : BaseTest() {

        @Test
        fun formatTimeFromUntil() {
            val formatted = formatter.formatTimeFromUntil(timeFrom, timeTo)
            assertThat(formatted).isEqualTo(expected)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}, {1}, -> {2}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(
                arrayOf(nullAny, nullAny, ""),
                arrayOf(nullAny, timeTo(), "до 18:00"),
                arrayOf(timeFrom(), nullAny, "с 10:00"),
                arrayOf(timeFrom(), timeTo(), "с 10:00 до 18:00")
            )
        }
    }

    abstract class BaseTest {

        @Inject
        lateinit var formatter: DeliveryDateTimeFormatter

        @Before
        fun setUp() {
            DaggerDeliveryDateTimeFormatterTest_Component.builder()
                .testComponent(TestApplication.instance.component)
                .build()
                .injectMembers(this)
        }
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<BaseTest>
}
