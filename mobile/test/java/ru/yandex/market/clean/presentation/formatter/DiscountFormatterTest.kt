package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.clean.data.mapper.OfferMapper
import ru.yandex.market.clean.data.mapper.OfferPromoMapper
import ru.yandex.market.clean.domain.model.Offer
import ru.yandex.market.clean.domain.model.OfferPrices
import ru.yandex.market.clean.domain.model.OfferPromoInfo
import ru.yandex.market.clean.domain.model.offerPricesTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.feature.videosnippets.ui.bage.DiscountVo
import ru.yandex.market.domain.product.model.offer.OfferPromoType
import ru.yandex.market.feature.manager.PromoCodeInTotalDiscountFeatureManager
import ru.yandex.market.feature.videosnippets.ui.bage.BadgeStyle
import ru.yandex.market.rub

@RunWith(Parameterized::class)
class DiscountFormatterTest(
    private val offer: Offer,
    private val offerPrices: OfferPrices,
    private val discountVo: DiscountVo,
) {

    private val promoCodeInTotalDiscountFeatureManager = mock<PromoCodeInTotalDiscountFeatureManager>() {
        on { isPromoCodeInTotalDiscountEnabled(any(), any()) } doReturn false
    }
    private val offerPromoMapper = mock<OfferPromoMapper>()
    private val offerMapper = mock<OfferMapper>()
    private val moneyFormatter = mock<MoneyFormatter>()
    private val formatter = DiscountFormatter(
        offerMapper = offerMapper,
        moneyFormatter = moneyFormatter,
        offerPromoMapper = offerPromoMapper,
        promoCodeInTotalDiscountFeatureManager = promoCodeInTotalDiscountFeatureManager,
    )

    @Test
    fun `Check actual result is match expectations format Offer`() {
        val result = formatter.format(offer, OfferPromoInfo(emptyList(), emptyList(), null), false)
        assertThat(result).isEqualTo(discountVo)
    }

    @Test
    fun `Check actual result is match expectations format OfferPrices`() {
        val result = formatter.format(offerPrices, false, 0f)
        assertThat(result).isEqualTo(discountVo)
    }

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(

            // 0 Без скидки
            arrayOf(
                offerTestInstance(
                    promoTypeToUrl = emptyMap(),
                    prices = offerPricesTestInstance(
                        discountPercent = 0f,
                        dropPrice = null
                    )
                ),
                offerPricesTestInstance(
                    discountPercent = 0f,
                    dropPrice = null
                ),
                DiscountVo(0, BadgeStyle.RED, formattedSaleSize = "")
            ),

            // 1 Скидка Красная
            arrayOf(
                offerTestInstance(
                    promoTypeToUrl = emptyMap(),
                    prices = offerPricesTestInstance(
                        discountPercent = 0.2f,
                        dropPrice = null
                    )
                ),
                offerPricesTestInstance(
                    discountPercent = 0.2f,
                    dropPrice = null
                ),
                DiscountVo(20, BadgeStyle.RED, formattedSaleSize = "−20 %")
            ),

            // 2 Скидка Фиолетовая
            arrayOf(
                offerTestInstance(
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty()),
                    prices = offerPricesTestInstance(
                        discountPercent = 0.1f,
                        dropPrice = null
                    )
                ),
                offerPricesTestInstance(
                    discountPercent = 0.1f,
                    dropPrice = null
                ),
                DiscountVo(10, BadgeStyle.RED, formattedSaleSize = "−10 %")
            ),

            // 3 Скидка Красная + Фиолетовая
            arrayOf(
                offerTestInstance(
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty()),
                    prices = offerPricesTestInstance(
                        dropPrice = 100.rub,
                        discountPercent = 0.1f
                    )
                ),
                offerPricesTestInstance(
                    discountPercent = 0.1f,
                    dropPrice = 100.rub
                ),
                DiscountVo(10, BadgeStyle.RED, formattedSaleSize = "−10 %")
            )
        )
    }
}
