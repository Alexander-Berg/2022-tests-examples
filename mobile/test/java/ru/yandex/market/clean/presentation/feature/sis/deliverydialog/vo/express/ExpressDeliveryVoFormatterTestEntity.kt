package ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express

import ru.beru.android.R
import ru.yandex.market.clean.domain.model.delivery.DeliveryConditionsRangeSpecialOffers
import ru.yandex.market.clean.domain.model.delivery.DeliveryConditionsRangeSpecialPrices
import ru.yandex.market.clean.domain.model.delivery.deliveryConditionsTestInstance
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.DeliveryVo

object ExpressDeliveryVoFormatterTestEntity {

    const val EXPRESS_DELIVERY_TITLE = "Доставка за 1–2 часа"
    const val DELIVERY_ORDER_ANY_PRICE = "Заказ на любую сумму"
    const val DELIVERY_FOR_PLUS_TITLE = "Для тех, кто в Плюсе"
    const val CURRENCY_RUR = "Р"
    private const val FROM = "от"
    private const val TO = "до"

    val DELIVERY_CONDITIONS = deliveryConditionsTestInstance()

    private val DELIVERY_CONDITIONS_RANGE_SPECIAL_OFFERS_WITH_NULL_PRICES = DeliveryConditionsRangeSpecialOffers(
        minPrice = null,
        maxPrice = null
    )

    private val DELIVERY_CONDITIONS_RANGE_SPECIAL_NULL_PRICES = DeliveryConditionsRangeSpecialPrices(
        common = listOf(DELIVERY_CONDITIONS_RANGE_SPECIAL_OFFERS_WITH_NULL_PRICES)
    )

    private val DELIVERY_CONDITIONS_RANGE_SPECIAL_WITH_EMPTY_PRICES = DeliveryConditionsRangeSpecialPrices(
        common = emptyList()
    )

    private val DELIVERY_CONDITIONS_TYPES_EXPRESS_WITH_NULL_PRICES = DELIVERY_CONDITIONS.types.express.copy(
        prices = DELIVERY_CONDITIONS_RANGE_SPECIAL_NULL_PRICES
    )

    private val DELIVERY_CONDITIONS_TYPES_EXPRESS_WITH_EMPTY_PRICES = DELIVERY_CONDITIONS.types.express.copy(
        prices = DELIVERY_CONDITIONS_RANGE_SPECIAL_WITH_EMPTY_PRICES
    )

    private val DELIVERY_CONDITIONS_TYPES_WITH_EXPRESS_NULL_PRICES = DELIVERY_CONDITIONS.types.copy(
        express = DELIVERY_CONDITIONS_TYPES_EXPRESS_WITH_NULL_PRICES
    )

    private val DELIVERY_CONDITIONS_TYPES_WITH_EXPRESS_EMPTY_PRICES = DELIVERY_CONDITIONS.types.copy(
        express = DELIVERY_CONDITIONS_TYPES_EXPRESS_WITH_EMPTY_PRICES
    )

    val DELIVERY_CONDITIONS_WITH_EXPRESS_NULL_PRICES = DELIVERY_CONDITIONS.copy(
        types = DELIVERY_CONDITIONS_TYPES_WITH_EXPRESS_NULL_PRICES
    )

    val DELIVERY_CONDITIONS_WITH_EXPRESS_EMPTY_PRICES = DELIVERY_CONDITIONS.copy(
        types = DELIVERY_CONDITIONS_TYPES_WITH_EXPRESS_EMPTY_PRICES
    )

    private val DELIVERY_CONDITIONS_SPECIAL_PRICES_WITH_EMPTY_EXPRESS =
        DELIVERY_CONDITIONS.specials.yaPlus.conditions.prices.copy(express = emptyList())

    private val DELIVERY_CONDITIONS_SPECIAL_CONDITIONS_WITH_PLUS_EMPTY_PRICES =
        DELIVERY_CONDITIONS.specials.yaPlus.conditions.copy(
            prices = DELIVERY_CONDITIONS_SPECIAL_PRICES_WITH_EMPTY_EXPRESS
        )

    private val DELIVERY_CONDITIONS_SPECIAL_WITH_PLUS_EMPTY_PRICES = DELIVERY_CONDITIONS.specials.yaPlus.copy(
        conditions = DELIVERY_CONDITIONS_SPECIAL_CONDITIONS_WITH_PLUS_EMPTY_PRICES
    )

    private val DELIVERY_CONDITIONS_SPECIALS_WITH_PLUS_EMPTY_PRICES = DELIVERY_CONDITIONS.specials.copy(
        yaPlus = DELIVERY_CONDITIONS_SPECIAL_WITH_PLUS_EMPTY_PRICES
    )

    val DELIVERY_CONDITIONS_WITH_PLUS_EMPTY_PRICES = DELIVERY_CONDITIONS.copy(
        specials = DELIVERY_CONDITIONS_SPECIALS_WITH_PLUS_EMPTY_PRICES
    )

    val MONEY_DELIVERY_CONDITIONS = DELIVERY_CONDITIONS.types.express.prices.common[0].minPrice?.money
    val AMOUNT_VALUE = MONEY_DELIVERY_CONDITIONS?.amount?.value ?: 0
    val PRICE_WITH_CURRENCY = "$AMOUNT_VALUE $CURRENCY_RUR"
    val DELIVERY_PRICE_RANGE = "$FROM $PRICE_WITH_CURRENCY $TO $PRICE_WITH_CURRENCY"

    private val ANY_PRICE_DELIVERY = ExpressDeliveryGroupElementVo(
        title = DELIVERY_ORDER_ANY_PRICE,
        price = DELIVERY_PRICE_RANGE,
        icon = null
    )

    private val YA_PLUS_DELIVERY = ExpressDeliveryGroupElementVo(
        title = DELIVERY_FOR_PLUS_TITLE,
        price = DELIVERY_PRICE_RANGE,
        icon = R.drawable.ic_plus_badge_round_12
    )

    val EXPRESS_DELIVERY_VO = DeliveryVo.ExpressDeliveryVo(
        title = EXPRESS_DELIVERY_TITLE,
        deliveryItems = listOf(ANY_PRICE_DELIVERY, YA_PLUS_DELIVERY)
    )

    val EXPRESS_DELIVERY_VO_WITHOUT_ANY_PRICE_DELIVERY = EXPRESS_DELIVERY_VO.copy(
        deliveryItems = listOf(YA_PLUS_DELIVERY)
    )

    val EXPRESS_DELIVERY_VO_WITHOUT_YA_PLUS_DELIVERY = EXPRESS_DELIVERY_VO.copy(
        deliveryItems = listOf(ANY_PRICE_DELIVERY)
    )

}