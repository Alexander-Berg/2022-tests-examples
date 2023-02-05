package ru.yandex.supercheck.domain.session

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.domain.session.TestData.EVEN_PAYMENT_COUNT
import ru.yandex.supercheck.domain.session.TestData.ODD_PAYMENT_COUNT
import ru.yandex.supercheck.domain.session.TestData.ZERO_PAYMENT_COUNT
import ru.yandex.supercheck.model.domain.rateme.AppOpening
import ru.yandex.supercheck.model.domain.rateme.RateMeStatus

@RunWith(MockitoJUnitRunner::class)
class SessionInteractorTest {

    private val rateMeRepository = mock<SessionsRepository>()
    private lateinit var sessionInteractor: SessionInteractor

    @Before
    fun setUp() {
        sessionInteractor = SessionInteractor(rateMeRepository)
    }

    @Test
    fun testNeedShowRateMeWhenRatingWasLeft() {
        whenever(rateMeRepository.ratingWasLeft()).thenReturn(Single.just(true))

        val testObserver = sessionInteractor.needShowRateMe().test()

        testObserver.assertResult(false)
    }

    @Test
    fun noMinTimeAppUsage() {
        testNeedShowRateMeOnData(TestData.noMinTimeAppUsage, false)
    }

    @Test
    fun noEnoughSessions() {
        testNeedShowRateMeOnData(TestData.noEnoughSessions, false)
    }

    @Test
    fun firstShowConditions() {
        testNeedShowRateMeOnData(TestData.firstShowConditions, true)
    }

    @Test
    fun afterEnoughCountNotShownSessions() {
        testNeedShowRateMeOnData(TestData.afterEnoughCountNotShownSessions, true)
    }

    @Test
    fun openingInsideShownSession() {
        testNeedShowRateMeOnData(TestData.openingInsideShownSession, true)
    }

    @Test
    fun notEnoughPreviousSessionsCountWithNoneStatus() {
        testNeedShowRateMeOnData(TestData.notEnoughPreviousSessionsCountWithNoneStatus, false)
    }

    @Test
    fun sessionIsSkippedInPreviousAppOpening() {
        testNeedShowRateMeOnData(TestData.sessionIsSkippedInPreviousAppOpening, false)
    }

    @Test
    fun skipIntervalIsPassed() {
        testNeedShowRateMeOnData(TestData.skipIntervalIsPassed, true)
    }

    @Test
    fun skippedOnFirstPeriodNotPassed() {
        testNeedShowRateMeOnData(TestData.skippedOnFirstPeriodNotPassed, false)
    }

    @Test
    fun skippedOnFirstPeriodPassed() {
        testNeedShowRateMeOnData(TestData.skippedOnFirstPeriodPassed, true)
    }

    @Test
    fun skippedOnSecondPeriodNotPassed() {
        testNeedShowRateMeOnData(TestData.skippedOnSecondPeriodNotPassed, false)
    }

    @Test
    fun skippedOnSecondPeriodPassed() {
        testNeedShowRateMeOnData(TestData.skippedOnSecondPeriodPassed, true)
    }

    @Test
    fun skippedThirdTimeNoShow() {
        testNeedShowRateMeOnData(TestData.skippedOnThirdPeriodNotPassed, false)
    }

    @Test
    fun skippedOnThirdPeriodPassed() {
        testNeedShowRateMeOnData(TestData.skippedOnThirdPeriodPassed, true)
    }

    @Test
    fun testNeedShowRateMeOnPaymentInFirstTime() {
        testNeedShowPaymentRateMeOnData(
            successfulPaymentsAmount = ZERO_PAYMENT_COUNT,
            lastShownRateMePaymentsAmount = ZERO_PAYMENT_COUNT,
            needShow = false
        )

        testNeedShowPaymentRateMeOnData(
            successfulPaymentsAmount = ODD_PAYMENT_COUNT,
            lastShownRateMePaymentsAmount = ZERO_PAYMENT_COUNT,
            needShow = false
        )

        testNeedShowPaymentRateMeOnData(
            successfulPaymentsAmount = EVEN_PAYMENT_COUNT,
            lastShownRateMePaymentsAmount = ZERO_PAYMENT_COUNT,
            needShow = true
        )
    }

    @Test
    fun testNeedShowRateMeOnPayment() {

        testNeedShowPaymentRateMeOnData(
            successfulPaymentsAmount = ODD_PAYMENT_COUNT,
            lastShownRateMePaymentsAmount = ODD_PAYMENT_COUNT,
            needShow = false
        )

        testNeedShowPaymentRateMeOnData(
            successfulPaymentsAmount = EVEN_PAYMENT_COUNT,
            lastShownRateMePaymentsAmount = EVEN_PAYMENT_COUNT,
            needShow = false
        )

        testNeedShowPaymentRateMeOnData(
            successfulPaymentsAmount = EVEN_PAYMENT_COUNT,
            lastShownRateMePaymentsAmount = EVEN_PAYMENT_COUNT - 2,
            needShow = true
        )
    }

    private fun testNeedShowPaymentRateMeOnData(
        successfulPaymentsAmount: Int,
        lastShownRateMePaymentsAmount: Int,
        needShow: Boolean
    ) {
        whenever(rateMeRepository.lastShownRateMePaymentsAmount).thenReturn(
            lastShownRateMePaymentsAmount
        )
        whenever(rateMeRepository.ratingWasLeft()).thenReturn(Single.just(false))
        whenever(rateMeRepository.updateRateMeStatus(RateMeStatus.SHOWN)).thenReturn(Completable.complete())

        val testObserver =
            sessionInteractor.needShowRateMeOnSuccessfulPayment(successfulPaymentsAmount).test()

        testObserver.assertResult(needShow)
    }

    private fun testNeedShowRateMeOnData(openings: List<AppOpening>, needShow: Boolean) {
        whenever(rateMeRepository.ratingWasLeft()).thenReturn(Single.just(false))
        whenever(rateMeRepository.getAllOpenings()).thenReturn(Single.just(openings))
        whenever(rateMeRepository.updateRateMeStatus(RateMeStatus.SHOWN)).thenReturn(Completable.complete())

        val testObserver = sessionInteractor.needShowRateMe().test()

        testObserver.assertResult(needShow)
    }

}