package com.yandex.mobile.realty.ui.userOffers

import com.yandex.mobile.realty.RobolectricTest
import com.yandex.mobile.realty.adapter.formatOfferStatus
import com.yandex.mobile.realty.domain.model.user.PlacementStatus
import com.yandex.mobile.realty.domain.model.user.UserOfferStatus
import com.yandex.mobile.realty.domain.model.user.Warning
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * @author rogovalex on 2020-04-22.
 */
@RunWith(RobolectricTestRunner::class)
class UserOfferStatusViewStateTest : RobolectricTest() {

    @Test
    fun testFreeOfferPublishedExpiresToday() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Free),
            setOf(Warning.EXPIRES_SOON),
            0
        )
        assertEquals("Менее суток до снятия", state.text)
    }

    @Test
    fun testFreeOfferPublishedExpiresSoon() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Free),
            setOf(Warning.EXPIRES_SOON),
            5
        )
        assertEquals("5 дней до снятия", state.text)
    }

    @Test
    fun testFreeOfferPublishedNoPhoto() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Free),
            setOf(Warning.NO_PHOTO),
            100
        )
        assertEquals("Активно", state.text)
    }

    @Test
    fun testFreeOfferPublishedActive() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Free),
            setOf(),
            100
        )
        assertEquals("Активно", state.text)
    }

    @Test
    fun testPaidOfferPublishedIgnoreExpiresSoon() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Paid),
            setOf(Warning.EXPIRES_SOON),
            0
        )
        assertEquals("Активно", state.text)
    }

    @Test
    fun testPaidOfferPublishedNoPhoto() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Paid),
            setOf(Warning.NO_PHOTO),
            100
        )
        assertEquals("Активно", state.text)
    }

    @Test
    fun testPaidOfferActive() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Paid),
            setOf(),
            100
        )
        assertEquals("Активно", state.text)
    }

    @Test
    fun testUnpaidOfferPublishedActivationRequired() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.Unpaid),
            setOf(),
            100
        )
        assertEquals("Требует активации", state.text)
    }

    @Test
    fun testJuridicalUnpaidOfferPublishedActivationRequired() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.JuridicalUnpaid(true)),
            setOf(),
            100
        )
        assertEquals("Требует активации", state.text)
    }

    @Test
    fun testPaymentInProcessOfferPublished() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Published(PlacementStatus.PaymentInProcess),
            setOf(),
            100
        )
        assertEquals("В процессе оплаты", state.text)
    }

    @Test
    fun testBannedOffer() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Banned(PlacementStatus.Free, listOf()),
            setOf(),
            100
        )
        assertEquals("Заблокировано", state.text)
    }

    @Test
    fun testFreeOfferInactive() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Inactive(PlacementStatus.Free),
            setOf(Warning.EXPIRES_SOON),
            100
        )
        assertEquals("Снято с публикации", state.text)
    }

    @Test
    fun testPaidOfferInactive() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Inactive(PlacementStatus.Paid),
            setOf(Warning.NO_PHOTO),
            100
        )
        assertEquals("Снято с публикации", state.text)
    }

    @Test
    fun testUnpaidOfferInactiveActivationRequired() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Inactive(PlacementStatus.Unpaid),
            setOf(),
            100
        )
        assertEquals("Требует активации", state.text)
    }

    @Test
    fun testUnpaidJuridicalOfferInactiveActivationRequired() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Inactive(PlacementStatus.JuridicalUnpaid(true)),
            setOf(),
            100
        )
        assertEquals("Требует активации", state.text)
    }

    @Test
    fun testPaymentInProcessOfferInactive() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Inactive(PlacementStatus.PaymentInProcess),
            setOf(),
            100
        )
        assertEquals("В процессе оплаты", state.text)
    }

    @Test
    fun testFreeOfferOnModeration() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Moderation(PlacementStatus.Free),
            setOf(Warning.NO_PHOTO),
            100
        )
        assertEquals("На публикации", state.text)
    }

    @Test
    fun testPaidOfferOnModeration() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Moderation(PlacementStatus.Paid),
            setOf(Warning.EXPIRES_SOON),
            100
        )
        assertEquals("На публикации", state.text)
    }

    @Test
    fun testUnpaidOfferOnModerationActivationRequired() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Moderation(PlacementStatus.Unpaid),
            setOf(),
            100
        )
        assertEquals("Требует активации", state.text)
    }

    @Test
    fun testUnpaidJuridicalOfferOnModerationActivationRequired() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Moderation(PlacementStatus.JuridicalUnpaid(true)),
            setOf(),
            100
        )
        assertEquals("Требует активации", state.text)
    }

    @Test
    fun testPaymentInProcessOfferOnModeration() {
        val state = formatOfferStatus(
            context,
            UserOfferStatus.Moderation(PlacementStatus.PaymentInProcess),
            setOf(),
            100
        )
        assertEquals("В процессе оплаты", state.text)
    }
}
