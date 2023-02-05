package ru.yandex.yandexbus.inhouse.taxi.call

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.taxi.experiment.TaxiFeaturesExperiment
import ru.yandex.yandexbus.inhouse.taxi.experiment.TaxiFeaturesExperiment.Group
import ru.yandex.yandexbus.inhouse.utils.ServerTimeProvider
import ru.yandex.yandexbus.inhouse.utils.TimeProvider
import ru.yandex.yandexbus.inhouse.whenever
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class CallTaxiActionAvailabilityProviderTest(private val testData: TestData) : BaseTest() {

    @Mock
    private lateinit var timeProvider: TimeProvider
    @Mock
    private lateinit var serverTimeProvider: ServerTimeProvider
    @Mock
    private lateinit var taxiFeaturesExperiment: TaxiFeaturesExperiment

    private lateinit var availabilityProvider: CallTaxiActionAvailabilityProvider

    override fun setUp() {
        super.setUp()

        val testTimeZone = TimeZone.getTimeZone(TimeZone.getAvailableIDs(testData.currentTimeZoneOffset)[0])
        val testDateMillis = UTC_DATE_FORMAT.parse(testData.date).time

        whenever(timeProvider.timeZone()).thenReturn(testTimeZone)
        whenever(serverTimeProvider.timeMillis()).thenReturn(testDateMillis)
        whenever(taxiFeaturesExperiment.group).thenReturn(testData.taxiFeaturesExperimentGroup)

        availabilityProvider = CallTaxiActionAvailabilityProvider(taxiFeaturesExperiment, timeProvider, serverTimeProvider)
    }

    @Test
    fun test() {
        Assert.assertEquals(testData.expectedAvailable, availabilityProvider.isAvailable())
    }

    data class TestData(
        val date: String,
        val currentTimeZoneOffset: Int,
        val taxiFeaturesExperimentGroup: Group?,
        val expectedAvailable: Boolean
    )

    private companion object {

        private val UTC_DATE_FORMAT = SimpleDateFormat("EEE, HH:mm:ss").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        private const val UTC_TIMEZONE_OFFSET = 0
        private val MINSK_TIMEZONE_OFFSET = TimeUnit.HOURS.toMillis(3).toInt()

        @JvmStatic
        @Parameterized.Parameters
        fun testData() = listOf(
            TestData("MON, 23:00:00", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, true),
            TestData("MON, 00:00:00", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, true),
            TestData("MON, 04:59:59", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, true),
            TestData("MON, 20:00:00", MINSK_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, true),
            TestData("MON, 00:00:00", MINSK_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, true),
            TestData("MON, 05:00:00", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, false),
            TestData("MON, 04:00:00", MINSK_TIMEZONE_OFFSET, Group.BUTTON_AT_NIGHT, false),

            TestData("SUN, 00:00:00", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_WEEKEND, true),
            TestData("SUN, 10:00:00", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_WEEKEND, true),
            TestData("MON, 00:00:00", UTC_TIMEZONE_OFFSET, Group.BUTTON_AT_WEEKEND, false),

            TestData("MON, 00:00:00", UTC_TIMEZONE_OFFSET, null, false),
            TestData("MON, 10:00:00", UTC_TIMEZONE_OFFSET, null, false),
            TestData("SUN, 00:00:00", UTC_TIMEZONE_OFFSET, null, false),
            TestData("SUN, 10:00:00", UTC_TIMEZONE_OFFSET, null, false)
        )
    }
}
