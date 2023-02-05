package ru.yandex.market.clean.data.mapper

import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.model.dto.promotionDtoTestInstance
import ru.yandex.market.clean.domain.model.Promotion
import ru.yandex.market.data.order.options.PromoType
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.net.requestContextTestInstance
import ru.yandex.market.rub
import ru.yandex.market.utils.asExceptional
import java.math.BigDecimal

class PromotionMapperTest {
    private val typeMapper = mock<PromoTypeMapper>()
    private val moneyMapper = mock<MoneyMapper>()
    private val mapper = PromotionMapper(typeMapper, moneyMapper)

    @Test
    fun `Maps normal promotion`() {
        val requestContext = requestContextTestInstance()
        val testDto = promotionDtoTestInstance(
            buyerDiscount = BigDecimal.TEN,
            deliveryDiscount = BigDecimal.ZERO,
            promoCode = "promoCode"
        )
        whenever(moneyMapper.map(testDto.buyerDiscount, requestContext.currency))
            .thenReturn(10.rub.asExceptional())
        whenever(moneyMapper.map(testDto.deliveryDiscount, requestContext.currency))
            .thenReturn(0.rub.asExceptional())
        whenever(typeMapper.map(anyOrNull())).thenReturn(PromoType.MARKET_BLUE)

        val mapped = mapper.map(testDto, requestContext)

        val expectedResult = Promotion(
            type = PromoType.MARKET_BLUE,
            buyerDiscount = 10.rub,
            deliveryDiscount = 0.rub,
            promoCode = "promoCode"
        )
        assertThat(mapped).extracting { it.get() }.isEqualTo(expectedResult)
    }

    @Test
    fun `Returns error when both buyer and delivery discount are zero after mapping`() {
        val requestContext = requestContextTestInstance()
        val testDto = promotionDtoTestInstance(buyerDiscount = BigDecimal.TEN, deliveryDiscount = BigDecimal.ZERO)
        whenever(moneyMapper.map(anyOrNull<BigDecimal>(), anyOrNull<Currency>()))
            .thenReturn(0.rub.asExceptional())

        val mapped = mapper.map(testDto, requestContext)

        assertThat(mapped).extracting { it.exception }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `Returns error when failed to map both buyer and delivery discounts`() {
        val requestContext = requestContextTestInstance()
        val testDto = promotionDtoTestInstance(buyerDiscount = BigDecimal.TEN, deliveryDiscount = BigDecimal.ZERO)
        whenever(moneyMapper.map(anyOrNull<BigDecimal>(), anyOrNull<Currency>())).thenReturn(
            Exceptional.of(RuntimeException())
        )

        val mapped = mapper.map(testDto, requestContext)

        assertThat(mapped).extracting { it.exception }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
