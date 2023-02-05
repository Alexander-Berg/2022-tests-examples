package ru.yandex.yandexbus.inhouse.promocode

import ru.yandex.yandexbus.inhouse.promocode.backend.PromoPartner
import ru.yandex.yandexbus.inhouse.promocode.repo.PromoCode

object PromoCodeTestData {

    val emptyPromoCode = PromoCode(
        id = "",
        title = "",
        description = "",
        link = null,
        code = null,
        terms = "",
        expirationInfo = "",
        partner = PromoPartner(id = "", name = "", icon = null, urlScheme = null, applink = null),
        priority = null,
        logo = null,
        banner = PromoCode.Banner(null, null),
        siteAction = null,
        callAction = null,
        appAction = null,
        shareAction = null
    )

}
