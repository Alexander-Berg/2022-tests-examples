package ru.yandex.market.clean.presentation.feature.product.extension.bottombar

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.OfferSpecificationInternal
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.offerPromoInfoTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.payByPlusTestInstance
import ru.yandex.market.clean.presentation.feature.sku.SkuOfferFormatter
import ru.yandex.market.clean.presentation.formatter.PricesFormatter
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.feature.price.pricesVoTestInstance
import ru.yandex.market.ui.view.mvp.cartcounterbutton.CartCounterArgumentsMapper
import ru.yandex.market.ui.view.mvp.cartcounterbutton.cartCounterArgumentsTestInstance

class ProductBottomBarStateFormatterTest {

    private val productOffer = mock<ProductOffer> {
        on { offer } doReturn offerTestInstance()
        on { promoInfo } doReturn offerPromoInfoTestInstance()
        on { internalOfferProperties } doReturn OfferSpecificationInternal(listOf("medicine"))
    }
    private val isCreditEnabled = false
    private val isLoggedIn = true
    private val cartCounterArguments = cartCounterArgumentsTestInstance()
    private val offerPriceVo = pricesVoTestInstance()
    private val creditPrice = "credit price"
    private val resourcesDataStore = mock<ResourcesManager>()
    private val cartCounterArgumentsMapper = mock<CartCounterArgumentsMapper> {
        on {
            map(
                productOffer = productOffer,
                isMinOrderForCurrentScreenEnabled = true
            )
        } doReturn cartCounterArguments
    }
    private val pricesFormatter = mock<PricesFormatter> {
        on { format(productOffer.offer, productOffer.promoInfo, isLoggedIn) } doReturn offerPriceVo
    }
    private val skuOfferFormatter = mock<SkuOfferFormatter> {
        on { formatPromoText(productOffer) } doReturn "promoText"
        on { formatCreditPrice(productOffer, "promoText", isCreditEnabled) } doReturn creditPrice
    }

    private val formatter = ProductBottomBarStateFormatter(
        resourcesDataStore,
        cartCounterArgumentsMapper,
        pricesFormatter,
        skuOfferFormatter,
    )

    @Test
    fun `return regular button`() {
        whenever(productOffer.payByPlus) doReturn payByPlusTestInstance()

        val expected = ProductBottomBarState.AddToCartButtonState(cartCounterArguments, offerPriceVo, creditPrice)
        val actual = formatter.format(productOffer, isCreditEnabled, isLoggedIn, false, false)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `return regular button if plus price is null`() {
        whenever(productOffer.payByPlus) doReturn null

        val expected = ProductBottomBarState.AddToCartButtonState(cartCounterArguments, offerPriceVo, creditPrice)
        val actual =
            formatter.format(productOffer, isCreditEnabled, isLoggedIn, false, false)

        assertThat(actual).isEqualTo(expected)
    }
}
