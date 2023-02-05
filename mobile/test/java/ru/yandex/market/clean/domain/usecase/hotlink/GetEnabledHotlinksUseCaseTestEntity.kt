package ru.yandex.market.clean.domain.usecase.hotlink

import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.clean.domain.model.cms.CmsHotLink
import ru.yandex.market.clean.domain.model.express.EntryPointInfo
import ru.yandex.market.clean.domain.model.express.EntryPoints
import ru.yandex.market.clean.domain.model.lavka2.LavkaAuthData
import ru.yandex.market.clean.domain.model.lavka2.LavkaStartupInfo
import ru.yandex.market.domain.cms.model.content.hotlink.CmsHotLinkType.CATALOG
import ru.yandex.market.domain.cms.model.content.hotlink.CmsHotLinkType.DEEPLINK
import ru.yandex.market.domain.cms.model.content.hotlink.CmsHotLinkType.EXPRESS
import ru.yandex.market.domain.cms.model.content.hotlink.CmsHotLinkType.GROCERIES
import ru.yandex.market.domain.cms.model.content.hotlink.CmsHotLinkType.SUPERMARKET
import ru.yandex.market.domain.cms.model.content.hotlink.CmsHotLinkType.UNKNOWN
import ru.yandex.market.domain.media.model.ImageReference

object GetEnabledHotlinksUseCaseTestEntity {

    private val CMS_HOT_LINK_DEEPLINK = CmsHotLink(ImageReference.empty(), "title", null, DEEPLINK)
    private val CMS_HOT_LINK_GROCERIES = CMS_HOT_LINK_DEEPLINK.copy(type = GROCERIES)
    private val CMS_HOT_LINK_CATALOG = CMS_HOT_LINK_DEEPLINK.copy(type = CATALOG)
    private val CMS_HOT_LINK_EXPRESS = CMS_HOT_LINK_DEEPLINK.copy(type = EXPRESS)
    private val CMS_HOT_LINK_SUPERMARKET = CMS_HOT_LINK_DEEPLINK.copy(type = SUPERMARKET)
    private val CMS_HOT_LINK_UNKNOWN = CMS_HOT_LINK_DEEPLINK.copy(type = UNKNOWN)

    val HOTLINKS = listOf(
        CMS_HOT_LINK_DEEPLINK,
        CMS_HOT_LINK_GROCERIES,
        CMS_HOT_LINK_CATALOG,
        CMS_HOT_LINK_EXPRESS,
        CMS_HOT_LINK_SUPERMARKET,
        CMS_HOT_LINK_UNKNOWN
    )

    val LAVKA_STARTUP_INFO_AVAILABLE = LavkaStartupInfo(
        isLavkaExists = true,
        isLavkaAvailable = true,
        isLavkaComingSoon = false,
        demoLavka = null,
        showOnboarding = false,
        lavkaShopId = null,
        authData = LavkaAuthData.Unauthorized,
        depotId = null,
        lavka24BadgeShowTimeConfig = null,
    )
    val LAVKA_STARTUP_INFO_UNAVAILABLE = LAVKA_STARTUP_INFO_AVAILABLE.copy(
        isLavkaExists = false,
        isLavkaAvailable = false,
    )

    val EXPRESS_ENABLED_ENTRYPOINT = EntryPoints(EntryPointInfo(isEnabled = true, link = HttpAddress.empty()))
    val EXPRESS_DISABLED_ENTRYPOINT = EntryPoints(EntryPointInfo.DISABLED)

    val WITHOUT_SUPERMARKET_HOT_LINKS_PREDICATE: (CmsHotLink) -> Boolean = { it.type == SUPERMARKET }
    val WITHOUT_EXPRESS_GROCERIES_SUPERMARKET_HOT_LINKS_PREDICATE: (CmsHotLink) -> Boolean = {
        it.type == EXPRESS || it.type == GROCERIES || it.type == SUPERMARKET
    }
}
