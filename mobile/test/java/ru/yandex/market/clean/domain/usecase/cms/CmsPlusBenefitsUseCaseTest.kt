package ru.yandex.market.clean.domain.usecase.cms

import io.reactivex.subjects.PublishSubject
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.domain.model.cms.CmsPlusBenefitsItem
import ru.yandex.market.clean.domain.usecase.plushome.NeedShowPlusHomeNewsBadgeUseCaseImpl
import ru.yandex.market.domain.cashback.model.CashBackBalance
import ru.yandex.market.domain.cashback.model.cashBackBalanceTestInstance
import ru.yandex.market.domain.cashback.usecase.GetCashbackBalanceStreamUseCase
import ru.yandex.market.domain.user.usecase.HasYandexPlusUseCase
import java.math.BigDecimal

class CmsPlusBenefitsUseCaseTest {

    private val balanceSubject: PublishSubject<CashBackBalance> = PublishSubject.create()
    private val showPlusHomeNewsBadgeSubject: PublishSubject<Boolean> = PublishSubject.create()
    private val hasYandexPlusSubject: PublishSubject<Boolean> = PublishSubject.create()

    private val getCashbackBalanceStreamUseCase = mock<GetCashbackBalanceStreamUseCase> {
        on { execute(true) } doReturn balanceSubject
    }
    private val needShowPlusHomeNewsBadgeUseCase = mock<NeedShowPlusHomeNewsBadgeUseCaseImpl> {
        on { execute() } doReturn showPlusHomeNewsBadgeSubject
    }
    private val hasYandexPlusUseCase = mock<HasYandexPlusUseCase> {
        on { execute() } doReturn hasYandexPlusSubject
    }

    private val useCase = CmsPlusBenefitsUseCase(
        getCashbackBalanceStreamUseCase,
        needShowPlusHomeNewsBadgeUseCase,
        hasYandexPlusUseCase,
    )

    @Test
    fun `Check all enabled user with ya plus value`() {
        val testObservable = useCase.execute().test()

        balanceSubject.onNext(cashBackBalanceTestInstance(balance = BigDecimal.TEN))
        hasYandexPlusSubject.onNext(true)
        showPlusHomeNewsBadgeSubject.onNext(true)

        testObservable
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(
                listOf(
                    CmsPlusBenefitsItem(true, BigDecimal.TEN)
                )
            )
    }

    @Test
    fun `Check all enabled user without ya plus value`() {
        val testObservable = useCase.execute().test()

        balanceSubject.onNext(cashBackBalanceTestInstance(balance = BigDecimal(42)))
        hasYandexPlusSubject.onNext(false)
        showPlusHomeNewsBadgeSubject.onNext(true)

        testObservable
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(
                listOf(
                    CmsPlusBenefitsItem(false, BigDecimal(42))
                )
            )
    }

    @Test
    fun `On zero plus balance return empty value`() {
        val testObservable = useCase.execute().test()

        balanceSubject.onNext(cashBackBalanceTestInstance(balance = BigDecimal.ZERO))
        hasYandexPlusSubject.onNext(true)
        showPlusHomeNewsBadgeSubject.onNext(true)

        testObservable
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(emptyList())
    }

    @Test
    fun `On null plus balance return empty value`() {
        val testObservable = useCase.execute().test()

        balanceSubject.onNext(cashBackBalanceTestInstance(balance = null))
        hasYandexPlusSubject.onNext(true)
        showPlusHomeNewsBadgeSubject.onNext(true)

        testObservable
            .assertNoErrors()
            .assertNotComplete()
            .assertValue(emptyList())
    }

    @Test
    fun `Changes balance or plus status or plus news should update value`() {
        val testObservable = useCase.execute().test()

        //перывй эммит чтобы собрать значение
        balanceSubject.onNext(cashBackBalanceTestInstance())
        hasYandexPlusSubject.onNext(true)
        showPlusHomeNewsBadgeSubject.onNext(true)

        //каждый эммит должен обновить значение
        balanceSubject.onNext(cashBackBalanceTestInstance())
        hasYandexPlusSubject.onNext(true)
        showPlusHomeNewsBadgeSubject.onNext(true)

        testObservable
            .assertNoErrors()
            .assertNotComplete()
            .assertValueCount(4)
    }
}