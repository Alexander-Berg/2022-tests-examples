package ru.yandex.market.clean.domain.usecase.dailybonuses

import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.dailybonuses.DailyBonusesRepository
import ru.yandex.market.datetime.DateTimeProvider

class CanShowDailyBonusesUseCaseTest {

    private val countOfClosingPopup = SingleSubject.create<Int>()
    private val canShowDailyPopup = SingleSubject.create<Boolean>()

    private val dailyBonusesRepository = mock<DailyBonusesRepository> {
        on { getLastBonusTime() } doReturn Single.just(LAST_SHOWING_DAILY_BONUSES_TIME)
        on { getCountOfClosingPopup() } doReturn countOfClosingPopup
        on { canShowDailyPopup() } doReturn canShowDailyPopup
    }

    private val dateTimeProvider = mock<DateTimeProvider>()


    private val canShowDailyBonusesUseCase = CanShowDailyBonusesUseCase(
        dailyBonusesRepository = dailyBonusesRepository,
        dateTimeProvider = dateTimeProvider
    )

    @Test
    fun `can show popup`() {
        countOfClosingPopup.onSuccess(BEFORE_LIMIT_OF_CLOSING)
        canShowDailyPopup.onSuccess(true)
        whenever(dateTimeProvider.currentUtcTimeInMillis) doReturn AFTER_TIME

        canShowDailyBonusesUseCase.execute()
            .test()
            .assertValue(true)
    }

    @Test
    fun `can show popup if last date is idle`() {
        countOfClosingPopup.onSuccess(BEFORE_LIMIT_OF_CLOSING)
        canShowDailyPopup.onSuccess(true)
        whenever(dailyBonusesRepository.getLastBonusTime()) doReturn Single.just(0)
        whenever(dateTimeProvider.currentUtcTimeInMillis) doReturn 0

        canShowDailyBonusesUseCase.execute()
            .test()
            .assertValue(true)
    }

    @Test
    fun `cant show popup if limit of closing got`() {
        countOfClosingPopup.onSuccess(AFTER_LIMIT_OF_CLOSING)
        canShowDailyPopup.onSuccess(true)
        whenever(dateTimeProvider.currentUtcTimeInMillis) doReturn AFTER_TIME

        canShowDailyBonusesUseCase.execute()
            .test()
            .assertValue(false)
    }

    @Test
    fun `cant show popup if early date`() {
        countOfClosingPopup.onSuccess(BEFORE_LIMIT_OF_CLOSING)
        canShowDailyPopup.onSuccess(true)
        whenever(dateTimeProvider.currentUtcTimeInMillis) doReturn BEFORE_TIME

        canShowDailyBonusesUseCase.execute()
            .test()
            .assertValue(false)
    }

    @Test
    fun `cant show popup if last date is idle and flag is false`() {
        countOfClosingPopup.onSuccess(BEFORE_LIMIT_OF_CLOSING)
        canShowDailyPopup.onSuccess(false)
        whenever(dailyBonusesRepository.getLastBonusTime()) doReturn Single.just(0)
        whenever(dateTimeProvider.currentUtcTimeInMillis) doReturn 0

        canShowDailyBonusesUseCase.execute()
            .test()
            .assertValue(false)
    }

    companion object {
        private const val LAST_SHOWING_DAILY_BONUSES_TIME = 1650402060000L // 2022-04-20 00:01:00
        private const val BEFORE_TIME = 1650315660000L // 2022-04-19 00:01:00
        private const val AFTER_TIME = 1650488460000L // 2022-04-21 00:01:00
        private const val BEFORE_LIMIT_OF_CLOSING = 2
        private const val AFTER_LIMIT_OF_CLOSING = 4
    }

}
