package ru.yandex.market.ui.view.mvp.cartcounterbutton

import android.os.Build
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class CartCounterCalculatorTest(
    private val testInputData: TestInputData,
    private val expected: TestExpectedData
) {
    val cartCounterCalculator = CartCounterCalculator()

    @Test
    fun checkIncrease() {
        val actual = testInputData.let { state ->
            cartCounterCalculator.calculateItemCountForIncrease(
                state.isEmpty,
                state.currentCount,
                state.stepCount,
                state.minItemCount,
                state.availableCountInStock
            )
        }
        assertThat(actual).isEqualTo(expected.increaseResult)
    }

    @Test
    fun checkDecrease() {
        val actual = testInputData.let { state ->
            cartCounterCalculator.calculateItemCountForDecrease(
                state.currentCount,
                state.minItemCount,
                state.stepCount
            )
        }
        assertThat(actual).isEqualTo(expected.decreaseResult)
    }

    data class TestInputData(
        val logName: String,
        val isEmpty: Boolean,
        val currentCount: Int,
        val stepCount: Int,
        val minItemCount: Int,
        val availableCountInStock: Int
    )

    data class TestExpectedData(
        val increaseResult: Int,
        val decreaseResult: Int
    )

    companion object {

        @ParameterizedRobolectricTestRunner.Parameters(name = "{index}: {0}")
        @JvmStatic
        fun data(): Iterable<Array<Any?>> = listOf(
            // 0
            arrayOf(
                TestInputData(
                    logName = "Всё по 0",
                    isEmpty = true,
                    currentCount = 0,
                    stepCount = 0,
                    minItemCount = 0,
                    availableCountInStock = 0
                ),
                TestExpectedData(increaseResult = 0, decreaseResult = 0)
            ),
            // 1
            arrayOf(
                TestInputData(
                    logName =
                    "Количество на стоке больше, чем шаг прибавления и  минимальное количество для добавления в корзину",
                    isEmpty = true,
                    currentCount = 0,
                    stepCount = 2,
                    minItemCount = 2,
                    availableCountInStock = 3
                ),
                TestExpectedData(increaseResult = 2, decreaseResult = 0)
            ),
            // 2
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке == шаг прибавления + текущее количество",
                    isEmpty = false,
                    currentCount = 2,
                    stepCount = 2,
                    minItemCount = 2,
                    availableCountInStock = 4
                ),
                TestExpectedData(increaseResult = 4, decreaseResult = 0)
            ),
            // 3
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке < текущее количество",
                    isEmpty = false,
                    currentCount = 6,
                    stepCount = 1,
                    minItemCount = 2,
                    availableCountInStock = 5
                ),
                TestExpectedData(increaseResult = 6, decreaseResult = 5)
            ),
            // 4
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке < шаг прибавления + текущее количество",
                    isEmpty = false,
                    currentCount = 4,
                    stepCount = 2,
                    minItemCount = 2,
                    availableCountInStock = 5
                ),
                TestExpectedData(increaseResult = 6, decreaseResult = 2)
            ),
            // 5
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке < шаг прибавления + текущее количество",
                    isEmpty = false,
                    currentCount = 6,
                    stepCount = 2,
                    minItemCount = 2,
                    availableCountInStock = 5
                ),
                TestExpectedData(increaseResult = 6, decreaseResult = 4)
            ),
            // 6
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке < шаг прибавления + текущее количество",
                    isEmpty = false,
                    currentCount = 3,
                    stepCount = 2,
                    minItemCount = 2,
                    availableCountInStock = 4
                ),
                TestExpectedData(increaseResult = 5, decreaseResult = 0)
            ),
            // 7
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке < шаг прибавления + текущее количество",
                    isEmpty = true,
                    currentCount = 0,
                    stepCount = 2,
                    minItemCount = 4,
                    availableCountInStock = 3
                ),
                TestExpectedData(increaseResult = 3, decreaseResult = 0)
            ),
            // 8
            arrayOf(
                TestInputData(
                    logName = "Количество на стоке = 0",
                    isEmpty = true,
                    currentCount = 0,
                    stepCount = 1,
                    minItemCount = 1,
                    availableCountInStock = 0
                ),
                TestExpectedData(increaseResult = 1, decreaseResult = 0)
            ),
        )
    }
}