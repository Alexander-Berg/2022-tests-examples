package ru.yandex.market.clean.presentation.feature.cart.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.clean.domain.model.CartItem
import ru.yandex.market.clean.domain.model.CartItemComplementaryDigest
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.offerPricesTestInstance
import ru.yandex.market.clean.domain.model.offerSpecificationInternalTestInstance
import ru.yandex.market.clean.domain.model.retail.EatsRetailCartItem
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_ActualizedTestInstance
import ru.yandex.market.clean.presentation.feature.cart.CartType
import ru.yandex.market.clean.presentation.feature.cart.SelectedByUserData
import ru.yandex.market.clean.presentation.feature.cart.vo.BasePackVo
import ru.yandex.market.clean.presentation.feature.cart.vo.CartItemVo
import ru.yandex.market.clean.presentation.vo.offerPromoInfoVoTestInstance
import ru.yandex.market.common.featureconfigs.managers.MinimumOrderToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.searchitem.offer.BundleSettingsDto
import ru.yandex.market.domain.product.model.offer.OfferColor
import ru.yandex.market.domain.product.model.offer.OfferPromoType
import ru.yandex.market.feature.price.pricesVoTestInstance
import ru.yandex.market.feature.videosnippets.ui.bage.discountVoTestInstance
import ru.yandex.market.net.sku.SkuType
import ru.yandex.market.utils.StringUtils

class CartItemFormatterTest {

    private val minimumOrderToggleManager = mock<MinimumOrderToggleManager> {
        on { getFromCacheOrDefault() } doReturn FeatureToggle(isEnabled = false)
    }

    private val featureConfigsProvider = mock<FeatureConfigsProvider>().also { provider ->
        whenever(provider.minimumOrderToggleManager) doReturn minimumOrderToggleManager
    }

    private val selectedByUserData = mock<SelectedByUserData>()
    private val cartType = mock<CartType>()
    private val cartFormatterConfig: CartFormatterConfig = mock {
        on { isCartPartialPurchaseEnabled } doReturn true
        on { isAnalogsInCartEnabled } doReturn false

    }

    private val formatter = CartItemFormatter(
        bundleGrouper = mock(),
        moneyFormatter = mock {
            on { formatPrice(any()) } doReturn DUMMY_PRICE
        },
        pricesFormatter = mock {
            on {
                format(
                    prices = any(),
                    isPreorder = any(),
                    isPriceDropPromoApplied = any(),
                    areSmartCoinsApplied = any(),
                    prefix = any(),
                    suffix = any()
                )
            } doReturn pricesVoTestInstance()
        },
        discountFormatter = mock {
            on {
                format(
                    offerPrices = any(),
                    hasBonuses = any(),
                    discountBeforeBonusesApplied = anyOrNull()
                )
            } doReturn discountVoTestInstance()
        },
        offerPromoFormatter = mock {
            on {
                format(
                    offer = anyOrNull(),
                    promoInfo = anyOrNull(),
                    isLoggedIn = any(),
                    isSnippetRedesign = any()
                )
            } doReturn offerPromoInfoVoTestInstance()
        },
        resourcesManager = mock {
            on { getString(any()) } doReturn DUMMY_STRING
        },
        dateFormatter = mock(),
        packTitleFormatter = mock(),
        featureConfigsProvider = featureConfigsProvider,
        orderDeliverySchemeClassifier = mock(),
        serviceFormatter = mock(),
        purchaseByListFeatureManager = mock(),
        addServiceButtonFormatter = mock(),
        cartPricePrefixFormatter = mock(),
        fittingInformationFormatter = mock(),
        dateTimeProvider = mock(),
        unitInfoFormatter = mock(),
        cartItemComplementaryFormatter = mock()
    )

    @Test
    fun `Should format eats retail cart items without unavailable pack`() {
        val availableCartItem = createAvailableEatsReatailCartItem()
        val availableCartItem2 = createAvailableEatsReatailCartItem()

        val cartItems = listOf(availableCartItem, availableCartItem2)

        val expected = listOf(
            createCartItemVo(
                item = availableCartItem.cartItem,
                isInStock = true,
                hasDivider = true
            ),
            createCartItemVo(
                item = availableCartItem2.cartItem,
                isInStock = true,
                hasDivider = true
            )
        )

        val actual = formatter.formatEatsRetailItems(
            cartItems = cartItems,
            selectedByUserData = selectedByUserData,
            cartFormatterConfig = cartFormatterConfig,
            cartType = cartType
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should sort eats retail cart items by availability`() {
        val availableCartItem = createAvailableEatsReatailCartItem()
        val notAvailableCartItem = createNotAvailableEatsRetailCartItem()

        val cartItems = listOf(notAvailableCartItem, availableCartItem)

        val expected = listOf(
            createCartItemVo(
                item = availableCartItem.cartItem,
                isInStock = true,
                hasDivider = true
            ),
            BasePackVo(DUMMY_STRING, cartType),
            createCartItemVo(
                item = notAvailableCartItem.cartItem,
                isInStock = false,
                hasDivider = false
            )
        )

        val actual = formatter.formatEatsRetailItems(
            cartItems = cartItems,
            selectedByUserData = selectedByUserData,
            cartFormatterConfig = cartFormatterConfig,
            cartType = cartType
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Should format eats retail cart items with unavailable pack`() {
        val cartItem = createNotAvailableEatsRetailCartItem()
        val cartItem2 = createNotAvailableEatsRetailCartItem()

        val cartItems = listOf(cartItem, cartItem2)

        val expected = listOf(
            BasePackVo(DUMMY_STRING, cartType),
            createCartItemVo(
                item = cartItem.cartItem,
                isInStock = false,
                hasDivider = false
            ),
            createCartItemVo(
                item = cartItem.cartItem,
                isInStock = false,
                hasDivider = true
            )
        )

        val actual = formatter.formatEatsRetailItems(
            cartItems = cartItems,
            selectedByUserData = selectedByUserData,
            cartFormatterConfig = cartFormatterConfig,
            cartType = cartType
        )

        assertThat(actual).isEqualTo(expected)
    }

    private fun createAvailableEatsReatailCartItem(): EatsRetailCartItem.Actualized {
        return eatsRetailCartItem_ActualizedTestInstance(
            cartItem = cartItemTestInstance(isExpired = false, isOutletOnlyAvailable = false),
        )
    }

    private fun createNotAvailableEatsRetailCartItem(): EatsRetailCartItem.Actualized {
        return eatsRetailCartItem_ActualizedTestInstance(
            cartItem = cartItemTestInstance(userBuyCount = 0, isOutletOnlyAvailable = false),
        )
    }

    private fun createCartItemVo(
        item: CartItem,
        isInStock: Boolean,
        hasDivider: Boolean,
    ): CartItemVo {
        return CartItemVo(
            id = item.serverId,
            name = item.name,
            image = item.image,
            prices = offerPricesTestInstance().copy(
                basePrice = null,
                discountPercent = 0.0f,
                dropPrice = null,
                oldDiscountPercent = 0.0f,
                paymentProcessingPrice = null
            ),
            count = item.userBuyCount,
            quantityLimit = BundleSettingsDto.QuantityLimitDto(1, item.offer?.offer?.stepOfferCount),
            errors = emptyList(),
            offerId = item.offerId,
            skuId = item.skuId ?: "",
            skuType = item.offer?.skuType ?: SkuType.UNKNOWN,
            modelId = item.modelId?.toString() ?: "",
            categoryId = item.categoryId,
            persistentOfferId = item.persistentOfferId,
            basePrice = DUMMY_PRICE,
            appliedCoinColors = emptyList(),
            buyerPriceNominal = item.carterPrice,
            isClickAndCollect = item.isClickAndCollect(),
            supplierName = StringUtils.nvl(item.supplierName),
            isExpired = item.isExpired,
            pricesViewObject = null,
            isPriceDropApplied = item.isOfferPromoApplied(OfferPromoType.PRICE_DROP),
            appliedPromos = listOf(OfferPromoType.PRICE_DROP.name),
            bundleId = item.bundleId,
            isPrimaryInBundle = item.isPrimaryBundleItem,
            giftSkuId = null,
            matchingKey = item.matchingKey,
            isInStock = isInStock,
            isDsbs = item.isDsbs(),
            isGift = false,
            offerPromos = offerPromoInfoVoTestInstance(),
            cashbackInfo = null,
            discount = discountVoTestInstance(),
            highlightCountModificationErrors = true,
            isSupplierShown = false,
            vendorId = item.vendorId,
            isPriceDropSpecialPlace = item.isPriceDropPromoEnabled,
            packInfo = item.packInfo,
            termsUrl = HttpAddress.empty(),
            offerColor = item.offer?.offerColor ?: OfferColor.UNKNOWN,
            isExpress = item.offer?.offer?.isExpressDelivery ?: false,
            primaryGiftOfferTermsUrl = HttpAddress.empty(),
            isDigital = item.isDigital,
            offerFeatures = item.offer?.offerFeatures ?: emptySet(),
            isEdaDelivery = item.offer?.offer?.isEdaDelivery ?: false,
            showUid = item.offer?.showUid,
            shopId = item.offer?.shopId ?: 0,
            feedOfferId = item.offer?.feedOfferId,
            feedId = item.offer?.feedId,
            supplierId = item.offer?.supplierId ?: 0,
            businessId = item.businessId,
            shopSku = item.offer?.shopSku,
            offerCpc = item.offer?.cpc,
            internalOfferProperties = offerSpecificationInternalTestInstance(),
            addServiceButtonVo = null,
            isFashion = item.offer?.isFashion ?: false,
            isPurchaseByListEnabled = false,
            isCartPartialPurchaseEnabled = true,
            fittingAvailable = item.packInfo?.isPartialDeliveryAvailable ?: false,
            hasCostLimitError = false,
            isExclusive = item.isExclusive,
            purchaseByListInformerInfo = null,
            dateInStockDate = item.dateInStock,
            isAnalogsInCartEnabled = false,
            unit = item.offer?.unitInfo?.mainUnit,
            countPerUnit = null,
            isLast = item.isLast,
            stockKeepingUnitId = item.offer?.stockKeepingUnitId,
            isExtendedDeliveryTime = false,
            isResaleGoodsEnabled = false,
            hasDivider = hasDivider,
            complementaryDigest = CartItemComplementaryDigest.RELEVANT,
            isUniqueOffer = item.offer?.isUniqueOffer ?: false,
        )
    }

    private companion object {
        const val DUMMY_PRICE = "DUMMY_PRICE"
        const val DUMMY_STRING = "DUMMY_STRING"
    }

}
