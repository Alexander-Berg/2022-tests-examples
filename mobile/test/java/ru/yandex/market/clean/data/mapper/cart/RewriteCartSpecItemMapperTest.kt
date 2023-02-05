package ru.yandex.market.clean.data.mapper.cart

import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.clean.data.model.db.cartItemEntityTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.clean.domain.usecase.cart.PrepareCartRewriteSpecsUseCase
import ru.yandex.market.domain.money.model.Currency

class RewriteCartSpecItemMapperTest {

    private val mapper = RewriteCartSpecItemMapper()

    @Test
    fun `check sku id`() {
        val res = mapper.map(
            cartItemTestInstance(),
            orderItemTestInstance(skuId = "123")
        )
        Assertions.assertThat(res.skuId).isEqualTo("123")
    }

    @Test
    fun `check sku id 2`() {
        val res = mapper.map(
            cartItemTestInstance(skuId = "123"),
        )
        Assertions.assertThat(res.skuId).isEqualTo("123")
    }

    @Test
    fun `check sku id 3`() {
        val res = mapper.map(
            cartItemEntityTestInstance(marketSku = "123", currency = Currency.RUR.name, value = "100"),
        )
        Assertions.assertThat(res.skuId).isEqualTo("123")
    }

    @Test
    fun `check sku id 4`() {
        val res = mapper.map(
            cartItemTestInstance(),
            orderItemTestInstance(skuId = "123"),
            emptyList(),
            PrepareCartRewriteSpecsUseCase.Configuration(emptySet())
        )
        Assertions.assertThat(res.skuId).isEqualTo("123")
    }
}