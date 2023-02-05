package ru.yandex.market.data.payment.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.data.payment.network.dto.YandexCardInfoDto
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.payment.model.YandexCardInfo
import java.math.BigDecimal

class YandexCardInfoMapperTest {

    private val mapper = YandexCardInfoMapper()

    @Test
    fun `null dto map as no limits`() {
        assertThat(
            mapper.map(null)
        ).isEqualTo(
            YandexCardInfo.NoLimited
        )
    }

    @Test
    fun `map as no limits if missed yandexCardPaymentAllowed property`() {
        assertThat(
            mapper.map(YandexCardInfoDto(yandexCardPaymentAllowed = null, limit = BigDecimal.TEN))
        ).isEqualTo(
            YandexCardInfo.NoLimited
        )
    }

    @Test
    fun `map as no limits if missed limit property`() {
        assertThat(
            mapper.map(YandexCardInfoDto(yandexCardPaymentAllowed = true, limit = null))
        ).isEqualTo(
            YandexCardInfo.NoLimited
        )
    }

    @Test
    fun `map limited`() {
        assertThat(
            mapper.map(YandexCardInfoDto(yandexCardPaymentAllowed = true, limit = BigDecimal.TEN))
        ).isEqualTo(
            YandexCardInfo.Limited(yandexPaymentAllowed = true, paymentLimit = Money.createRub(BigDecimal.TEN))
        )
    }
}
