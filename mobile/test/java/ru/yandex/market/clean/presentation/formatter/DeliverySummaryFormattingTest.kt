package ru.yandex.market.clean.presentation.formatter

import android.os.Build
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.analytics.facades.CartThresholdAnalytics
import ru.yandex.market.asViewObject
import ru.yandex.market.clean.domain.model.FreeDeliveryInfo
import ru.yandex.market.clean.domain.model.FreeDeliveryReason
import ru.yandex.market.clean.domain.model.FreeDeliveryStatus
import ru.yandex.market.clean.domain.model.cart.CartPlusInfo
import ru.yandex.market.clean.domain.model.freeDeliveryThresholdTestInstance
import ru.yandex.market.clean.presentation.feature.cart.vo.CartContentsInfo
import ru.yandex.market.clean.presentation.feature.cashback.Gradient
import ru.yandex.market.clean.presentation.vo.DeliveryDescriptionVo
import ru.yandex.market.clean.presentation.vo.DeliverySummaryVo
import ru.yandex.market.clean.presentation.vo.ThresholdProgressStyle
import ru.yandex.market.data.order.options.DeliverySummary
import ru.yandex.market.di.TestScope
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.rub
import ru.yandex.market.utils.Characters
import java.math.BigDecimal
import javax.inject.Inject

@RunWith(Enclosed::class)
class DeliverySummaryFormattingTest {

    @dagger.Component(dependencies = [TestComponent::class])
    @TestScope
    interface Component : MembersInjector<BaseTest>

    abstract class BaseTest {

        @Inject
        lateinit var formatter: DeliverySummaryFormatter

        @Before
        fun setUp() {
            DaggerDeliverySummaryFormattingTest_Component.builder()
                .testComponent(TestApplication.instance.component)
                .build()
                .injectMembers(this)
        }

        companion object {
            val YANDEX_PLUS_THRESHOLD_STYLE =
                ThresholdProgressStyle.YandexPlusThreshold(Gradient.GRADIENT_RADIAL_YANDEX_PLUS_REDESIGN)
        }

    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class GeneralFormattingTest(
        private val deliverySummary: DeliverySummary,
        private val cartContentsInfo: CartContentsInfo,
        private val cartPlusInfo: CartPlusInfo?,
        private val expectedResult: DeliverySummaryVo
    ) : BaseTest() {

        @Test
        fun `Check formatted result matches expectations`() {
            assertThat(formatter.format(deliverySummary, cartContentsInfo, cartPlusInfo)).isEqualTo(expectedResult)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}")
            @JvmStatic
            fun data() = listOf<Array<*>>(

                // Coin free delivery cases
                // 1
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                reason = FreeDeliveryReason.COIN_FREE_DELIVERY,
                                status = FreeDeliveryStatus.ALREADY_FREE
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 1,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 0,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.NonLogin,
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.freeByCondition(
                            "Доставка будет",
                            "бесплатной",
                            "благодаря купону",
                            Characters.NON_BREAKING_SPACE,
                            Characters.SPACE,
                            DeliverySummaryVo.Color.GREEN
                        ),
                        iconProgress = 100,
                        progressStyle = ThresholdProgressStyle.ColoredThreshold(DeliverySummaryVo.Color.GREEN),
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.coupon(isPlusUser = false)
                    )
                ),

                // 2
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                reason = FreeDeliveryReason.COIN_FREE_DELIVERY,
                                status = FreeDeliveryStatus.ALREADY_FREE
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 2,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 1,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.NonLogin,
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.freeByCondition(
                            "Доставка товаров ниже будет",
                            "бесплатной",
                            "благодаря купону",
                            Characters.NON_BREAKING_SPACE,
                            Characters.SPACE,
                            DeliverySummaryVo.Color.GREEN
                        ),
                        iconProgress = 100,
                        progressStyle = ThresholdProgressStyle.ColoredThreshold(DeliverySummaryVo.Color.GREEN),
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.coupon(isPlusUser = false)
                    )
                ),

                // Free Yandex Plus delivery cases
                // 3
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.ALREADY_FREE,
                                reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                            )
                        )
                        .setAllAvailableThresholds(
                            listOf(
                                freeDeliveryThresholdTestInstance(
                                    reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                                )
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 1,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 0,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = true,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.freeByCondition(
                            leftPart = "Доставка будет",
                            midPart = "бесплатной",
                            rightPart = "благодаря Плюсу",
                            midSeparator = Characters.NON_BREAKING_SPACE,
                            rightSeparator = Characters.SPACE,
                            midPartColor = DeliverySummaryVo.Color.GREEN
                        ),
                        iconProgress = 100,
                        progressStyle = YANDEX_PLUS_THRESHOLD_STYLE,
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = true,
                            sumNeedToAddToThreshold = BigDecimal.ZERO
                        )
                    )
                ),

                // 4
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.ALREADY_FREE,
                                reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                            )
                        )
                        .setAllAvailableThresholds(
                            listOf(
                                freeDeliveryThresholdTestInstance(
                                    reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                                )
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 2,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 1,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = true,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.freeByCondition(
                            leftPart = "Доставка товаров ниже будет",
                            midPart = "бесплатной",
                            rightPart = "благодаря Плюсу",
                            midSeparator = Characters.NON_BREAKING_SPACE,
                            rightSeparator = Characters.SPACE,
                            midPartColor = DeliverySummaryVo.Color.GREEN
                        ),
                        iconProgress = 100,
                        progressStyle = YANDEX_PLUS_THRESHOLD_STYLE,
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = true,
                            sumNeedToAddToThreshold = BigDecimal.ZERO
                        )
                    )
                ),

                // Add more products to use Yandex Plus Free Delivery cases
                // 5
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS,
                                reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS,
                                remainder = 300.rub,
                                value = 600.rub
                            )
                        )
                        .setAllAvailableThresholds(
                            listOf(
                                freeDeliveryThresholdTestInstance(
                                    reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                                )
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 1,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 0,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = true,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.paid(
                            leftPart = "Добавьте товаров на",
                            midPart = 300.rub.asViewObject(),
                            rightPart = "(кроме экспресса), и доставка будет бесплатной благодаря Плюсу",
                            midSeparator = Characters.NON_BREAKING_SPACE,
                            rightSeparator = Characters.NON_BREAKING_SPACE
                        ),
                        iconProgress = 50,
                        progressStyle = YANDEX_PLUS_THRESHOLD_STYLE,
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = true,
                            sumNeedToAddToThreshold = BigDecimal.valueOf(300)
                        )
                    )
                ),

                // 6
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS,
                                reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS,
                                remainder = 300.rub,
                                value = 600.rub
                            )
                        )
                        .setAllAvailableThresholds(
                            listOf(
                                freeDeliveryThresholdTestInstance(
                                    reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                                )
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 2,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 1,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = true,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.paid(
                            leftPart = "Ещё",
                            midPart = 300.rub.asViewObject(),
                            rightPart = "\u00A0и товары ниже приедут бесплатно благодаря Плюсу",
                            midSeparator = Characters.NON_BREAKING_SPACE,
                            rightSeparator = Characters.COMMA
                        ),
                        iconProgress = 50,
                        progressStyle = YANDEX_PLUS_THRESHOLD_STYLE,
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = true,
                            sumNeedToAddToThreshold = BigDecimal.valueOf(300)
                        )
                    )
                ),

                // User needs Yandex Plus for free delivery
                // 7
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 1,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 0,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = false,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.freeWithPlus(
                            leftPart = "Для бесплатной доставки",
                            rightPart = ":click:подключите Плюс:click:",
                            rightSeparator = Characters.SPACE
                        ),
                        iconProgress = 100,
                        progressStyle = ThresholdProgressStyle.ColoredThreshold(DeliverySummaryVo.Color.RED),
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = false,
                            sumNeedToAddToThreshold = BigDecimal.ZERO
                        )
                    )
                ),

                // 8
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.WILL_BE_FREE_WITH_YA_PLUS_SUBSCRIPTION
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 2,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 1,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = false,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.freeWithPlus(
                            leftPart = "Для бесплатной доставки товаров ниже",
                            rightPart = ":click:подключите Плюс:click:",
                            rightSeparator = Characters.NON_BREAKING_SPACE
                        ),
                        iconProgress = 100,
                        progressStyle = ThresholdProgressStyle.ColoredThreshold(DeliverySummaryVo.Color.RED),
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = false,
                            sumNeedToAddToThreshold = BigDecimal.ZERO
                        )
                    )
                ),

                // User needs Yandex Plus and add more items for free Delivery
                // 9
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS,
                                reason = FreeDeliveryReason.FREE_DELIVERY_BY_THRESHOLD,
                                remainder = 300.rub,
                                value = 600.rub
                            )
                        )
                        .setAllAvailableThresholds(
                            listOf(
                                freeDeliveryThresholdTestInstance(
                                    reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                                )
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 1,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 0,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = false,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.paidWithPlusLink(
                            leftPart = "Для бесплатной доставки заказа от",
                            midPart = 600.rub.asViewObject(),
                            rightPart = ":click:подключите Плюс:click:",
                            midSeparator = Characters.NON_BREAKING_SPACE,
                            rightSeparator = Characters.NON_BREAKING_SPACE
                        ),
                        iconProgress = 50,
                        progressStyle = ThresholdProgressStyle.ColoredThreshold(DeliverySummaryVo.Color.RED),
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = false,
                            sumNeedToAddToThreshold = BigDecimal.valueOf(300)
                        )
                    )
                ),

                // 10
                arrayOf(
                    DeliverySummary.testBuilder()
                        .setFreeDeliveryThreshold(
                            freeDeliveryThresholdTestInstance(
                                status = FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS,
                                reason = FreeDeliveryReason.FREE_DELIVERY_BY_THRESHOLD,
                                remainder = 300.rub,
                                value = 600.rub
                            )
                        )
                        .setAllAvailableThresholds(
                            listOf(
                                freeDeliveryThresholdTestInstance(
                                    reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                                )
                            )
                        )
                        .build(),
                    CartContentsInfo(
                        totalPacksCount = 2,
                        marketPacksCount = 1,
                        dsbsPacksCount = 0,
                        expressPacksCount = 1,
                        supermarketPacksCount = 0
                    ),
                    CartPlusInfo.Login(
                        hasYandexPlus = false,
                        cashbackBalance = BigDecimal.ZERO,
                        cashbackPercent = null,
                        freeDeliveryByPlusAvailable = true,
                        freeDeliveryByPlusThreshold = null,
                        promoType = FreeDeliveryInfo.PromoType.POST_ONLY_DELIVERY,
                        isMastercardPromoAvailable = false,
                        isGrowingCashbackEnabled = false,
                        isYandexCardPromoAvailable = false
                    ),
                    DeliverySummaryVo(
                        descriptionText = DeliveryDescriptionVo.paidWithPlusLink(
                            leftPart = "Добавьте товаров на",
                            midPart = 300.rub.asViewObject(),
                            rightPart = "и :click:подключите Плюс:click:, и товары ниже приедут бесплатно",
                            midSeparator = Characters.NON_BREAKING_SPACE,
                            rightSeparator = Characters.NON_BREAKING_SPACE
                        ),
                        iconProgress = 50,
                        progressStyle = ThresholdProgressStyle.ColoredThreshold(DeliverySummaryVo.Color.RED),
                        icon = DeliverySummaryVo.Icon.DELIVERY,
                        analyticsThresholdInfo = CartThresholdAnalytics.ThresholdInfo.yandexDelivery(
                            isPlusUser = false,
                            sumNeedToAddToThreshold = BigDecimal.valueOf(300)
                        )
                    )
                )
            )
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class FreeDeliveryPercentsFormattingTest(
        private val leftToFree: Money,
        private val freeDeliveryThreshold: Money,
        private val expectedIconProgress: Int
    ) : BaseTest() {

        @Test
        fun `Calculated percents to free delivery with Yandex Plus matches expectation`() {
            val deliverySummary = DeliverySummary.testBuilder()
                .setFreeDeliveryThreshold(
                    freeDeliveryThresholdTestInstance(
                        value = freeDeliveryThreshold,
                        remainder = leftToFree,
                        status = FreeDeliveryStatus.WILL_BE_FREE_WITH_MORE_ITEMS,
                        reason = FreeDeliveryReason.FREE_DELIVERY_BY_THRESHOLD
                    )
                )
                .setAllAvailableThresholds(
                    listOf(
                        freeDeliveryThresholdTestInstance(
                            reason = FreeDeliveryReason.FREE_DELIVERY_BY_YA_PLUS
                        )
                    )
                )
                .build()

            val cartContentsInfo = CartContentsInfo(
                totalPacksCount = 1,
                marketPacksCount = 1,
                dsbsPacksCount = 0,
                expressPacksCount = 0,
                supermarketPacksCount = 0
            )

            val cartPlusInfo = CartPlusInfo.Login(
                hasYandexPlus = false,
                cashbackBalance = BigDecimal.ZERO,
                cashbackPercent = null,
                freeDeliveryByPlusAvailable = true,
                freeDeliveryByPlusThreshold = null,
                promoType = FreeDeliveryInfo.PromoType.DELIVERY,
                isMastercardPromoAvailable = false,
                isGrowingCashbackEnabled = false,
                isYandexCardPromoAvailable = false
            )

            val formatted = formatter.format(deliverySummary, cartContentsInfo, cartPlusInfo)

            assertThat(formatted!!.iconProgress).isEqualTo(expectedIconProgress)
        }

        companion object {

            @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0} / {1} == {2}%")
            @JvmStatic
            fun data() = listOf<Array<*>>(
                arrayOf(0.rub, 100.rub, 100),
                arrayOf(100.rub, 100.rub, 0),
                arrayOf(100.rub, 200.rub, 50),
                arrayOf(0.rub, 0.rub, 100),
                arrayOf(100.rub, 0.rub, 100),
                arrayOf(200.rub, 100.rub, 0),
                arrayOf(99.rub, 100.rub, 1),
                arrayOf(10.rub, 33.rub, 70),
                arrayOf(10.rub, 35.rub, 71)
            )
        }
    }
}
