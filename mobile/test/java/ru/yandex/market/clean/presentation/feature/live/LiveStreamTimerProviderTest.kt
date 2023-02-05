package ru.yandex.market.clean.presentation.feature.live

import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.feature.timer.ui.ElapsedTimeFormatter
import ru.yandex.market.feature.videosnippets.ui.formatter.TranslationViewersFormatter
import ru.yandex.market.feature.videosnippets.ui.vo.TranslationViewersVo
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.feature.videosnippets.ui.providers.LiveStreamTimerProviderImpl
import ru.yandex.market.feature.videosnippets.ui.vo.LiveStreamStateVo
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.Duration
import java.util.Date

class LiveStreamTimerProviderTest {

    private val presentationSchedulers = presentationSchedulersMock()
    private val dateTimeProvider = mock<DateTimeProvider> {
        on { currentUnixTimeInMillis } doReturn DUMMY_CURRENT_TIME_IN_MILLIS
    }
    private val dateFormatter = mock<DateFormatter>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val elapsedTimeFormatter = mock<ElapsedTimeFormatter>()
    private val viewersFormatter = mock<TranslationViewersFormatter>()
    private val viewersVo = mock<TranslationViewersVo>()

    private val liveStreamTimerProvider = LiveStreamTimerProviderImpl(
        presentationSchedulers,
        dateTimeProvider,
        dateFormatter,
        resourcesDataStore,
        elapsedTimeFormatter,
        viewersFormatter
    )

    @Test
    fun `Should return Scheduled state when start time is later than current time`() {
        whenever(dateFormatter.formatMonthDayInWeekHourAccusative(DUMMY_SCHEDULED_TRANSLATION_DATE))
            .thenReturn(DUMMY_TEXT)
        liveStreamTimerProvider.create(
            DUMMY_SCHEDULED_TRANSLATION_DATE,
            Duration(DUMMY_TRANSLATION_DURATION_IN_SECONDS),
            DUMMY_TOTAL_VIEWS
        )
            .test()
            .assertValue { it is LiveStreamStateVo.Scheduled }
    }

    @Test
    fun `Should return OnAir state when current time is in start time plus duration`() {
        whenever(dateFormatter.formatMonthDayInWeekHourAccusative(DUMMY_ON_AIR_TRANSLATION_DATE)).thenReturn(DUMMY_TEXT)
        whenever(viewersFormatter.format(true, 0)).thenReturn(viewersVo)
        liveStreamTimerProvider.create(
            DUMMY_ON_AIR_TRANSLATION_DATE,
            Duration(DUMMY_TRANSLATION_DURATION_IN_SECONDS),
            DUMMY_TOTAL_VIEWS
        )
            .test()
            .assertValue { it is LiveStreamStateVo.OnAir }
    }

    @Test
    fun `Should return Record state when start time is earlier than current time`() {
        whenever(
            dateFormatter.formatReviewDateWithOptionalYear(DUMMY_RECORD_TRANSLATION_DATE)
        ).thenReturn(DUMMY_RECORD_START_TIME_TEXT)

        whenever(
            resourcesDataStore.getFormattedString(R.string.live_stream_date_record, DUMMY_RECORD_START_TIME_TEXT)
        ).thenReturn(DUMMY_RECORD_FORMATTED_RESULT_TEXT)

        whenever(dateFormatter.formatMonthDayInWeekHourAccusative(DUMMY_RECORD_TRANSLATION_DATE)).thenReturn(DUMMY_TEXT)
        whenever(viewersFormatter.format(false, 0)).thenReturn(viewersVo)
        liveStreamTimerProvider.create(
            DUMMY_RECORD_TRANSLATION_DATE,
            Duration(DUMMY_TRANSLATION_DURATION_IN_SECONDS),
            DUMMY_TOTAL_VIEWS
        )
            .test()
            .assertValue { it == LiveStreamStateVo.Record(DUMMY_RECORD_FORMATTED_RESULT_TEXT, viewersVo) }
    }

    private companion object {
        val DUMMY_ON_AIR_TRANSLATION_DATE = Date(4_950L)
        val DUMMY_RECORD_TRANSLATION_DATE = Date(1_000L)
        val DUMMY_SCHEDULED_TRANSLATION_DATE = Date(10_000L)
        const val DUMMY_CURRENT_TIME_IN_MILLIS = 5_000L
        const val DUMMY_TRANSLATION_DURATION_IN_SECONDS = 3.0 // 1.0 equals 1_000L
        const val DUMMY_TEXT = ""
        const val DUMMY_RECORD_START_TIME_TEXT = "DUMMY_RECORD_START_TIME_TEXT"
        const val DUMMY_RECORD_FORMATTED_RESULT_TEXT = "DUMMY_RECORD_FORMATTED_RESULT_TEXT"
        const val DUMMY_TOTAL_VIEWS = 0
    }
}