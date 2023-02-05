package ru.yandex.market.clean.presentation.feature.checkout.confirm

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.checkout.summary.LiftingSummaryFormatter
import ru.yandex.market.checkout.summary.SummaryPriceVo
import ru.yandex.market.clean.domain.model.order.delivery.OrderDeliveryScheme
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.formatter.error.checkout.CargoLiftingType
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.data.order.options.DeliveryLiftingSummary
import ru.yandex.market.data.order.options.DeliverySummary
import ru.yandex.market.domain.money.model.Money

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class LiftingSummaryFormatterTest(
    private val deliverySummary: DeliverySummary,
    private val supplierNameByShopId: Map<String, String>,
    private val deliverySchemeByShopId: Map<String, OrderDeliveryScheme>,
    private val liftingTypes: Map<String, CargoLiftingType>,
    private val expected: List<SummaryPriceVo.Position>,
) {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resourcesDataStore = ResourcesManagerImpl(context.resources)

    private val moneyFormatter = mock<MoneyFormatter>()

    private val formatter = LiftingSummaryFormatter(resourcesDataStore, moneyFormatter)

    @Test
    fun formattingTest() {
        val result = formatter.format(deliverySummary, supplierNameByShopId, deliverySchemeByShopId, liftingTypes)
        assertThat(result).isEqualTo(expected)
    }

    companion object {
        private const val TEXT_IF_PRICE_IS_NULL = "не включен"
        private val DELIVERY_SUMMARY_W_LIFTING = DeliverySummary.testInstance().copy(
            liftingPrices = listOf(
                DeliveryLiftingSummary("1", Money.createRub(1)),
                DeliveryLiftingSummary("2", Money.createRub(1)),
                DeliveryLiftingSummary("3", Money.createRub(1)),
                DeliveryLiftingSummary("4", Money.createRub(1)),
            )
        )

        @ParameterizedRobolectricTestRunner.Parameters
        @JvmStatic
        fun parameters() = listOf(
            //1
            arrayOf(
                DELIVERY_SUMMARY_W_LIFTING,
                mapOf(
                    "1" to "test 1",
                    "2" to "test 2",
                    "3" to "test 3",
                    "4" to "test 4",
                ),
                mapOf(
                    "1" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "2" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "3" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "4" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                ),
                mapOf(
                    "1" to CargoLiftingType.UNLOAD,
                    "2" to CargoLiftingType.UNLOAD,
                    "3" to CargoLiftingType.UNLOAD,
                    "4" to CargoLiftingType.UNLOAD,
                ),
                listOf(
                    SummaryPriceVo.Position(
                        name = "Разгрузка ",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    )
                )
            ),
            //2
            arrayOf(
                DELIVERY_SUMMARY_W_LIFTING,
                mapOf(
                    "1" to "test 1",
                    "2" to "test 2",
                    "3" to "test 3",
                    "4" to "test 4",
                ),
                mapOf(
                    "1" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "2" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "3" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "4" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                ),
                mapOf(
                    "1" to CargoLiftingType.MANUAL,
                    "2" to CargoLiftingType.ELEVATOR,
                    "3" to CargoLiftingType.ELEVATOR,
                    "4" to CargoLiftingType.CARGO_ELEVATOR,
                ),
                listOf(
                    SummaryPriceVo.Position(
                        name = "Подъем на этаж ",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    )
                )
            ),
            //3
            arrayOf(
                DELIVERY_SUMMARY_W_LIFTING,
                mapOf(
                    "1" to "test 1",
                    "2" to "test 2",
                    "3" to "test 3",
                    "4" to "test 4",
                ),
                mapOf(
                    "1" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "2" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "3" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "4" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                ),
                mapOf(
                    "1" to CargoLiftingType.UNLOAD,
                    "2" to CargoLiftingType.ELEVATOR,
                    "3" to CargoLiftingType.ELEVATOR,
                    "4" to CargoLiftingType.CARGO_ELEVATOR,
                ),
                listOf(
                    SummaryPriceVo.Position(
                        name = "Разгрузка (test 1)",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    ),
                    SummaryPriceVo.Position(
                        name = "Подъем на этаж (test 2)",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    ),
                    SummaryPriceVo.Position(
                        name = "Подъем на этаж (test 3)",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    ),
                    SummaryPriceVo.Position(
                        name = "Подъем на этаж (test 4)",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    ),
                )
            ),
            //4
            arrayOf(
                DELIVERY_SUMMARY_W_LIFTING,
                mapOf(
                    "1" to "test 1",
                    "2" to "test 2",
                    "3" to "test 3",
                    "4" to "test 4",
                ),
                mapOf(
                    "1" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "2" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "3" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                    "4" to OrderDeliveryScheme.DROPSHIP_BY_SELLER,
                ),
                mapOf(
                    "1" to CargoLiftingType.NOT_NEEDED,
                    "2" to CargoLiftingType.NOT_NEEDED,
                    "3" to CargoLiftingType.NOT_NEEDED,
                    "4" to CargoLiftingType.NOT_NEEDED,
                ),
                listOf(
                    SummaryPriceVo.Position(
                        name = "Подъем на этаж ",
                        textIfPriceIsNull = TEXT_IF_PRICE_IS_NULL,
                        textFont = SummaryPriceVo.TextFont.BOLD,
                        price = null,
                        textColor = SummaryPriceVo.TextColor.DEFAULT,
                        icon = SummaryPriceVo.Icon.NONE,
                        rightIcon = SummaryPriceVo.Icon.NONE,
                    ),
                )
            ),
        )
    }
}
