package ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.delivery.DeliveryConditions
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.DeliveryVo
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.AMOUNT_VALUE
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.CURRENCY_RUR
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_CONDITIONS
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_CONDITIONS_WITH_EXPRESS_EMPTY_PRICES
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_CONDITIONS_WITH_EXPRESS_NULL_PRICES
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_CONDITIONS_WITH_PLUS_EMPTY_PRICES
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_FOR_PLUS_TITLE
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_ORDER_ANY_PRICE
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.DELIVERY_PRICE_RANGE
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.EXPRESS_DELIVERY_TITLE
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.EXPRESS_DELIVERY_VO
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.EXPRESS_DELIVERY_VO_WITHOUT_ANY_PRICE_DELIVERY
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.EXPRESS_DELIVERY_VO_WITHOUT_YA_PLUS_DELIVERY
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.MONEY_DELIVERY_CONDITIONS
import ru.yandex.market.clean.presentation.feature.sis.deliverydialog.vo.express.ExpressDeliveryVoFormatterTestEntity.PRICE_WITH_CURRENCY
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.feature.money.formatter.CurrencyFormatter

@RunWith(Parameterized::class)
class ExpressDeliveryVoFormatterTest(
    private val input: DeliveryConditions,
    private val expectedResult: DeliveryVo.ExpressDeliveryVo
) {

    private val currencyFormatter = mock<CurrencyFormatter>() {
        on {
            format(
                MONEY_DELIVERY_CONDITIONS?.currency ?: Currency.UNKNOWN
            )
        } doReturn CURRENCY_RUR
    }

    private val resourcesManager = mock<ResourcesManager>() {

        on { getString(R.string.dialog_express_delivery_title) } doReturn EXPRESS_DELIVERY_TITLE

        on { getFormattedString(R.string.dialog_delivery_order_any_price) } doReturn DELIVERY_ORDER_ANY_PRICE

        on { getFormattedString(R.string.dialog_delivery_for_plus_title) } doReturn DELIVERY_FOR_PLUS_TITLE

        on {
            getFormattedString(
                R.string.dialog_common_delivery_price_with_currency,
                AMOUNT_VALUE,
                CURRENCY_RUR
            )
        } doReturn PRICE_WITH_CURRENCY

        on {
            getFormattedString(
                R.string.dialog_express_delivery_price_range,
                PRICE_WITH_CURRENCY,
                PRICE_WITH_CURRENCY
            )
        } doReturn DELIVERY_PRICE_RANGE

    }

    private val formatter = ExpressDeliveryVoFormatter(
        currencyFormatter = currencyFormatter,
        resourcesManager = resourcesManager
    )

    @Test
    fun `Check express delivery vo formatter`() {
        assertThat(formatter.format(input)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun parameters() = listOf(

            // 0
            arrayOf(
                DELIVERY_CONDITIONS,
                EXPRESS_DELIVERY_VO
            ),

            // 1
            arrayOf(
                DELIVERY_CONDITIONS_WITH_EXPRESS_EMPTY_PRICES,
                EXPRESS_DELIVERY_VO_WITHOUT_ANY_PRICE_DELIVERY
            ),

            // 2
            arrayOf(
                DELIVERY_CONDITIONS_WITH_PLUS_EMPTY_PRICES,
                EXPRESS_DELIVERY_VO_WITHOUT_YA_PLUS_DELIVERY
            ),

            // 3
            arrayOf(
                DELIVERY_CONDITIONS_WITH_EXPRESS_NULL_PRICES,
                EXPRESS_DELIVERY_VO_WITHOUT_ANY_PRICE_DELIVERY
            ),

            )
    }

}