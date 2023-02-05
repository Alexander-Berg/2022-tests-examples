package ru.yandex.market.test.models

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import ru.beru.android.R
import ru.yandex.market.utils.Characters.NARROW_NO_BREAK_SPACE
import ru.yandex.market.utils.Characters.NON_BREAKING_SPACE
import ru.yandex.market.utils.Characters.RUBLE_SIGN

sealed class SummaryRowItemDescription(
    val title: String,
    val value: String,
    @DrawableRes val titleIcon: Int? = null,
    @ColorRes val valueColor: Int = DEFAULT_VALUE_COLOR
) {
    class BasePrice(price: String, itemsCount: Int) : SummaryRowItemDescription(
        title = "$SUMMARY_BASE_PRICE - $itemsCount шт.",
        value = price
    )

    class Discount(discount: String) : SummaryRowItemDescription(
        title = SUMMARY_DISCOUNT,
        value = discount,
        valueColor = DISCOUNT_VALUE_COLOR
    )

    class MonthlyPayment(monthlyPaymentSum: String) : SummaryRowItemDescription(
        title = CREDIT_PAYMENT,
        value = "от $monthlyPaymentSum$MONTHLY_PAYMENT_STRING",
        titleIcon = null,
        valueColor = DEFAULT_VALUE_COLOR
    )

    class PriceDropDiscount(discount: String) : SummaryRowItemDescription(
        title = SUMMARY_PRICE_DROP,
        value = discount,
        valueColor = PRICE_DROP_VALUE_COLOR
    )

    class SmartCoinsDiscount(discount: String) : SummaryRowItemDescription(
        title = SUMMARY_SMART_COINS,
        value = discount,
        valueColor = SMART_COINS_VALUE_COLOR
    )

    class CouponDiscount(discount: String) : SummaryRowItemDescription(
        title = SUMMARY_COUPON,
        value = discount,
        valueColor = COUPON_VALUE_COLOR
    )

    class DeliveryPrice(
        deliveryType: String,
        discount: String,
        color: Int = DEFAULT_VALUE_COLOR
    ) : SummaryRowItemDescription(
        title = deliveryType,
        value = discount,
        valueColor = color
    )

    class Total(totalCoast: String) : SummaryRowItemDescription(title = SUMMARY_TOTAL_PRICE, value = totalCoast)

    class CashbackEmit(amount: String, isDelivered: Boolean) : SummaryRowItemDescription(
        title = if (isDelivered) SUMMARY_EMIT_CASHBACK_DELIVERED else SUMMARY_EMIT_CASHBACK,
        value = amount,
        titleIcon = SUMMARY_CASHBACK_ICON,
        valueColor = SUMMARY_CASHBACK_VALUE_COLOR
    )

    class RiseFloor(value: String, supplierName: String?) : SummaryRowItemDescription(
        title = if (supplierName != null) "$RISE_FLOOR $supplierName" else RISE_FLOOR,
        value = value
    )

    class HelpIsNearDonation(amount: Int) : SummaryRowItemDescription(
        title = HELP_IS_NEAR,
        value = "$amount$NARROW_NO_BREAK_SPACE$RUBLE_SIGN",
        titleIcon = SUMMARY_HELP_IS_NEAR_ICON
    )

    class ServicesPrice(price: String, itemsCount: Int) : SummaryRowItemDescription(
        title = SUMMARY_SERVICE_PRICE.format(itemsCount),
        value = price
    )

    class Medicine(price: String, itemsCount: Int) : SummaryRowItemDescription(
        title = SUMMARY_MEDICINE.format(itemsCount),
        value = price
    )

    companion object {
        private const val SUMMARY_BASE_PRICE = "Товары"
        private const val SUMMARY_DISCOUNT = "Скидка на товары"
        private const val SUMMARY_PRICE_DROP = "Скидка по акции"
        private const val SUMMARY_SMART_COINS = "Купоны"
        private const val SUMMARY_COUPON = "Скидка по промокоду"
        private const val SUMMARY_TOTAL_PRICE = "Итого"
        private const val SUMMARY_EMIT_CASHBACK = "Вернётся на Плюс"
        private const val SUMMARY_EMIT_CASHBACK_DELIVERED = "Начислено на Плюс"
        private const val CREDIT_PAYMENT = "В кредит от Тинькофф"
        private const val RISE_FLOOR = "Подъём на этаж"
        private const val HELP_IS_NEAR = "И ещё в «Помощь рядом»"
        private const val SUMMARY_SERVICE_PRICE = "Установка (%d)"
        private const val SUMMARY_MEDICINE = "Лекарства (%d)"
        private const val MONTHLY_PAYMENT_STRING = "$NON_BREAKING_SPACE$RUBLE_SIGN / мес."
        private const val DISCOUNT_VALUE_COLOR = R.color.red
        private const val PRICE_DROP_VALUE_COLOR = R.color.red
        private const val COUPON_VALUE_COLOR = R.color.red
        private const val SMART_COINS_VALUE_COLOR = R.color.purple
        private const val SUMMARY_PRICE_DROP_ICON = R.drawable.ic_drop_price_13
        private const val SUMMARY_CASHBACK_ICON = R.drawable.ic_question_cashback_summary
        private const val SUMMARY_HELP_IS_NEAR_ICON = R.drawable.ic_help_is_near_color_16
        private const val DEFAULT_VALUE_COLOR = R.color.black
        private const val SUMMARY_CASHBACK_VALUE_COLOR = R.color.moderate_purple_red
    }
}
