package ru.yandex.supercheck.domain.cusdev

import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.supercheck.domain.scanandgo.payment.ScanAndGoPaymentRepository

@RunWith(MockitoJUnitRunner::class)
class CusDevInteractorTest {

    lateinit var cusDevInteractor: CusDevInteractor

    private val scanAndGoPaymentRepository = mock<ScanAndGoPaymentRepository>()
    private val cusDevRepository = mock<CusDevRepository>()

    @Before
    fun setUp() {
        whenever(cusDevRepository.setCusDevSent()).doAnswer { }
        cusDevInteractor = CusDevInteractor(cusDevRepository, scanAndGoPaymentRepository)
    }

    @Test
    fun shouldShowCusDevPopup() {
        testDontShowPopupWithoutPurchases()
        testShowPopupOnFirstPurchase()
        testDontShowPopupIfAlreadyShown()
        testDontShowPopuponEvenPuchaseNumber()
        testShowPopupOnFifthPuchase()
        testDontShowPopupOnMoreThenFifthPurchase()
        testDontShowPopupOnOddUpdatePurchase()
        testShowPopupOnEvenUpdatePurchase()
    }

    private fun testDontShowPopupWithoutPurchases() {
        withCusDevWasSent(false)
        withPurchaseCount(0)
        withLastShownPopupAt(0)
        assertFalse(cusDevInteractor.shouldShowCusDevPopup())
    }

    private fun testShowPopupOnFirstPurchase() {
        withCusDevWasSent(false)
        withPurchaseCount(1)
        withLastShownPopupAt(0)
        assertTrue(cusDevInteractor.shouldShowCusDevPopup())
        verify(cusDevRepository, Mockito.times(1))::lastShownCusDevPaymentsAmount.set(1)
    }

    private fun testDontShowPopupIfAlreadyShown() {
        withCusDevWasSent(false)
        withPurchaseCount(1)
        withLastShownPopupAt(1)
        assertFalse(cusDevInteractor.shouldShowCusDevPopup())
    }

    private fun testDontShowPopuponEvenPuchaseNumber() {
        withCusDevWasSent(false)
        withPurchaseCount(4)
        withLastShownPopupAt(0)
        assertFalse(cusDevInteractor.shouldShowCusDevPopup())
    }

    private fun testShowPopupOnFifthPuchase() {
        withCusDevWasSent(false)
        withPurchaseCount(5)
        withLastShownPopupAt(0)
        assertTrue(cusDevInteractor.shouldShowCusDevPopup())
        verify(cusDevRepository, Mockito.times(1))::lastShownCusDevPaymentsAmount.set(5)
    }

    private fun testDontShowPopupOnMoreThenFifthPurchase() {
        withCusDevWasSent(false)
        withPurchaseCount(7)
        withLastShownPopupAt(0)
        assertFalse(cusDevInteractor.shouldShowCusDevPopup())
    }

    private fun testDontShowPopupOnOddUpdatePurchase() {
        withCusDevWasSent(false)
        withUpdatePurchaseCount(19)
        withPurchaseCount(20)
        withLastShownPopupAt(0)
        assertFalse(cusDevInteractor.shouldShowCusDevPopup())
    }

    private fun testShowPopupOnEvenUpdatePurchase() {
        withCusDevWasSent(false)
        withUpdatePurchaseCount(20)
        withPurchaseCount(21)
        withLastShownPopupAt(0)
        assertTrue(cusDevInteractor.shouldShowCusDevPopup())
    }

    @Test
    fun shouldShowCusDevBanner() {
        testDontShowBannerWithoutPurchases()
        testShowBannerOnThirdPurchase()
        testDontShowBannerOnMoreThenThirteenthPurchase()
        testShowBannerOnSeventhPuchase()
        testDontShowBannerOnOddUpdatePurchase()
        testShowBannerOnEvenUpdatePurchase()
    }

    private fun testDontShowBannerWithoutPurchases() {
        withCusDevWasSent(false)
        withPurchaseCount(0)
        assertFalse(cusDevInteractor.shouldShowCusDevBanner())
    }

    private fun testShowBannerOnThirdPurchase() {
        withCusDevWasSent(false)
        withPurchaseCount(3)
        assertTrue(cusDevInteractor.shouldShowCusDevBanner())
    }

    private fun testDontShowBannerOnMoreThenThirteenthPurchase() {
        withCusDevWasSent(false)
        withPurchaseCount(15)
        assertFalse(cusDevInteractor.shouldShowCusDevBanner())
    }

    private fun testShowBannerOnSeventhPuchase() {
        withCusDevWasSent(true)
        withPurchaseCount(7)
        assertFalse(cusDevInteractor.shouldShowCusDevBanner())
    }

    private fun testDontShowBannerOnOddUpdatePurchase() {
        withCusDevWasSent(false)
        withUpdatePurchaseCount(19)
        withPurchaseCount(22)
        assertFalse(cusDevInteractor.shouldShowCusDevBanner())
    }

    private fun testShowBannerOnEvenUpdatePurchase() {
        withCusDevWasSent(false)
        withUpdatePurchaseCount(20)
        withPurchaseCount(23)
        assertTrue(cusDevInteractor.shouldShowCusDevBanner())
    }

    @Test
    fun setCusDevInviteSent() {
        cusDevInteractor.setCusDevSent()
        verify(cusDevRepository, Mockito.times(1)).setCusDevSent()
    }

    private fun withPurchaseCount(count: Int) {
        whenever(scanAndGoPaymentRepository.successfulPaymentsAmount).thenReturn(count)
    }

    private fun withUpdatePurchaseCount(count: Int) {
        whenever(cusDevRepository.paymentsAmountInUpdate).thenReturn(count)
    }

    private fun withLastShownPopupAt(count: Int) {
        whenever(cusDevRepository.lastShownCusDevPaymentsAmount).thenReturn(count)
    }

    private fun withCusDevWasSent(wasSent: Boolean) {
        whenever(cusDevRepository.cusDevWasSent).thenReturn(wasSent)
    }
}