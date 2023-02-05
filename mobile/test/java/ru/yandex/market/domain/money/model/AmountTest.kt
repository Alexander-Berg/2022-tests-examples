package ru.yandex.market.domain.money.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal

class AmountTest {

    @Test
    fun `Zero is zero`() {
        val zeroInstance = Amount.zero()
        val zeroInstance2 = Amount.zero()
        val zeroFromInteger = Amount(0)
        val zeroFromBigDecimal = Amount(BigDecimal(0))
        val zeroFromShortString = Amount(BigDecimal("0.0"))
        val zeroFromLongString = Amount(BigDecimal("0000.000000"))

        assertThat(zeroInstance).isEqualTo(zeroInstance2)
        assertThat(zeroInstance.hashCode()).isEqualTo(zeroInstance2.hashCode())
        assertThat(zeroInstance).isEqualTo(zeroFromInteger)
        assertThat(zeroInstance).isEqualTo(zeroFromBigDecimal)
        assertThat(zeroInstance).isEqualTo(zeroFromShortString)
        assertThat(zeroInstance).isEqualTo(zeroFromLongString)
    }

    @Test
    fun `Amount fixes trailing zero problem`() {
        val decimal1 = BigDecimal("123.456")
        val decimal2 = BigDecimal("123.4560")

        assertThat(decimal1).isNotEqualTo(decimal2)
        assertThat(Amount(decimal1)).isEqualTo(Amount(decimal2))
    }
}