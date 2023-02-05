package ru.yandex.market.clean.domain.usecase.dailybonuses

import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.repository.dailybonuses.DailyBonusesRepository
import ru.yandex.market.clean.domain.model.dailybonuses.DailyBonusInfo
import ru.yandex.market.clean.domain.model.dailybonuses.PopupConfig
import ru.yandex.market.clean.domain.model.dailybonuses.dailyBonusInfoTestInstance
import ru.yandex.market.clean.domain.model.dailybonuses.dailyBonusesInfoTestInstance
import ru.yandex.market.clean.domain.model.dailybonuses.popupConfigTestInstance
import ru.yandex.market.clean.domain.model.offerAffectingInformationTestInstance
import ru.yandex.market.clean.domain.usecase.OfferAffectingInformationUseCase
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.safe.Safe

class GetDailyBonusesUseCaseTest {

    private val dailyBonusesSubject: SingleSubject<Pair<PopupConfig, List<Safe<DailyBonusInfo>>>> =
        SingleSubject.create()

    private val dailyBonusesRepository = mock<DailyBonusesRepository> {
        on { getDailyBonusesAndPopupConfig() } doReturn dailyBonusesSubject
        on { setLastBonusTime(any()) } doReturn Completable.complete()
    }

    private val dateTimeProvider = mock<DateTimeProvider>()

    private val offerAffectingInformationUseCase = mock<OfferAffectingInformationUseCase> {
        on { getOfferAffectingInformation() } doReturn Single.just(offerAffectingInformationTestInstance())
    }

    private val getDailyBonusesUseCase = GetDailyBonusesUseCase(
        dailyBonusesRepository,
        offerAffectingInformationUseCase,
        dateTimeProvider
    )

    @Test
    fun `return values if bonuses received`() {
        val expectedBonus = dailyBonusInfoTestInstance()
        val expectedValue = dailyBonusesInfoTestInstance(bonuses = listOf(expectedBonus))
        dailyBonusesSubject.onSuccess(popupConfigTestInstance() to listOf(Safe.value(expectedBonus)))

        getDailyBonusesUseCase.execute()
            .test()
            .assertNoErrors()
            .assertValue(expectedValue)
    }

    @Test
    fun `return error if bonuses result is error`() {
        val expectedError = IllegalArgumentException()
        dailyBonusesSubject.onSuccess(PopupConfig() to listOf(Safe.error(expectedError)))

        getDailyBonusesUseCase.execute()
            .test()
            .assertError(expectedError)
    }
}
