package ru.yandex.market.clean.presentation.formatter

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.asViewObject
import ru.yandex.market.base.network.common.address.HttpAddress
import ru.yandex.market.clean.domain.model.Offer
import ru.yandex.market.domain.product.model.offer.OfferPromoType
import ru.yandex.market.clean.domain.model.offerPricesTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.feature.price.PricesVo
import ru.yandex.market.di.TestScope
import ru.yandex.market.feature.manager.PromoCodeInTotalDiscountFeatureManager
import ru.yandex.market.rub
import javax.inject.Inject

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class PricesFormatterTest(
    private val offer: Offer,
    private val areSmartCoinsApplied: Boolean,
    private val expectedViewObject: PricesVo,
) {

    @Inject
    lateinit var moneyFormatter: MoneyFormatter

    private val promoCodeInTotalDiscountFeatureManager = mock<PromoCodeInTotalDiscountFeatureManager> {
        on {
            isPromoCodeInTotalDiscountEnabled(any(), any())
        } doReturn false
    }

    private val formatter by lazy { PricesFormatter(moneyFormatter, promoCodeInTotalDiscountFeatureManager) }

    @Before
    fun setUp() {
        DaggerPricesFormatterTest_Component.builder()
            .testComponent(TestApplication.instance.component)
            .build()
            .injectMembers(this)
    }

    @Test
    fun `Check actual result is match expectations`() {
        val result = formatter.format(offer, areSmartCoinsApplied, null)
        assertThat(result).isEqualTo(expectedViewObject)
    }

    @TestScope
    @dagger.Component(dependencies = [TestComponent::class])
    interface Component : MembersInjector<PricesFormatterTest>

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(

            // 0
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = null,
                        dropPrice = null
                    ),
                    isPreorder = true
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.NORMAL,
                    basePrice = null,
                    dropPrice = null
                )
            ),

            // 1
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 200.rub,
                        dropPrice = 150.rub
                    ),
                    isPreorder = false,
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty())
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = PricesVo.BasePrice(
                        value = 200.rub.asViewObject(),
                        strikeThroughColor = PricesVo.StrikeThroughColor.NORMAL
                    ),
                    dropPrice = 150.rub.asViewObject()
                )
            ),

            // 2
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 200.rub,
                        dropPrice = null
                    ),
                    isPreorder = false,
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty())
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = PricesVo.BasePrice(
                        value = 200.rub.asViewObject(),
                        strikeThroughColor = PricesVo.StrikeThroughColor.NORMAL
                    ),
                    dropPrice = null
                )
            ),

            // 3
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = null,
                        dropPrice = 200.rub
                    ),
                    isPreorder = false,
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty())
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = null,
                    dropPrice = 200.rub.asViewObject()
                )
            ),

            // 4
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = null,
                        dropPrice = null
                    ),
                    isPreorder = false,
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty())
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.NORMAL,
                    basePrice = null,
                    dropPrice = null
                )
            ),

            // 5
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 200.rub,
                        dropPrice = 150.rub
                    ),
                    isPreorder = false,
                    promoTypeToUrl = emptyMap()
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = PricesVo.BasePrice(
                        value = 200.rub.asViewObject(),
                        strikeThroughColor = PricesVo.StrikeThroughColor.NORMAL
                    ),
                    dropPrice = 150.rub.asViewObject()
                )
            ),

            // 6
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 200.rub,
                        dropPrice = null
                    ),
                    isPreorder = false,
                    promoTypeToUrl = emptyMap()
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = PricesVo.BasePrice(
                        value = 200.rub.asViewObject(),
                        strikeThroughColor = PricesVo.StrikeThroughColor.NORMAL
                    ),
                    dropPrice = null
                )
            ),

            // 7
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = null,
                        dropPrice = 150.rub
                    ),
                    isPreorder = false,
                    promoTypeToUrl = emptyMap()
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.NORMAL,
                    basePrice = null,
                    dropPrice = 150.rub.asViewObject()
                )
            ),

            // 8
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = null,
                        dropPrice = null
                    ),
                    isPreorder = false,
                    promoTypeToUrl = emptyMap()
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.NORMAL,
                    basePrice = null,
                    dropPrice = null
                )
            ),

            // 9
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = null,
                        dropPrice = null
                    ),
                    isPreorder = false,
                    promoTypeToUrl = emptyMap()
                ),
                true,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.PROMO,
                    basePrice = null,
                    dropPrice = null
                )
            ),

            // 10
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 200.rub,
                        dropPrice = null
                    ),
                    isPreorder = false,
                    promoTypeToUrl = emptyMap()
                ),
                true,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.PROMO,
                    basePrice = PricesVo.BasePrice(
                        value = 200.rub.asViewObject(),
                        strikeThroughColor = PricesVo.StrikeThroughColor.NORMAL
                    ),
                    dropPrice = null
                )
            ),

            // 11
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 200.rub,
                        dropPrice = 150.rub
                    ),
                    isPreorder = false,
                    promoTypeToUrl = mapOf(OfferPromoType.PRICE_DROP to HttpAddress.empty())
                ),
                true,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = PricesVo.BasePrice(
                        value = 200.rub.asViewObject(),
                        strikeThroughColor = PricesVo.StrikeThroughColor.NORMAL
                    ),
                    dropPrice = 150.rub.asViewObject()
                )
            ),

            // 12
            arrayOf(
                offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = 100.rub,
                        basePrice = 15.rub,
                        dropPrice = null
                    ),
                    isPreorder = true
                ),
                false,
                PricesVo(
                    price = 100.rub.asViewObject(),
                    priceTextColor = PricesVo.PriceColor.DISCOUNT,
                    basePrice = PricesVo.BasePrice(15.rub.asViewObject(), PricesVo.StrikeThroughColor.NORMAL),
                    dropPrice = null
                )
            )
        )
    }
}
