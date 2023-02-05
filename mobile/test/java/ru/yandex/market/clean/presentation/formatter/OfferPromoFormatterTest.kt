package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.base.network.common.address.httpAddressTestInstance
import ru.yandex.market.clean.data.mapper.cms.CmsProductPromoCodeMapper
import ru.yandex.market.clean.domain.model.OfferPromo
import ru.yandex.market.clean.domain.model.OfferPromoInfo
import ru.yandex.market.clean.domain.model.offerPromoTestInstance
import ru.yandex.market.clean.domain.model.offerPromo_CheapestAsGiftTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.about.AboutCashbackInfoTypeArgumentMapper
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.common.dateformatter.DateFormatter
import ru.yandex.market.common.featureconfigs.managers.DirectDiscountConditionsToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.feature.manager.KingBadgeFeatureManager
import ru.yandex.market.feature.manager.ParentPromoBadgeFeatureManager
import ru.yandex.market.feature.manager.PromoCodeInTotalDiscountFeatureManager
import java.util.Date

class OfferPromoFormatterTest {

    private val moneyFormatter = mock<MoneyFormatter>()
    private val pricesFormatter = mock<PricesFormatter>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val dateFormatter = mock<DateFormatter>()
    private val cmsProductPromoCodeMapper = mock<CmsProductPromoCodeMapper>()
    private val promoCodeInTotalDiscountFeatureManager = mock<PromoCodeInTotalDiscountFeatureManager> {
        on { isPromoCodeInTotalDiscountEnabled(any(), any()) } doReturn false
    }
    private val discountConditionsToggleManager = mock<DirectDiscountConditionsToggleManager> {
        on { getFromCacheOrDefault() } doReturn FeatureToggle(false)
    }
    private val parentPromoBadgeFeatureManager = mock<ParentPromoBadgeFeatureManager> {
        on { isEnabled() } doReturn false
    }
    private val kingBadgeFeatureManager = mock<KingBadgeFeatureManager> {
        on { isEnabled() } doReturn false
    }
    private val aboutCashbackInfoTypeArgumentMapper = mock<AboutCashbackInfoTypeArgumentMapper>()

    private val formatter = OfferPromoFormatter(
        moneyFormatter = moneyFormatter,
        pricesFormatter = pricesFormatter,
        resourcesManager = resourcesDataStore,
        dateFormatter = dateFormatter,
        cmsProductPromoCodeMapper = cmsProductPromoCodeMapper,
        directDiscountConditionsToggleManager = discountConditionsToggleManager,
        promoCodeInTotalDiscountFeatureManager = promoCodeInTotalDiscountFeatureManager,
        kingBadgeFeatureManager = kingBadgeFeatureManager,
        parentPromoBadgeFeatureManager = parentPromoBadgeFeatureManager,
        aboutCashbackInfoTypeArgumentMapper = aboutCashbackInfoTypeArgumentMapper,
    )

    @Test
    fun `Return CheapestAsGift view object when promo bundle size equals 3`() {
        assertThat(formatter.formatCheapestAsGift(offerPromo_CheapestAsGiftTestInstance(bundleSize = 3))).isNotNull
    }

    @Test
    fun `Return CheapestAsGift view object when promo bundle size equals 4`() {
        assertThat(formatter.formatCheapestAsGift(offerPromo_CheapestAsGiftTestInstance(bundleSize = 4))).isNotNull
    }

    @Test
    fun `Return null when promo bundle size equals 5`() {
        assertThat(formatter.formatCheapestAsGift(offerPromo_CheapestAsGiftTestInstance(bundleSize = 5))).isNotNull
    }

    @Test
    fun `Return DirectDiscount view object when conditions in DirectDiscount equals null`() {
        val directDiscount = OfferPromo.DirectDiscount(
            conditions = "conditions",
            landingUrl = httpAddressTestInstance(),
            anaplanId = "anaplanId",
            termsUrl = httpAddressTestInstance(),
            shopPromoId = "shopPromoId",
            key = "key",
            promoKey = "promoKey",
            isPersonal = false,
            endDate = Date(),
            personalDiscountPrices = null,
            parentPromoId = null,
        )
        assertThat(formatter.formatDirectDiscount(directDiscount)).isNotNull
    }

    @Test
    fun `Return OfferPromo view object for productOffer and offerPromos list`() {
        assertThat(
            formatter.format(
                productOfferTestInstance(),
                OfferPromoInfo(
                    listOf(
                        offerPromoTestInstance(),
                        OfferPromo.testInstancePriceDrop(),
                        OfferPromo.GiftAdditionalItem(
                            key = "key",
                            anaplanId = "anaplanId",
                            termsUrl = httpAddressTestInstance(),
                            landingUrl = httpAddressTestInstance(),
                            shopPromoId = "shopPromoId",
                            promoKey = "promoKey",
                            parentPromoId = null,
                        ),
                        OfferPromo.BlueSetAdditionalItem(
                            key = "key",
                            anaplanId = "anaplanId",
                            termsUrl = httpAddressTestInstance(),
                            landingUrl = httpAddressTestInstance(),
                            shopPromoId = "shopPromoId",
                            promoKey = "promoKey",
                            parentPromoId = null,
                        ),
                        OfferPromo.PromoCode(
                            promoCode = "promoCode",
                            endDate = Date(),
                            termsUrlText = "termsUrlText",
                            prices = null,
                            conditions = null,
                            key = "key",
                            anaplanId = "anaplanId",
                            termsUrl = httpAddressTestInstance(),
                            landingUrl = httpAddressTestInstance(),
                            shopPromoId = "shopPromoId",
                            promoKey = "promoKey",
                            promoCodeInDiscountPrices = null,
                            orderMinPrice = null,
                            parentPromoId = null,
                        ),
                    ),
                    emptyList(),
                    null,
                ),
                false
            ).promos,
        ).isNotEmpty
    }
}
