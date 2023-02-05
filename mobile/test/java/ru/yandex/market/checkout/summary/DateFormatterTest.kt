package ru.yandex.market.checkout.summary

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.utils.createDate
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.clean.presentation.feature.sku.DeliveryConditionsFormatter
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.di.TestScope
import java.util.Date
import javax.inject.Inject

@RunWith(Enclosed::class)
class DateFormatterTest {

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FormatUntilShortPlusDayInWeekAtAccusativeTest(
        private val input: Date,
        private val expectedOutput: String
    ) : BaseTest() {

        @Test
        fun testFormattedOutputMatchesExpectations() {
            val formatted = formatter.formatUntilShortPlusDayInWeekAtAccusative(input)
            assertThat(formatted).isEqualTo(expectedOutput)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun testData(): Iterable<Array<*>> = listOf(
                arrayOf(createDate(2018, 4, 21), "до понедельника, 21 мая"),
                arrayOf(createDate(2018, 4, 22), "до вторника, 22 мая"),
                arrayOf(createDate(2018, 4, 23), "до среды, 23 мая"),
                arrayOf(createDate(2018, 4, 24), "до четверга, 24 мая"),
                arrayOf(createDate(2018, 4, 25), "до пятницы, 25 мая"),
                arrayOf(createDate(2018, 4, 26), "до субботы, 26 мая"),
                arrayOf(createDate(2018, 4, 27), "до воскресенья, 27 мая")
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FormatShortPlusDayInWeekAtAccusativeTest(
        private val input: Date,
        private val expectedOutput: String
    ) : BaseTest() {

        @Test
        fun testFormattedOutputMatchesExpectations() {
            val formatted = formatter.formatShortPlusDayInWeekAtAccusative(input)
            assertThat(formatted).isEqualTo(expectedOutput)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun testData(): Iterable<Array<*>> = listOf(
                arrayOf(createDate(2018, 4, 21), "в понедельник, 21 мая"),
                arrayOf(createDate(2018, 4, 22), "во вторник, 22 мая"),
                arrayOf(createDate(2018, 4, 23), "в среду, 23 мая"),
                arrayOf(createDate(2018, 4, 24), "в четверг, 24 мая"),
                arrayOf(createDate(2018, 4, 25), "в пятницу, 25 мая"),
                arrayOf(createDate(2018, 4, 26), "в субботу, 26 мая"),
                arrayOf(createDate(2018, 4, 27), "в воскресенье, 27 мая")
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class SmartFormatRangeAccusativeWithRespectToCurrentDateTest(
        private val startDate: Date?,
        private val endDate: Date?,
        private val expectedOutput: String
    ) : BaseTest() {

        @Before
        override fun setUp() {
            super.setUp()
            TestApplication.instance.currentDateTime = today
        }

        @Test
        fun `Formatting works as expected`() {
            val formatted = formatter.smartFormatRangeAccusativeWithRespectToCurrentDate(
                startDate, endDate
            )

            assertThat(formatted).isEqualTo(expectedOutput)
        }

        companion object {

            private val today = createDate(2018, 6, 16)
            private val tomorrow = createDate(2018, 6, 17)

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: [{0}, {1}] -> {2}")
            @JvmStatic
            fun data(): Iterable<Array<*>> = listOf(
                arrayOf(null, null, ""),
                arrayOf(today, null, "Сегодня, 16\u00A0июля"),
                arrayOf(tomorrow, null, "Завтра, 17\u00A0июля"),
                arrayOf(createDate(2018, 6, 18), null, "в среду, 18 июля"),
                arrayOf(createDate(2018, 6, 19), null, "в четверг, 19 июля"),
                arrayOf(createDate(2018, 6, 20), null, "в пятницу, 20 июля"),
                arrayOf(createDate(2018, 6, 21), null, "в субботу, 21 июля"),
                arrayOf(createDate(2018, 6, 22), null, "в воскресенье, 22 июля"),
                arrayOf(createDate(2018, 6, 23), null, "в понедельник, 23 июля"),
                arrayOf(createDate(2018, 6, 24), null, "во вторник, 24 июля"),
                arrayOf(
                    createDate(2018, 6, 24),
                    createDate(2018, 6, 24),
                    "во вторник, 24 июля"
                ),
                arrayOf(
                    createDate(2018, 6, 24),
                    createDate(2018, 6, 25),
                    "24${DeliveryConditionsFormatter.SEPARATOR}25\u00A0июля"
                ),
                arrayOf(
                    createDate(2018, 6, 24),
                    createDate(2018, 7, 3),
                    "24\u00A0июля${DeliveryConditionsFormatter.SEPARATOR}3\u00A0августа"
                ),
                arrayOf(null, createDate(2018, 6, 18), "в среду, 18 июля")
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FormatNumericShortWithRespectToCurrentDateTest(
        private val input: Date,
        private val expectedOutput: String
    ) : BaseTest() {


        @Before
        override fun setUp() {
            super.setUp()
            TestApplication.instance.currentDateTime = today
        }

        @Test
        fun testFormattedOutputMatchesExpectations() {
            val formatted = formatter.formatNumericShortWithRespectToCurrentDate(input)
            assertThat(formatted).isEqualTo(expectedOutput)
        }

        companion object {

            private val today = createDate(2018, 4, 21)

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0} -> {1}")
            @JvmStatic
            fun testData(): Iterable<Array<*>> = listOf(
                arrayOf(createDate(2018, 4, 20), "с 20.05"),
                arrayOf(createDate(2018, 4, 21), "с 21.05, сегодня"),
                arrayOf(createDate(2018, 4, 22), "с 22.05, завтра"),
                arrayOf(createDate(2018, 4, 23), "с 23.05"),
                arrayOf(createDate(2018, 4, 24), "с 24.05"),
                arrayOf(createDate(2018, 4, 25), "с 25.05"),
                arrayOf(createDate(2018, 4, 26), "с 26.05"),
                arrayOf(createDate(2018, 4, 27), "с 27.05")
            )
        }
    }

    abstract class BaseTest {

        @Inject
        lateinit var formatter: DateFormatter

        @Before
        open fun setUp() {
            DaggerDateFormatterTest_Component.builder()
                .testComponent(TestApplication.instance.component)
                .build()
                .injectMembers(this)
        }
    }

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<BaseTest>
}