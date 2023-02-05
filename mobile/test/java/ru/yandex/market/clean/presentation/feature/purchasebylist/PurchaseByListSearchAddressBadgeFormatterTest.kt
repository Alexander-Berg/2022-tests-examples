package ru.yandex.market.clean.presentation.feature.purchasebylist

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.beru.android.R
import ru.yandex.market.clean.presentation.feature.map.formatter.PurchaseByListSearchAddressBadgeFormatter

class PurchaseByListSearchAddressBadgeFormatterTest {

    private val formatter = PurchaseByListSearchAddressBadgeFormatter()

    @Test
    fun `Check correct format by null`() {
        val badge = formatter.format(
            isPrescriptionBadgeVisible = false,
            isOutOfStock = false,
            isDeliveryAvailable = true,
            isAnalogAvailable = true,
        )
        assertThat(badge).isEqualTo(null)
    }

    @Test
    fun `Check correct format with no stock`() {
        val noStock = formatter.format(
            isPrescriptionBadgeVisible = false,
            isOutOfStock = true,
            isDeliveryAvailable = false,
            isAnalogAvailable = false,
        )
        assertThat(noStock).isEqualTo(noStockBadge)
    }

    @Test
    fun `Check correct format with prescription`() {
        val prescription = formatter.format(
            isPrescriptionBadgeVisible = true,
            isOutOfStock = false,
            isDeliveryAvailable = true,
            isAnalogAvailable = true,
        )
        assertThat(prescription).isEqualTo(prescriptionBadge)
    }

    @Test
    fun `Check correct format with analog available`() {
        val analogs = formatter.format(
            isPrescriptionBadgeVisible = false,
            isOutOfStock = true,
            isDeliveryAvailable = true,
            isAnalogAvailable = true,
        )
        assertThat(analogs).isEqualTo(analogsBadge)
    }

    companion object {
        private const val prescriptionBadge = R.drawable.ic_only_outlet_available
        private const val analogsBadge = R.drawable.ic_analog_available
        private const val noStockBadge = R.drawable.ic_pharma_no_stock
    }
}