package ru.yandex.market.clean.presentation.formatter

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.analytics.facades.CartThresholdAnalytics
import ru.yandex.market.clean.domain.model.FreeDeliveryInfo
import ru.yandex.market.clean.domain.model.FreeDeliveryReason
import ru.yandex.market.clean.domain.model.FreeDeliveryStatus
import ru.yandex.market.clean.domain.model.actualizedCartItemTestInstance
import ru.yandex.market.clean.domain.model.actualizedCartTestInstance
import ru.yandex.market.clean.domain.model.cart.CartPlusInfo
import ru.yandex.market.clean.domain.model.cart.cartValidationResultTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.freeDeliveryThresholdTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.model.supplierTestInstance
import ru.yandex.market.clean.presentation.vo.DeliveryDescriptionVo
import ru.yandex.market.clean.presentation.vo.DeliverySummaryVo
import ru.yandex.market.clean.presentation.vo.ThresholdProgressStyle
import ru.yandex.market.data.order.options.DeliverySummary
import ru.yandex.market.data.order.options.OrderSummary
import ru.yandex.market.di.TestScope
import ru.yandex.market.domain.product.model.offer.OfferFeature
import ru.yandex.market.rub
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(Enclosed::class)
class ExpressDeliverySummaryFormattingTest {

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<BaseTest>

    abstract class BaseTest {

        @Inject
        lateinit var formatter: ExpressDeliverySummaryFormatter

        @Before
        fun setUp() {
            DaggerExpressDeliverySummaryFormattingTest_Component.builder()
                .testComponent(TestApplication.instance.component)
                .build()
                .injectMembers(this)
        }

        companion object {

            val YANDEX_PLUS_THRESHOLD_STYLE = ThresholdProgressStyle.ColoredThreshold(
                DeliverySummaryVo.Color.PURPLE
            )

        }

    }

    @RunWith(RobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class GeneralFormattingTest : BaseTest() {

        private val cartWithYandexPlus = CartPlusInfo.Login(
            hasYandexPlus = true,
            cashbackBalance = BigDecimal.ZERO,
            cashbackPercent = null,
            freeDeliveryByPlusAvailable = true,
            freeDeliveryByPlusThreshold = null,
            promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
            isMastercardPromoAvailable = false,
            isGrowingCashbackEnabled = false,
            isYandexCardPromoAvailable = false
        )

        @Test
        fun `Check formatted result in cart without express`() {
            val cartValidationResult = cartValidationResultTestInstance(
                cart = actualizedCartTestInstance(
                    actualizedItems = listOf(
                        actualizedCartItemTestInstance()
                    )
                )
            )

            assertThat(formatter.format(cartValidationResult, cartPlusInfo = null)).isNull()
            assertThat(
                formatter.format(
                    cartValidationResult,
                    cartPlusInfo = cartWithYandexPlus
                )
            ).isNull()
        }

        @Test
        fun `Check delivery will be calculated at checkout`() {
            val supplier = supplierTestInstance(name = "Test Supplier")
            val cartValidationResult = cartValidationResultTestInstance(
                cart = actualizedCartTestInstance(
                    actualizedItems = listOf(
                        actualizedCartItemTestInstance(
                            cartItem = cartItemTestInstance(
                                offer = productOfferTestInstance(
                                    offer = offerTestInstance(
                                        supplier = supplier,
                                        features = setOf(OfferFeature.EXPRESS_DELIVERY),
                                        isExpressDelivery = true
                                    )
                                ),
                                isExpired = false
                            )
                        )
                    ),
                    missingCartItems = emptyList(),
                    summary = OrderSummary
                        .testInstance()
                        .copy(
                            delivery = DeliverySummary.testBuilder()
                                .setFreeDeliveryThreshold(
                                    freeDeliveryThresholdTestInstance(
                                        reason = FreeDeliveryReason.FREE_DELIVERY_BY_THRESHOLD,
                                        status = FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS,
                                        value = 600.rub,
                                        remainder = 300.rub
                                    )
                                )
                                .build()
                        )
                )
            )

            val expectWillBeCheaperWithPlusVo = DeliverySummaryVo(
                descriptionText = DeliveryDescriptionVo.simple(
                    descriptionText = "Стоимость экспресс-доставки рассчитаем при оформлении заказа"
                ),
                progressStyle = YANDEX_PLUS_THRESHOLD_STYLE,
                iconProgress = 100,
                icon = DeliverySummaryVo.Icon.EXPRESS_DELIVERY,
                analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.express(
                    isPlusUser = true,
                    sumNeedToAddToThreshold = BigDecimal.valueOf(0)
                )
            )

            val expectWillBeCheaperWithoutPlusVo = DeliverySummaryVo(
                descriptionText = DeliveryDescriptionVo.simple(
                    descriptionText = "Стоимость экспресс-доставки рассчитаем при оформлении заказа"
                ),
                progressStyle = YANDEX_PLUS_THRESHOLD_STYLE,
                iconProgress = 100,
                icon = DeliverySummaryVo.Icon.EXPRESS_DELIVERY,
                analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.express(
                    isPlusUser = false,
                    sumNeedToAddToThreshold = BigDecimal.valueOf(0)
                )
            )

            assertThat(formatter.format(cartValidationResult, cartWithYandexPlus))
                .isEqualTo(expectWillBeCheaperWithPlusVo)

            assertThat(formatter.format(cartValidationResult, cartPlusInfo = null))
                .isEqualTo(expectWillBeCheaperWithoutPlusVo)
        }

    }
}
