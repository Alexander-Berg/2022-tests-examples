@file:Suppress("ArchitectureLayersRule")

package ru.yandex.market.clean.domain.usecase.payment

import com.yandex.payment.sdk.core.data.BankName
import com.yandex.payment.sdk.model.data.PartnerInfo
import com.yandex.payment.sdk.model.data.PaymentOption
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.presentation.feature.nativepayment.NativePaymentTargetService
import ru.yandex.market.clean.presentation.feature.oneclick.ActualizePaymentOptionsUseCase
import ru.yandex.market.common.schedulers.TimerScheduler
import ru.yandex.market.utils.seconds
import java.util.concurrent.TimeoutException

class AwaitUserCardsUpdateUseCaseTest {

    private val timerScheduler = TestScheduler()
    private val actualizePaymentOptionsUseCase = mock<ActualizePaymentOptionsUseCase> {
        on { execute(NativePaymentTargetService.MARKET, true) } doReturn Completable.complete()
    }
    private val getUserCardsUseCase = mock<GetUserCardsUseCase>()

    private val useCase = AwaitUserCardsUpdateUseCase(
        actualizePaymentOptionsUseCase = actualizePaymentOptionsUseCase,
        getUserCardsUseCase = getUserCardsUseCase,
        timerScheduler = TimerScheduler(timerScheduler)
    )

    @Test
    fun `wait until user cards update`() {
        val initialCards = listOf(MASTER_CARD, VISA)
        val updatedCards = listOf(MASTER_CARD, VISA, YANDEX_CARD)
        var retryCount = 0
        whenever(getUserCardsUseCase.execute()).doReturn(
            Single.fromCallable {
                retryCount++
                if (retryCount != 4) {
                    initialCards
                } else {
                    updatedCards
                }
            }
        )

        val testObserver = useCase.execute(initialCards).test()
        timerScheduler.advanceTimeBy(TIMEOUT.longValue, TIMEOUT.javaUnit)
        testObserver
            .assertValue(updatedCards)
    }

    @Test
    fun `timeout if no cards received`() {
        val initialCards = listOf(MASTER_CARD, VISA)
        whenever(getUserCardsUseCase.execute()).doReturn(Single.never())
        val testObserver = useCase.execute(initialCards).test()
        timerScheduler.advanceTimeBy(TIMEOUT.longValue, TIMEOUT.javaUnit)
        testObserver
            .assertError(TimeoutException::class.java)
    }

    @Test
    fun `timeout if cards not updated in 5 seconds`() {
        val initialCards = listOf(MASTER_CARD, VISA)
        whenever(getUserCardsUseCase.execute()).doReturn(Single.just(initialCards))

        val testObserver = useCase.execute(initialCards).test()
        timerScheduler.advanceTimeBy(TIMEOUT.longValue, TIMEOUT.javaUnit)
        testObserver
            .assertError(TimeoutException::class.java)
    }

    @Test
    fun `retry maximum 5 times before timeout`() {
        val initialCards = listOf(MASTER_CARD, VISA)
        whenever(getUserCardsUseCase.execute()).doReturn(Single.just(initialCards))
        var retryCount = 0
        whenever(actualizePaymentOptionsUseCase.execute(NativePaymentTargetService.MARKET, true)).doReturn(
            Completable.complete()
                .doOnSubscribe { retryCount++ }
        )

        val testObserver = useCase.execute(initialCards).test()
        timerScheduler.advanceTimeBy(TIMEOUT.longValue, TIMEOUT.javaUnit)
        assertThat(retryCount).isEqualTo(5)
        testObserver.assertNoValues()
    }

    companion object {

        private val TIMEOUT = 5.seconds

        private val MASTER_CARD = PaymentOption(
            id = "firstCard",
            account = "firstAccount",
            system = "mastercard",
            bankName = BankName.AlfaBank,
            familyInfo = null,
            partnerInfo = null
        )

        private val VISA = PaymentOption(
            id = "secondCard",
            account = "secondAccount",
            system = "visa",
            bankName = BankName.BankOfMoscow,
            familyInfo = null,
            partnerInfo = null
        )

        private val YANDEX_CARD = PaymentOption(
            id = "thirdCard",
            account = "thirdAccount",
            system = "mir",
            bankName = BankName.RaiffeisenBank,
            familyInfo = null,
            partnerInfo = PartnerInfo(isYabankCardOwner = true)
        )
    }
}
