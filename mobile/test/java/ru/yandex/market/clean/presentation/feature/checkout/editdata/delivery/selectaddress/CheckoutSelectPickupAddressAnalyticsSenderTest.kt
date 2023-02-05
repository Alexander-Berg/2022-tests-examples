package ru.yandex.market.clean.presentation.feature.checkout.editdata.delivery.selectaddress

import org.junit.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.health.HealthPortion
import ru.yandex.market.analytics.speed.SpeedService
import ru.yandex.market.clean.presentation.feature.checkout.editdata.delivery.selectaddress.CheckoutSelectAddressSpeedAnalyticsSender.Companion.PICKUP_ADDRESS_CHANGED
import ru.yandex.market.domain.delivery.model.DeliveryType

class CheckoutSelectPickupAddressAnalyticsSenderTest {
    private val speedService = mock<SpeedService>()
    private val checkoutSelectAddressAnalyticsSender =
        CheckoutSelectAddressSpeedAnalyticsSender(speedService, DeliveryType.PICKUP)

    @Test
    fun `execute startEvent on SpeedService then call startSavingSelectedAddress`() {
        whenever(
            speedService.startEvent(
                PICKUP_ADDRESS_CHANGED,
                HealthPortion.CHECKOUT_V2,
                isMainComponent = false
            )
        ).thenReturn("")

        checkoutSelectAddressAnalyticsSender.startSavingSelectedAddress()
        verify(speedService).startEvent(
            PICKUP_ADDRESS_CHANGED,
            HealthPortion.CHECKOUT_V2,
            isMainComponent = false
        )
    }

    @Test
    fun `execute stopEvent on SpeedService then call startSavingSelectedAddress`() {
        doNothing().`when`(
            speedService
        ).stopEvent(
            PICKUP_ADDRESS_CHANGED,
            HealthPortion.CHECKOUT_V2
        )

        checkoutSelectAddressAnalyticsSender.stopSavingSelectedAddress()
        verify(speedService).stopEvent(
            PICKUP_ADDRESS_CHANGED,
            HealthPortion.CHECKOUT_V2
        )
    }

    @Test
    fun `execute removeEvent on SpeedService then call startSavingSelectedAddress`() {
        doNothing().`when`(
            speedService
        ).removeEvent(
            PICKUP_ADDRESS_CHANGED,
            HealthPortion.CHECKOUT_V2
        )

        checkoutSelectAddressAnalyticsSender.cancelSavingSelectedAddress()
        verify(speedService).removeEvent(
            PICKUP_ADDRESS_CHANGED,
            HealthPortion.CHECKOUT_V2
        )
    }
}