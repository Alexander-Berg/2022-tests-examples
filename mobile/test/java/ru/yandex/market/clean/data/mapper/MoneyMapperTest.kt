package ru.yandex.market.clean.data.mapper

import junit.framework.TestCase
import org.junit.Test
import ru.yandex.market.clean.domain.model.price.Price
import ru.yandex.market.data.money.mapper.CurrencyMapper
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.data.money.parser.MoneyAmountParser
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.utils.orNull
import java.math.BigDecimal

class MoneyMapperTest : TestCase() {

    private val moneyMapper =
        MoneyMapper(MoneyAmountParser(), CurrencyMapper())

    @Test
    fun `test map Price`() {
        val price =
            Price(BigDecimal.valueOf(20), Currency.RUR)
        val expected = Money.createRub(20)
        assertEquals(expected, moneyMapper.map(price).orNull)
    }

    @Test
    fun `test map text`() {
        val expected = Money(BigDecimal.valueOf(250), Currency.KZT)
        assertEquals(expected, moneyMapper.map("250", "KZT").orNull)
    }

    @Test
    fun `test map float_currency`() {
        val expected = Money(BigDecimal.valueOf(300), Currency.BYR)
        assertEquals(expected, moneyMapper.map(300f, Currency.BYR))
    }

    @Test
    fun `test map big_decimal`() {
        val expected = Money.createRub(10000)
        assertEquals(expected, moneyMapper.map(BigDecimal.valueOf(10000)).orNull)
    }

    @Test
    fun `test map big_decimal_string`() {
        val expected = Money.createRub(300)
        assertEquals(expected, moneyMapper.map(BigDecimal.valueOf(300), "RUR").orNull)
    }

    @Test
    fun `test map big_decimal_currency`() {
        val expected = Money(BigDecimal.valueOf(40), Currency.UAH)
        assertEquals(expected, moneyMapper.map(BigDecimal.valueOf(40), Currency.UAH).orNull)
    }
}
