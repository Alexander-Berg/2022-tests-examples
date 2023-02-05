package ru.yandex.market.ui.view.mvp.cartcounterbutton

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.beru.android.R
import ru.yandex.market.clean.presentation.formatter.PricesFormatter
import ru.yandex.market.clean.presentation.formatter.UnitInfoFormatter
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.feature.cartbutton.CartButtonState
import ru.yandex.market.feature.cartbutton.CartButtonVo
import ru.yandex.market.feature.price.PricesVo

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CartButtonFormatterTest(
    private val testInputData: TestInputData,
    private val expected: TestOutputData
) {

    private val resourcesManager = mock<ResourcesManager> {
        on { getString(R.string.create_preorder) } doReturn "Оформить предзаказ"
        on { getString(R.string.not_for_sale) } doReturn "Не продаётся"
        on { getString(R.string.sku_blue_set_add_to_cart) } doReturn "Комплектом в корзину"
        on { getString(R.string.add_to_cart) } doReturn "Добавить в корзину"
        on { getString(R.string.add_to_cart_min_count) } doReturn "В корзину от"
        on { getString(R.string.cart_button_set_is_deleted) } doReturn "Комплект удалён"
        on { getString(R.string.cart_button_items_is_deleted) } doReturn "Товар удалён"
        on { getString(R.string.offer_order) } doReturn "В корзину"
        on {
            getFormattedQuantityString(
                R.plurals.cart_counter,
                3, 3
            )
        } doReturn "3\\u00A0товара\\u00A0в\\u00A0корзине"
        on {
            getFormattedQuantityString(
                R.plurals.cart_counter,
                1, 1
            )
        } doReturn "1\\u00A0товар\\u00A0в\\u00A0корзине"
        on { getString(R.string.preorder) } doReturn "Предзаказ"
        on {
            getFormattedString(
                R.string.offer_min_order,
                expected.offerMinOrder
            )
        } doReturn "В корзину от ${expected.offerMinOrder}"
        on { getFormattedQuantityString(R.plurals.x_products_genitive, 2, 2) } doReturn "2 товаров"
        on { getFormattedQuantityString(R.plurals.x_products_genitive, 3, 3) } doReturn "3 товаров"
        on { getFormattedString(R.string.count_units_in_cart, 2, 2) } doReturn "2 шт \\u00A0в\\u00A0корзине"
    }
    private val pricesFormatter = mock<PricesFormatter>()
    private val cartLongTextCache = mock<CartLongTextCache>() {
        on { getLongTextMapForCount(any(), any(), any()) } doReturn null
    }
    private val unitInfoFormatter = mock<UnitInfoFormatter>()
    private val cartButtonFormatter = CartButtonFormatter(
        resourcesManager = resourcesManager,
        pricesFormatter = pricesFormatter,
        cartLongTextCache = cartLongTextCache,
        unitInfoFormatter = unitInfoFormatter
    )

    @Test
    fun format() {
        val actual = testInputData.let { input ->
            cartButtonFormatter.format(
                state = input.state,
                purchasePrice = input.purchasePrice,
                basePrice = input.basePrice,
                isPreorder = input.isPreorder,
                previousVo = input.previousVo,
                isBlueSetFormat = input.isBlueSetFormat,
                isSample = input.isSample,
                maxItems = input.maxItems,
                unitInfo = input.unitInfo
            )
        }
        assertThat(actual.toString()).isEqualTo(expected.cartButtonVo.toString())
    }

    data class TestInputData(
        val testDescription: String,
        val state: CartButtonState,
        val purchasePrice: Money?,
        val basePrice: Money?,
        val isPreorder: Boolean,
        val previousVo: CartButtonVo?,
        val isBlueSetFormat: Boolean = false,
        val isSample: Boolean = false,
        val maxItems: Int,
        val unitInfo: CartCounterArguments.CartOfferUnit? = null,
    )

    data class TestOutputData(
        val offerMinOrder: String,
        val cartButtonVo: CartButtonVo,
    )

    companion object {
        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): Iterable<Array<Any?>> = listOf(
            // 0
            arrayOf(
                TestInputData(
                    testDescription = "availableCount > minItemCount",
                    CartButtonState(
                        buttonState = CartButtonState.ButtonState.NOT_IN_CART,
                        itemCount = 0,
                        minItemCount = 1,
                        availableItemCount = 10
                    ),
                    purchasePrice = null,
                    basePrice = null,
                    isPreorder = false,
                    previousVo = null,
                    isBlueSetFormat = false,
                    isSample = false,
                    maxItems = 10,
                ),
                TestOutputData(
                    offerMinOrder = "1",
                    cartButtonVo = CartButtonVo(
                        buttonStyle = CartButtonVo.ButtonStyle.NOT_IN_CART,
                        isEnabled = true,
                        isProgressVisible = false,
                        isShaded = false,
                        state = CartButtonState.ButtonState.NOT_IN_CART,
                        longText = CartButtonVo.TextOptions(
                            options = listOf(
                                "Товар удалён",
                                "Оформить предзаказ",
                                "Не продаётся",
                                "Добавить в корзину"
                            ),
                            current = "Добавить в корзину"
                        ),
                        shortText = CartButtonVo.TextOptions(
                            options = listOf(
                                "В корзину",
                                "0",
                                "0",
                                "Предзаказ",
                                "Не продаётся",
                                "В корзину",
                            ), current = "В корзину"
                        ),
                        prices = PricesVo.EMPTY,
                        countButtonsState = CartButtonVo.CountButtonsState.GONE,
                        unitText = null,
                        countPerUnitText = null
                    )
                )
            ),
            // 1
            arrayOf(
                TestInputData(
                    testDescription = "availableItemCount < 0",
                    CartButtonState(
                        buttonState = CartButtonState.ButtonState.NOT_IN_CART,
                        itemCount = 0,
                        minItemCount = 1,
                        availableItemCount = -10
                    ),
                    purchasePrice = null,
                    basePrice = null,
                    isPreorder = false,
                    previousVo = null,
                    isBlueSetFormat = false,
                    isSample = false,
                    maxItems = -10,
                ),
                TestOutputData(
                    offerMinOrder = "1",
                    cartButtonVo = CartButtonVo(
                        buttonStyle = CartButtonVo.ButtonStyle.NOT_IN_CART,
                        isEnabled = true,
                        isProgressVisible = false,
                        isShaded = false,
                        state = CartButtonState.ButtonState.NOT_IN_CART,
                        longText = CartButtonVo.TextOptions(
                            options = listOf(
                                "Товар удалён",
                                "Оформить предзаказ",
                                "Не продаётся",
                                "Добавить в корзину"
                            ),
                            current = "Добавить в корзину"
                        ),
                        shortText = CartButtonVo.TextOptions(
                            options = listOf(
                                "В корзину",
                                "0",
                                "0",
                                "Предзаказ",
                                "Не продаётся",
                                "В корзину",
                            ), current = "В корзину"
                        ),
                        prices = PricesVo.EMPTY,
                        countButtonsState = CartButtonVo.CountButtonsState.GONE,
                        unitText = null,
                        countPerUnitText = null
                    )
                )
            ),
            // 2
            arrayOf(
                TestInputData(
                    testDescription = "minItemCount > 1",
                    CartButtonState(
                        buttonState = CartButtonState.ButtonState.NOT_IN_CART,
                        itemCount = 0,
                        minItemCount = 2,
                        availableItemCount = 10
                    ),
                    purchasePrice = null,
                    basePrice = null,
                    isPreorder = false,
                    previousVo = null,
                    isBlueSetFormat = false,
                    isSample = false,
                    maxItems = 10,
                ),
                TestOutputData(
                    offerMinOrder = "2",
                    cartButtonVo = CartButtonVo(
                        buttonStyle = CartButtonVo.ButtonStyle.NOT_IN_CART,
                        isEnabled = true,
                        isProgressVisible = false,
                        isShaded = false,
                        state = CartButtonState.ButtonState.NOT_IN_CART,
                        longText = CartButtonVo.TextOptions(
                            options = listOf(
                                "Товар удалён",
                                "Оформить предзаказ",
                                "Не продаётся",
                                "В корзину от 2 товаров"
                            ),
                            current = "В корзину от 2 товаров"
                        ),
                        shortText = CartButtonVo.TextOptions(
                            options = listOf(
                                "В корзину от 2",
                                "0",
                                "0",
                                "Предзаказ",
                                "Не продаётся",
                                "В корзину",
                            ), current = "В корзину от 2"
                        ),
                        prices = PricesVo.EMPTY,
                        countButtonsState = CartButtonVo.CountButtonsState.GONE,
                        unitText = null,
                        countPerUnitText = null
                    )
                )
            ),
            // 3
            arrayOf(
                TestInputData(
                    testDescription = "availableItemCount < minItemCount",
                    CartButtonState(
                        buttonState = CartButtonState.ButtonState.NOT_IN_CART,
                        itemCount = 0,
                        minItemCount = 4,
                        availableItemCount = 3
                    ),
                    purchasePrice = null,
                    basePrice = null,
                    isPreorder = false,
                    previousVo = null,
                    isBlueSetFormat = false,
                    isSample = false,
                    maxItems = 3,
                ),
                TestOutputData(
                    offerMinOrder = "3",
                    cartButtonVo = CartButtonVo(
                        buttonStyle = CartButtonVo.ButtonStyle.NOT_IN_CART,
                        isEnabled = true,
                        isProgressVisible = false,
                        isShaded = false,
                        state = CartButtonState.ButtonState.NOT_IN_CART,
                        longText = CartButtonVo.TextOptions(
                            options = listOf(
                                "Товар удалён",
                                "Оформить предзаказ",
                                "Не продаётся",
                                "В корзину от 3 товаров"
                            ),
                            current = "В корзину от 3 товаров"
                        ),
                        shortText = CartButtonVo.TextOptions(
                            options = listOf(
                                "В корзину от 3",
                                "0",
                                "0",
                                "Предзаказ",
                                "Не продаётся",
                                "В корзину",
                            ), current = "В корзину от 3"
                        ),
                        prices = PricesVo.EMPTY,
                        countButtonsState = CartButtonVo.CountButtonsState.GONE,
                        unitText = null,
                        countPerUnitText = null
                    )
                )
            ),
            // 4
            arrayOf(
                TestInputData(
                    testDescription = "availableItemCount = minItemCount",
                    CartButtonState(
                        buttonState = CartButtonState.ButtonState.NOT_IN_CART,
                        itemCount = 0,
                        minItemCount = 3,
                        availableItemCount = 3
                    ),
                    purchasePrice = null,
                    basePrice = null,
                    isPreorder = false,
                    previousVo = null,
                    isBlueSetFormat = false,
                    isSample = false,
                    maxItems = 3,
                ),
                TestOutputData(
                    offerMinOrder = "3",
                    cartButtonVo = CartButtonVo(
                        buttonStyle = CartButtonVo.ButtonStyle.NOT_IN_CART,
                        isEnabled = true,
                        isProgressVisible = false,
                        isShaded = false,
                        state = CartButtonState.ButtonState.NOT_IN_CART,
                        longText = CartButtonVo.TextOptions(
                            options = listOf(
                                "Товар удалён",
                                "Оформить предзаказ",
                                "Не продаётся",
                                "В корзину от 3 товаров"
                            ),
                            current = "В корзину от 3 товаров"
                        ),
                        shortText = CartButtonVo.TextOptions(
                            options = listOf(
                                "В корзину от 3",
                                "0",
                                "0",
                                "Предзаказ",
                                "Не продаётся",
                                "В корзину",
                            ), current = "В корзину от 3"
                        ),
                        prices = PricesVo.EMPTY,
                        countButtonsState = CartButtonVo.CountButtonsState.GONE,
                        unitText = null,
                        countPerUnitText = null
                    )
                )
            ),
            // 5
            arrayOf(
                TestInputData(
                    testDescription = "IN_CART state",
                    CartButtonState(
                        buttonState = CartButtonState.ButtonState.IN_CART,
                        itemCount = 1,
                        minItemCount = 3,
                        availableItemCount = 3
                    ),
                    purchasePrice = null,
                    basePrice = null,
                    isPreorder = false,
                    previousVo = null,
                    isBlueSetFormat = false,
                    isSample = false,
                    maxItems = 3,
                ),
                TestOutputData(
                    offerMinOrder = "3",
                    cartButtonVo = CartButtonVo(
                        buttonStyle = CartButtonVo.ButtonStyle.IN_CART,
                        isEnabled = true,
                        isProgressVisible = false,
                        isShaded = false,
                        state = CartButtonState.ButtonState.IN_CART,
                        longText = CartButtonVo.TextOptions(
                            options = listOf(
                                "1\\u00A0товар\\u00A0в\\u00A0корзине",
                                "Оформить предзаказ",
                                "Не продаётся",
                                "В корзину от 3 товаров"
                            ),
                            current = "1\\u00A0товар\\u00A0в\\u00A0корзине"
                        ),
                        shortText = CartButtonVo.TextOptions(
                            options = listOf(
                                "В корзину от 3",
                                "1",
                                "1",
                                "Предзаказ",
                                "Не продаётся",
                                "В корзину",
                            ), current = "1"
                        ),
                        prices = PricesVo.EMPTY,
                        countButtonsState = CartButtonVo.CountButtonsState.ENABLE_PLUS_ENABLE_MINUS,
                        unitText = null,
                        countPerUnitText = null
                    )
                )
            )
        )
    }
}
