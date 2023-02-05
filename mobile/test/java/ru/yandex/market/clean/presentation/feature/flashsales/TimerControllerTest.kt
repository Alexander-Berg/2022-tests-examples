package ru.yandex.market.clean.presentation.feature.flashsales

import com.annimon.stream.Optional
import io.reactivex.schedulers.TestScheduler
import io.reactivex.schedulers.assertQueueSize
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doReturnConsecutively
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.feature.timer.ui.ElapsedTimeFormatter
import ru.yandex.market.feature.timer.ui.ElapsedTimeVo
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.presentationSchedulersMock
import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.feature.timer.ui.ElapsedVideoTimeVo
import ru.yandex.market.feature.timer.ui.TimerController
import ru.yandex.market.feature.timer.ui.ElapsedTimeCalculator
import ru.yandex.market.feature.timer.ui.elapsedVideoTimeVoTestInstance
import ru.yandex.market.test.extensions.arg
import ru.yandex.market.utils.advanceTimeBy
import ru.yandex.market.utils.asOptional
import ru.yandex.market.utils.createDate
import ru.yandex.market.utils.days
import ru.yandex.market.utils.hours
import ru.yandex.market.utils.minus
import ru.yandex.market.utils.minutes
import ru.yandex.market.utils.normalize
import ru.yandex.market.utils.plus
import ru.yandex.market.utils.seconds
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TimerControllerTest {

    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentUnixTimeInMillis } doReturn NOW.time
    }
    private val formatter = mock<ElapsedTimeFormatter> {
        on { format(any()) } doAnswer { it.arg<Long>().toVo() }
    }
    private val schedulers = presentationSchedulersMock()
    private val configuration = spy(
        TimerController.Configuration(
            startEpochTime = (NOW - 2.hours).time,
            endEpochTime = (NOW + 2.hours).time,
            updatePeriod = UPDATE_PERIOD
        )
    )
    private val flashSalesElapsedTimeCalculator = mock<ElapsedTimeCalculator> {
        on { getMaximumElapsedMilliseconds() } doReturn 1.days.longMillis
        on { getMillisecondsUntil(any()) } doAnswer {
            normalize(
                value = it.arg<Long>() - dateTimeProvider.currentUnixTimeInMillis,
                rangeStart = 0,
                rangeEnd = 1.days.longMillis
            )
        }
    }
    private val controller = TimerController(
        flashSalesElapsedTimeCalculator,
        dateTimeProvider,
        formatter,
        schedulers,
        configuration
    )
    private val timerScheduler = TestScheduler()

    private fun Long.toVo(): ElapsedVideoTimeVo {
        return ElapsedVideoTimeVo(
            milliseconds = this,
            formattedNormal = toString(),
            formattedWide = toString(),
            formattedWithoutHoursNormal = toString()
        )
    }

    @Test
    fun `Multiple subscribers use same inner observable`() {
        whenever(schedulers.timer) doReturn timerScheduler

        controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        timerScheduler.triggerActions()

        controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        timerScheduler.triggerActions()

        verify(formatter, times(1)).format(any())
    }

    @Test
    fun `Dispose inner observable after last subscriber disposed`() {
        whenever(schedulers.timer) doReturn timerScheduler

        controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()
            .dispose()

        timerScheduler.triggerActions()
        timerScheduler.assertQueueSize(0)
    }

    @Test
    fun `Resubscribe to previously disposed observable starts it again`() {
        whenever(schedulers.timer) doReturn timerScheduler
        val formattedOutputs = listOf<Long>(10, 9, 8).map { it.toVo() }
        whenever(formatter.format(any())) doReturnConsecutively formattedOutputs.toList()

        controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()
            .dispose()

        val observer = controller.getFlowable().test()
        timerScheduler.tick(formattedOutputs.size - 1)

        observer.assertValueSequenceOnly(formattedOutputs.map {
            ElapsedTimeVo(
                _milliseconds = it.milliseconds,
                formattedNormal = it.formattedNormal,
                formattedWide = it.formattedWide,
                formattedWithoutHoursNormal = it.formattedWithoutHoursNormal
            ).asOptional()
        })
    }

    @Test
    fun `Dispose observable after end time passes`() {
        whenever(schedulers.timer) doReturn timerScheduler
        val endTime = configuration.endEpochTime
        whenever(dateTimeProvider.currentUnixTimeInMillis) doReturnConsecutively listOf(
            NOW.time,
            endTime + 1
        )

        val observer = controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        timerScheduler.tick()

        observer.assertComplete()
    }

    @Test
    fun `Do not tick every update interval until start time`() {
        whenever(schedulers.timer) doReturn timerScheduler
        val startTime = configuration.startEpochTime
        val intervalsUntilStart = 5
        val testClock = TestClock(startTime - (UPDATE_PERIOD * intervalsUntilStart).longMillis)
        whenever(dateTimeProvider.currentUnixTimeInMillis) doAnswer { testClock.currentTime }

        val observer = controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        timerScheduler.tick(intervalsUntilStart)

        observer.assertValueCount(1)
    }

    @Test
    fun `Start tick every update interval after start time`() {
        whenever(schedulers.timer) doReturn timerScheduler
        val intervalsUntilStart = 5
        val intervalsAfterStart = 5
        val clockStartTime =
            configuration.startEpochTime - (configuration.updatePeriod * intervalsUntilStart).longMillis
        val clock = TestClock(clockStartTime)
        whenever(dateTimeProvider.currentUnixTimeInMillis) doAnswer { clock.currentTime }

        val observer = controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertNotComplete()

        clock.isEnabled = true
        val totalTickCount = intervalsUntilStart + intervalsAfterStart
        timerScheduler.tick(totalTickCount)

        observer.assertValueCount(1 + intervalsAfterStart)
    }

    @Test
    fun `Notify optional empty when subscribe before start time`() {
        val startDateTime = NOW - 1.hours
        whenever(configuration.startEpochTime) doReturn startDateTime.time
        whenever(configuration.endEpochTime) doReturn (NOW + 1.hours).time
        whenever(dateTimeProvider.currentUnixTimeInMillis) doReturn (startDateTime - 10.minutes).time
        whenever(formatter.format(any())) doAnswer { it.arg<Long>().toVo() }

        controller.getFlowable()
            .firstOrError()
            .test()
            .assertResult(Optional.empty())
    }

    @Test
    fun `Coerce elapsed time as one day`() {
        whenever(configuration.endEpochTime) doReturn (NOW + 30.hours).time
        whenever(formatter.format(any())) doAnswer { it.arg<Long>(0).toVo() }

        val vo = 1.days.longMillis.toVo()
        controller.getFlowable()
            .firstOrError()
            .test()
            .assertResult(
                ElapsedTimeVo(
                    _milliseconds = vo.milliseconds,
                    formattedNormal = vo.formattedNormal,
                    formattedWide = vo.formattedWide,
                    formattedWithoutHoursNormal = vo.formattedWithoutHoursNormal
                ).asOptional()
            )
    }

    @Test
    fun `Do not tick until difference between current and end time is greater than one day`() {
        val intervalsUnderOneDay = 5
        whenever(configuration.endEpochTime) doReturn (NOW + 1.days + (UPDATE_PERIOD * intervalsUnderOneDay)).time
        val clock = TestClock(NOW)
        whenever(dateTimeProvider.currentUnixTimeInMillis) doAnswer { clock.currentTime }
        whenever(schedulers.timer) doReturn timerScheduler

        controller.getFlowable()
            .test()
            .assertNoErrors()

        clock.isEnabled = true
        timerScheduler.tick(intervalsUnderOneDay + 1)

        verify(formatter, times(2)).format(any())
    }

    @Test(expected = IllegalStateException::class)
    fun `Throw exception when trying to get observable from different thread`() {
        val latch = CountDownLatch(1)
        Executors.newSingleThreadExecutor().execute {
            controller.getFlowable()
            latch.countDown()
        }
        latch.await(1, TimeUnit.SECONDS)
        controller.getFlowable()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throw exception when configuration start time equal to end time`() {
        TimerController.Configuration(
            startDateTime = NOW,
            endDateTime = NOW,
            updatePeriod = 1.hours
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throw exception when configuration start time greater than end time`() {
        TimerController.Configuration(
            startDateTime = NOW + 10.minutes,
            endDateTime = NOW,
            updatePeriod = 1.hours
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Throw exception when configuration update period is zero`() {
        TimerController.Configuration(
            startDateTime = NOW - 10.minutes,
            endDateTime = NOW + 10.minutes,
            updatePeriod = 0.minutes
        )
    }

    @Test
    fun `Last tick returns zero value even when timings was weird`() {
        val endTime = configuration.endEpochTime
        val timeBeforeStart = endTime - UPDATE_PERIOD.longMillis
        whenever(dateTimeProvider.currentUnixTimeInMillis) doReturnConsecutively
                listOf(timeBeforeStart, timeBeforeStart, timeBeforeStart, endTime + 1)
        whenever(schedulers.timer) doReturn timerScheduler
        val formatterArgs = mutableListOf<Long>()
        whenever(formatter.format(any())) doAnswer {
            formatterArgs.add(it.arg())
            elapsedVideoTimeVoTestInstance()
        }

        controller.getFlowable()
            .test()
            .assertNoErrors()

        timerScheduler.tick(2)

        assertThat(formatterArgs).isEqualTo(listOf(UPDATE_PERIOD.longMillis, 0))
    }

    @Test
    fun `Subscribing just after first tick immediately returns first value`() {
        val startTime = configuration.startEpochTime
        whenever(dateTimeProvider.currentUnixTimeInMillis) doReturnConsecutively
                listOf(startTime, startTime, startTime + (UPDATE_PERIOD / 2).longMillis)
        whenever(schedulers.timer) doReturn timerScheduler

        controller.getFlowable()
            .test()
            .assertNoErrors()

        timerScheduler.triggerActions()

        controller.getFlowable()
            .test()
            .assertValueCount(1)
    }

    @Test
    fun `Subscribing to flowable with delay leads to correct timing being used`() {
        val startTime = configuration.startEpochTime
        val endTime = configuration.endEpochTime
        val flowableGetTime = startTime - 10.minutes.longMillis
        val flowableSubscribeTime = startTime + 10.minutes.longMillis
        whenever(dateTimeProvider.currentUnixTimeInMillis) doReturnConsecutively
                listOf(flowableGetTime, flowableSubscribeTime)
        whenever(schedulers.timer) doReturn timerScheduler

        val observer = controller.getFlowable().test()
        timerScheduler.triggerActions()

        val vo = (endTime - flowableSubscribeTime).toVo()
        observer.assertValuesOnly(
            ElapsedTimeVo(
                _milliseconds = vo.milliseconds,
                formattedNormal = vo.formattedNormal,
                formattedWide = vo.formattedWide,
                formattedWithoutHoursNormal = vo.formattedWithoutHoursNormal
            ).asOptional()
        )
    }

    @Test
    fun `Return empty optional when subscribing after end`() {
        val endTime = configuration.endEpochTime + 1.hours.longMillis
        whenever(dateTimeProvider.currentUnixTimeInMillis) doReturn endTime

        controller.getFlowable()
            .test()
            .assertNoErrors()
            .assertValue(Optional.empty())

        verify(formatter, never()).format(0)
    }

    private fun TestScheduler.tick(times: Int = 1) {
        repeat(times) { advanceTimeBy(UPDATE_PERIOD) }
    }

    private class TestClock(startTime: Long) {

        constructor(startDateTime: Date) : this(startDateTime.time)

        var isEnabled = false

        var currentTime = startTime
            get() {
                val result = field
                if (isEnabled) {
                    field -= UPDATE_PERIOD.longMillis
                }
                return result
            }
    }

    companion object {
        private val NOW = createDate(2020, 3, 4, 17, 8, 0, 0)
        private val UPDATE_PERIOD = 1.seconds
    }
}