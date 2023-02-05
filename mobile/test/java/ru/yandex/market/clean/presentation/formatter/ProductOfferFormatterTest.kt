package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.ModelInformation
import ru.yandex.market.clean.domain.model.Offer
import ru.yandex.market.clean.domain.model.OfferPromoInfo
import ru.yandex.market.clean.domain.model.ProductOffer
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.feature.videosnippets.ui.bage.DiscountVo
import ru.yandex.market.clean.presentation.vo.offerPromoInfoVoTestInstance
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.feature.price.PricesVo
import ru.yandex.market.common.android.ResourcesManager

class ProductOfferFormatterTest {

    private val resourcesDataStore = mock<ResourcesManager>()
    private val reasonsFormatter = mock<RecommendedReasonsFormatter>()
    private val pricesFormatter = mock<PricesFormatter>()
    private val moneyFormatter = mock<MoneyFormatter>()
    private val discountFormatter = mock<DiscountFormatter>() {
        on { format(any<ProductOffer>(), any()) } doReturn DiscountVo.EMPTY
    }
    private val offerPromoFormatter = mock<OfferPromoFormatter>() {
        on {
            format(
                anyOrNull<ProductOffer>(),
                anyOrNull<OfferPromoInfo>(),
                any(),
                any()
            )
        } doReturn offerPromoInfoVoTestInstance()
    }

    private val formatter = ProductOfferFormatter(
        resourcesDataStore,
        reasonsFormatter,
        pricesFormatter,
        discountFormatter,
        moneyFormatter,
        offerPromoFormatter,
    )

    @Test
    fun `Return empty string for offers count when there is only one offer`() {
        whenever(discountFormatter.format(any<Offer>(), any(), any())).thenReturn(DiscountVo.EMPTY)
        whenever(
            pricesFormatter.format(
                any(),
                any<OfferPromoInfo>(),
                any(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(PricesVo.EMPTY)
        whenever(reasonsFormatter.format(any<ProductOffer>(), any())).thenReturn(
            recommendedReasonsVoTestInstance()
        )
        whenever(resourcesDataStore.getQuantityString(any(), any())).thenReturn("")
        whenever(moneyFormatter.formatPriceAsViewObject(any(), any<Char>(), any()))
            .thenReturn(MoneyVo.empty())

        val offer = productOfferTestInstance(model = ModelInformation.testBuilder().offersCount(1).build())

        val viewObject = formatter.format(offer, false)

        assertThat(viewObject.offersCount).isEmpty()
    }

    @Test
    fun `Return empty string for offers count when no model information present`() {
        whenever(discountFormatter.format(any<Offer>(), any(), any())).thenReturn(DiscountVo.EMPTY)
        whenever(
            pricesFormatter.format(
                any(),
                any<OfferPromoInfo>(),
                any(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(PricesVo.EMPTY)
        whenever(reasonsFormatter.format(any<ProductOffer>(), any())).thenReturn(
            recommendedReasonsVoTestInstance()
        )
        whenever(resourcesDataStore.getString(any())).thenReturn("")
        val offer = productOfferTestInstance(model = null)

        val viewObject = formatter.format(offer, false)

        assertThat(viewObject.offersCount).isEmpty()
    }

    @Test
    fun `Return formatted string for offers count when there is two or more offers`() {
        whenever(discountFormatter.format(any<Offer>(), any(), any())).thenReturn(DiscountVo.EMPTY)
        whenever(
            pricesFormatter.format(
                any(),
                any<OfferPromoInfo>(),
                any(),
                anyOrNull(),
                anyOrNull(),
            )
        ).thenReturn(PricesVo.EMPTY)
        whenever(reasonsFormatter.format(any<ProductOffer>(), any())).thenReturn(
            recommendedReasonsVoTestInstance()
        )
        val offersCount = 2
        val formattedString = "$offersCount offers"
        whenever(
            resourcesDataStore.getQuantityString(
                R.plurals.x_variants,
                offersCount
            )
        ).thenReturn(formattedString)
        whenever(resourcesDataStore.getQuantityString(eq(R.plurals.reviews), any())).thenReturn("")
        val offer = productOfferTestInstance(model = ModelInformation.testBuilder().offersCount(2).build())

        val viewObject = formatter.format(offer, false)

        assertThat(viewObject.offersCount).isEqualTo(formattedString)
    }
}
