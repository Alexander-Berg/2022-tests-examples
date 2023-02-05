package ru.yandex.market.test.util

import ru.yandex.market.utils.Characters
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

object PriceUtils {

    @Deprecated(
        "Используйте Int.toRubPriceString() для получения цены",
        ReplaceWith("price.toRubPriceString()", "ru.yandex.market.test.util.PriceUtils.toRubPriceString")
    )
    fun getPriceText(price: Int): String {
        return price.toRubPriceString()
    }

    fun Int.toRubPriceString(): String {
        return if (this == 0) FREE else """${this}$CURRENCY_SEPARATOR$CURRENCY_RUB"""
    }

    fun Long.toRubPriceString(): String {
        return if (this == 0L) FREE else """${this}$CURRENCY_SEPARATOR$CURRENCY_RUB"""
    }

    fun BigDecimal.toRubPriceString(): String {
        return if (this == BigDecimal.ZERO) FREE else """${this}$CURRENCY_SEPARATOR$CURRENCY_RUB"""
    }

    fun Long.toDecimalRubPriceString(): String {
        return if (this == 0L) FREE else "${DECIMAL_FORMAT.format(this)}$CURRENCY_SEPARATOR$CURRENCY_RUB"
    }

    private const val CURRENCY_SEPARATOR = Characters.NARROW_NO_BREAK_SPACE
    private const val CURRENCY_RUB = Characters.RUBLE_SIGN
    private const val FREE = "бесплатно"
    private val DECIMAL_FORMAT_SYMBOLS = DecimalFormatSymbols().apply {
        groupingSeparator = Characters.NARROW_NO_BREAK_SPACE
    }
    private val DECIMAL_FORMAT = DecimalFormat("#,##0.##", DECIMAL_FORMAT_SYMBOLS)
}