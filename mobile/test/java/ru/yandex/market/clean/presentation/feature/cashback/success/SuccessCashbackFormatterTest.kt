package ru.yandex.market.clean.presentation.feature.cashback.success

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.SuccessOrderResult
import ru.yandex.market.clean.domain.model.orderCreatedBucketResultTestInstance
import ru.yandex.market.clean.presentation.feature.cashback.details.CashbackDetailsFormatter
import ru.yandex.market.clean.presentation.feature.checkout.success.SuccessCashbackAction
import ru.yandex.market.clean.presentation.feature.checkout.success.SuccessCashbackNavigationTarget
import ru.yandex.market.clean.presentation.feature.checkout.success.SuccessCashbackPromoVo
import ru.yandex.market.clean.presentation.feature.checkout.success.SuccessCashbackValueVo
import ru.yandex.market.clean.presentation.feature.checkout.success.SuccessCashbackVo
import ru.yandex.market.domain.cashback.model.OrderSuccessCashbackCommonInfo
import ru.yandex.market.domain.money.model.Money
import java.math.BigDecimal

@RunWith(Parameterized::class)
class SuccessCashbackFormatterTest(
    private val successOrderResult: SuccessOrderResult,
    private val orderSuccessCashbackCommonInfo: OrderSuccessCashbackCommonInfo,
    private val expectedVo: SuccessCashbackVo?
) {

    private val formatter = SuccessCashbackFormatter(
        mock {
            on { getString(R.string.cashback_success_ya_plus_user_emit_subtitle) } doReturn PLUS_USER_EMIT_TITLE
            on {
                getFormattedString(
                    R.string.cashback_success_ya_plus_user_spend_subtitle,
                    orderSuccessCashbackCommonInfo.spendCashbackValue.toInt()
                )
            } doReturn PLUS_USER_SPEND_TITLE + orderSuccessCashbackCommonInfo.spendCashbackValue.toInt()
            on {
                getFormattedString(
                    R.string.cashback_success_not_ya_plus_user_no_cashback_subtitle_with_delivery,
                    orderSuccessCashbackCommonInfo.deliveryByYandexPlusThreshold?.amount?.value.toString()
                )
            } doReturn GET_PLUS_TO_FREE_DELIVERY_TITLE + orderSuccessCashbackCommonInfo.deliveryByYandexPlusThreshold?.amount?.value?.toString()
            on {
                getFormattedString(
                    R.string.cashback_success_not_ya_plus_user_no_cashback_subtitle_without_delivery_with_percent,
                    orderSuccessCashbackCommonInfo.cashbackPercent
                )
            } doReturn GET_PLUS_NO_FREE_DELIVERY_TITLE + orderSuccessCashbackCommonInfo.cashbackPercent.toString()
            on { getString(R.string.cashback_success_not_ya_plus_user_no_cashback_subtitle_without_delivery_without_percent) } doReturn GET_PLUS_ZERO_PERCENT_TITLE
            on { getString(R.string.cashback_success_promo) } doReturn ZERO_CASHBACK_VALUE
            on { getString(R.string.cashback_success_not_ya_plus_user_subtitle_with_delivery) } doReturn FREE_DELIVERY_REGULAR_USER_TITLE
            on { getString(R.string.cashback_success_not_ya_plus_user_subtitle) } doReturn REGULAR_USER_TITLE
            on { getString(R.string.cashback_success_about_action) } doReturn ABOUT_PLUS_TITLE
        },
        mock {
            on {
                formatPrice(orderSuccessCashbackCommonInfo.deliveryByYandexPlusThreshold ?: Money.zeroRub())
            } doReturn orderSuccessCashbackCommonInfo.deliveryByYandexPlusThreshold?.amount?.value.toString()
        },
        mock<CashbackDetailsFormatter>()
    )

    @Test
    fun testFormat() {
        val actualVo = formatter.format(successOrderResult, orderSuccessCashbackCommonInfo)
        assertThat(actualVo).isEqualTo(expectedVo)
    }

    companion object {

        private const val ZERO_CASHBACK_VALUE = "ПЛЮС"
        private const val PLUS_USER_EMIT_TITLE = "заголовок плюсовика получившего балы"
        private const val PLUS_USER_SPEND_TITLE = "заголовок плюсовика списавшего балы - "
        private const val FREE_DELIVERY_REGULAR_USER_TITLE = "заголовок обычного пользователя с бесплатной доставкой"
        private const val REGULAR_USER_TITLE = "заголовок обычного пользователя без бесплатной доставки"
        private const val GET_PLUS_TO_FREE_DELIVERY_TITLE = "подключите плюс для бесплатной доствки от "
        private const val GET_PLUS_NO_FREE_DELIVERY_TITLE = "подключите плюс для кэшбэка процент "
        private const val GET_PLUS_ZERO_PERCENT_TITLE = "подключите плюс для кэшбэка"
        private const val ABOUT_PLUS_TITLE = "про плюс"

        @Parameterized.Parameters(name = "{index}: \"{0} {1}\" -> {2}")
        @JvmStatic
        fun data(): Iterable<Array<*>> {
            return listOf(
                //0
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false
                            )
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = true,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal.valueOf(100),
                        spendCashbackValue = BigDecimal.TEN,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackValueVo(0, 100),
                        title = PLUS_USER_EMIT_TITLE,
                        action = null
                    )
                ),
                //1
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(isBnpl = false)
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = true,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal.ZERO,
                        spendCashbackValue = BigDecimal(200),
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackPromoVo(ZERO_CASHBACK_VALUE),
                        title = PLUS_USER_SPEND_TITLE + "200",
                        action = null
                    )
                ),
                //2
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(isBnpl = false)
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = false,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal.valueOf(300),
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackValueVo(0, 300),
                        title = FREE_DELIVERY_REGULAR_USER_TITLE,
                        action = SuccessCashbackAction(
                            text = ABOUT_PLUS_TITLE,
                            withGradient = false,
                            navigationTarget = SuccessCashbackNavigationTarget.PlusHome,
                        )
                    )
                ),
                //3
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(isBnpl = false)
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = false,
                        hasFreeDeliveryByYandexPlus = false,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal.valueOf(300),
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackValueVo(0, 300),
                        title = REGULAR_USER_TITLE,
                        action = SuccessCashbackAction(
                            text = ABOUT_PLUS_TITLE,
                            withGradient = false,
                            navigationTarget = SuccessCashbackNavigationTarget.PlusHome,
                        )
                    )
                ),
                //4
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(isBnpl = false)
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = false,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = Money.Companion.createRub(150),
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal.ZERO,
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackPromoVo(ZERO_CASHBACK_VALUE),
                        title = GET_PLUS_TO_FREE_DELIVERY_TITLE + "150",
                        action = SuccessCashbackAction(
                            text = ABOUT_PLUS_TITLE,
                            withGradient = false,
                            navigationTarget = SuccessCashbackNavigationTarget.PlusHome,
                        )
                    )
                ),
                //5
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(isBnpl = false)
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = false,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal.ZERO,
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackPromoVo(ZERO_CASHBACK_VALUE),
                        title = GET_PLUS_NO_FREE_DELIVERY_TITLE + "5",
                        action = SuccessCashbackAction(
                            text = ABOUT_PLUS_TITLE,
                            withGradient = false,
                            navigationTarget = SuccessCashbackNavigationTarget.PlusHome,
                        )
                    )
                ),
                //6
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(isBnpl = false)
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = false,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 0,
                        emitCashbackValue = BigDecimal.ZERO,
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackPromoVo(ZERO_CASHBACK_VALUE),
                        title = GET_PLUS_ZERO_PERCENT_TITLE,
                        action = SuccessCashbackAction(
                            text = ABOUT_PLUS_TITLE,
                            withGradient = false,
                            navigationTarget = SuccessCashbackNavigationTarget.PlusHome,
                        )
                    )
                ),
                //7
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(500)
                            ),
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(200),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(500)
                            ),
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(300),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(500)
                            )
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = true,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal(1100),
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackValueVo(0, 1100),
                        title = PLUS_USER_EMIT_TITLE,
                        action = null
                    )
                ),
                //8
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = null
                            ),
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(500)
                            ),
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(700)
                            )
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = true,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal(800),
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackValueVo(0, 800),
                        title = PLUS_USER_EMIT_TITLE,
                        action = null
                    )
                ),
                //9
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = null
                            ),
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(700)
                            ),
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false,
                                emitCashbackValue = BigDecimal.valueOf(100),
                                totalAdditionalMultiorderCashback = BigDecimal.valueOf(100)
                            )
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = true,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 5,
                        emitCashbackValue = BigDecimal(1000),
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackValueVo(0, 1000),
                        title = PLUS_USER_EMIT_TITLE,
                        action = null
                    )
                ),
                //10
                arrayOf(
                    SuccessOrderResult(
                        listOf(
                            orderCreatedBucketResultTestInstance(
                                isBnpl = false
                            )
                        ),
                        true
                    ),
                    OrderSuccessCashbackCommonInfo(
                        hasYandexPlus = false,
                        hasFreeDeliveryByYandexPlus = true,
                        deliveryByYandexPlusThreshold = null,
                        cashbackPercent = 0,
                        emitCashbackValue = BigDecimal.ZERO,
                        spendCashbackValue = BigDecimal.ZERO,
                        cashbackDetails = null,
                    ),
                    SuccessCashbackVo(
                        badgeVo = SuccessCashbackPromoVo(ZERO_CASHBACK_VALUE),
                        title = GET_PLUS_ZERO_PERCENT_TITLE,
                        action = SuccessCashbackAction(
                            text = ABOUT_PLUS_TITLE,
                            withGradient = false,
                            navigationTarget = SuccessCashbackNavigationTarget.PlusHome,
                        )
                    )
                ),
            )
        }
    }
}
