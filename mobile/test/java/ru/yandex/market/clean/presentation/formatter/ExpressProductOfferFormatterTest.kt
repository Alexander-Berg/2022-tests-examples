package ru.yandex.market.clean.presentation.formatter

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.model.ModelInformation
import ru.yandex.market.clean.domain.model.OperationalRating
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.feature.cahsback.AboutCashBackInfoTypeArgument
import ru.yandex.market.clean.presentation.feature.cashback.about.AboutCashbackInfoTypeArgumentMapper
import ru.yandex.market.clean.presentation.feature.cms.item.offerexpress.formatter.ExpressProductOfferBlockFormatter
import ru.yandex.market.clean.presentation.feature.cms.item.offerexpress.formatter.ProductOfferExpressInfoFormatter
import ru.yandex.market.clean.presentation.feature.cms.model.CmsPricesVo
import ru.yandex.market.clean.presentation.feature.operationalrating.OperationalRatingFormatter
import ru.yandex.market.clean.presentation.feature.operationalrating.vo.supplierOperationalRatingVoTestInstance
import ru.yandex.market.clean.presentation.feature.sku.DeliveryInformationFormatter
import ru.yandex.market.feature.productexpressofferinfowidget.ExpressInfoVo
import ru.yandex.market.clean.presentation.feature.sku.deliveryInformationVoTestInstance
import ru.yandex.market.clean.presentation.feature.sku.fittingVoTestInstance
import ru.yandex.market.clean.presentation.feature.trust.vo.TrustMainFormatter
import ru.yandex.market.feature.videosnippets.ui.bage.DiscountVo
import ru.yandex.market.domain.product.model.offer.OfferFeature
import ru.yandex.market.feature.price.PricesVo
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.feature.constructorsnippetblocks.offer.OfferVo
import ru.yandex.market.ui.view.mvp.cartcounterbutton.CartCounterArgumentsMapper
import ru.yandex.market.ui.view.mvp.cartcounterbutton.cartCounterArgumentsTestInstance

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ExpressProductOfferBlockFormatterTest {
    private val resourcesDataStore = mock<ResourcesManager>()
    private val pricesFormatter = mock<CmsPricesFormatter>() {
        on { format(any(), any(), any(), any()) } doReturn EMPTY_PRICES
    }
    private val expressInfoFormatter = mock<ProductOfferExpressInfoFormatter> {
        on { format(any()) } doReturn EXPRESS_INFO_VO
    }

    private val aboutCashbackInfoTypeArgumentMapper = mock<AboutCashbackInfoTypeArgumentMapper> {
        on { map(any()) } doReturn AboutCashBackInfoTypeArgument.Common
    }

    private val operationalRatingFormatter = mock<OperationalRatingFormatter> {
        on { format(any<ProductOffer>()) } doReturn OPERATIONAL_RATING_VO
        on { format(any<OperationalRating>()) } doReturn OPERATIONAL_RATING_VO
    }
    private val discountFormatter = mock<DiscountFormatter> {
        on { format(any<ProductOffer>(), any()) } doReturn DiscountVo.EMPTY
    }
    private val unitInfoFormatter = mock<UnitInfoFormatter> {
        on { format(any()) } doReturn OfferVo.UnitInfoVo.EMPTY
        on { formatOffer(any(), any()) } doReturn OfferVo.UnitInfoVo.EMPTY
    }
    private val cartCounterArgumentsMapper = mock<CartCounterArgumentsMapper> {
        on {
            map(
                any(),
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
                any(),
                any(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        } doReturn CART_COUNTER_TEST_ARGUMENTS
    }
    private val deliveryInformationFormatter = mock<DeliveryInformationFormatter> {
        on { format(any(), any(), any()) } doReturn DELIVERY_INFORMATION_VO
    }
    private val trustMainFormatter = mock<TrustMainFormatter>()

    private val formatter = ExpressProductOfferBlockFormatter(
        cmsPricesFormatter = pricesFormatter,
        discountFormatter = discountFormatter,
        cartCounterArgumentsMapper = cartCounterArgumentsMapper,
        expressInfoFormatter = expressInfoFormatter,
        operationalRatingFormatter = operationalRatingFormatter,
        resourcesManager = resourcesDataStore,
        deliveryInformationFormatter = deliveryInformationFormatter,
        aboutCashbackInfoTypeArgumentMapper = aboutCashbackInfoTypeArgumentMapper,
        trustMainFormatter = trustMainFormatter,
        unitInfoFormatter = unitInfoFormatter,
    )

    @Test
    fun `Test format()`() {
        whenever(resourcesDataStore.getQuantityString(any(), any())).thenReturn("")

        val offer =
            productOfferTestInstance(model = ModelInformation.testBuilder().offersCount(1).build())

        val viewObject = formatter.format(
            productOffer = offer,
            isTinkoffCreditsEnable = false,
            isLoggedIn = false,
            isTrustFeatureToggleEnabled = false,
        )

        assertThat(viewObject.prices).isEqualTo(EMPTY_PRICES)
        assertThat(viewObject.cashback).isNull()
        assertThat(viewObject.discount).isEqualTo(DiscountVo.EMPTY)
        assertThat(viewObject.cartCounterArguments).isEqualTo(CART_COUNTER_TEST_ARGUMENTS)
        assertThat(viewObject.encryptedUrl).isEqualTo(offer.encryptedUrl)
        assertThat(viewObject.supplierId).isEqualTo(offer.supplier?.id)
        assertThat(viewObject.supplierName).isEqualTo(offer.supplierName)
        assertThat(viewObject.shopId).isEqualTo(offer.shopId.toString())
        assertThat(viewObject.isDsbs).isEqualTo(offer.hasFeature(OfferFeature.DSBS))
        assertThat(viewObject.isCpa).isEqualTo(offer.isCpa)
        assertThat(viewObject.expressInfo).isEqualTo(EXPRESS_INFO_VO)
        assertThat(viewObject.encryptedUrl).isEqualTo(offer.encryptedUrl)
        assertThat(viewObject.supplierOperationalRatingVo).isEqualTo(OPERATIONAL_RATING_VO)
    }

    companion object {
        val CART_COUNTER_TEST_ARGUMENTS = cartCounterArgumentsTestInstance(lavkaRedirectDialogParams = null)
        val OPERATIONAL_RATING_VO = supplierOperationalRatingVoTestInstance()
        val EXPRESS_INFO_VO = ExpressInfoVo(
            isExpressShopCurrentlyOpen = false,
            expressDeliveryDate = "",
            expressDeliveryTime = "",
        )
        val EMPTY_PRICES = CmsPricesVo.Prices(
            pricesVo = PricesVo.EMPTY,
            promoText = "",
            creditPrice = ""
        )
        val DELIVERY_INFORMATION_VO = deliveryInformationVoTestInstance()
        val FITTING_INFORMATION_VO = fittingVoTestInstance()
    }
}
