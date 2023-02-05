package ru.yandex.supercheck.domain.preload

import ru.yandex.supercheck.core.glide.image.ImageUri
import ru.yandex.supercheck.data.stubs.StubFactory
import ru.yandex.supercheck.domain.error.ServerResponseError
import ru.yandex.supercheck.model.domain.address.AddressDomain
import ru.yandex.supercheck.model.domain.address.AddressName
import ru.yandex.supercheck.model.domain.address.AddressState
import ru.yandex.supercheck.model.domain.category.CategoryDomain
import ru.yandex.supercheck.model.domain.category.ShopCategoryListDomain
import ru.yandex.supercheck.model.domain.common.GeoLocation
import ru.yandex.supercheck.model.domain.scanandgo.ScanAndGoShopsResult
import ru.yandex.supercheck.model.domain.shops.LoyaltyBindingType
import ru.yandex.supercheck.model.domain.shops.RetailerInfo
import ru.yandex.supercheck.model.domain.shops.ShopsLoadingResult
import ru.yandex.supercheck.model.domain.shops.ShopsWithCurrentAddressAndState

object TestData {

    val SHOPS = Pair(
        CategoryDomain(
            id = "",
            name = "",
            promoCategories = null,
            childCategories = null,
            alcoCategory = false
        ),
        ShopsWithCurrentAddressAndState(
            ShopsLoadingResult(
                emptyList(),
                ShopCategoryListDomain(
                    emptyList(), emptyList()
                ),
                null,
                null
            ),
            AddressDomain(GeoLocation.EMPTY_GEO_LOCATION, AddressName("", "")),
            AddressState.Location
        )
    )

    val SCAN_AND_GO_SHOPS = ScanAndGoShopsResult(
        mapOf(
            RetailerInfo(
                "",
                "",
                ImageUri.Empty,
                null,
                "",
                null,
                LoyaltyBindingType.PHONE_WITH_OPTIONAL_CONFIRMATION,
                shopChain = null,
                scanAndGoBanner = null,
                benefitColor = null
            ) to listOf(StubFactory.getShopDomain())
        ),
        emptyMap(),
        emptyMap(),
        true
    )

    val SERVER_RESPONSE_ERROR = ServerResponseError(
        "server",
        "500",
        "Server error",
        "requestId"
    )


}