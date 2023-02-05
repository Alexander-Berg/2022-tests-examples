package ru.yandex.supercheck.domain.promocode

import ru.yandex.supercheck.model.domain.promocode.CashbackType
import ru.yandex.supercheck.model.domain.promocode.PromoCode
import ru.yandex.supercheck.model.domain.shops.ShopSpecialization

object TestData {

    const val VKUSVILL_ID = "some_vkusvill_id"
    const val ULYBKA_RADUGI_ID = "some_ulybka_radugi_id"

    val VKUSVILL_PROMO_CODE_15 = PromoCode(
        title = "title",
        description = "description",
        backgroundColor = "#f3161f",
        shopIds = listOf(VKUSVILL_ID),
        code = "VKUSVILL15",
        cashback = CashbackType.Percent("15"),
        shopSpecialization = ShopSpecialization.FOOD_PRODUCTS
    )

    val ULYBKA_RADUGI_PROMO_CODE_100 = PromoCode(
        title = "title",
        description = "description",
        backgroundColor = "#f3161f",
        shopIds = listOf(ULYBKA_RADUGI_ID),
        code = "ULYBKA500",
        cashback = CashbackType.Money("10000"),
        shopSpecialization = ShopSpecialization.HOME_GOODS
    )

    val PROMO_CODES = listOf(VKUSVILL_PROMO_CODE_15, ULYBKA_RADUGI_PROMO_CODE_100)

}