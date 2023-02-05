package ru.yandex.market.clean.domain

import org.hamcrest.Matcher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.test.extensions.instanceOf
import ru.yandex.market.test.matchers.hasFailureValue
import ru.yandex.market.test.matchers.hasSuccessValue
import ru.yandex.market.test.matchers.isCloseTo
import ru.yandex.market.utils.SuccessOrFailure
import ru.yandex.market.rub
import java.math.BigDecimal

private typealias Result = SuccessOrFailure<Float, DiscountCalculationError>

@RunWith(Parameterized::class)
class DiscountCalculatorTest(
    private val firstPrice: Money,
    private val secondPrice: Money,
    private val resultMatcher: Matcher<Result>
) {
    private val calculator = DiscountCalculator()

    @Test
    fun `Calculated result matches expectation`() {
        assertThat(calculator.calculateDiscount(firstPrice, secondPrice)).`is`(HamcrestCondition(resultMatcher))
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: calculateDiscount({0}, {1}) == {2}")
        @JvmStatic
        fun data() = listOf<Array<*>>(
            arrayOf(100.rub, 80.rub, isSuccess(0.20f)),
            arrayOf(80.rub, 100.rub, isSuccess(0.20f)),
            arrayOf(100.rub, 100.rub, isSuccess(0.0f)),
            arrayOf(100.rub, 1.rub, isSuccess(0.99f)),
            arrayOf(100.rub, 30.rub, isSuccess(0.70f)),
            arrayOf(100.rub, 67.rub, isSuccess(0.33f)),
            arrayOf(80.rub, 42.rub, isSuccess(0.475f)),
            arrayOf(80.rub, 42.rub, isSuccess(0.475f)),
            arrayOf(300.rub, 1.rub, isSuccess(0.996f)),

            arrayOf(0.rub, 42.rub, isFailure<DiscountCalculationError.IncorrectAmount>()),
            arrayOf(100.rub, 0.rub, isFailure<DiscountCalculationError.IncorrectAmount>()),
            arrayOf(80.rub, (-100).rub, isFailure<DiscountCalculationError.IncorrectAmount>()),
            arrayOf((-100).rub, 80.rub, isFailure<DiscountCalculationError.IncorrectAmount>()),
            arrayOf(
                Money(BigDecimal.TEN, Currency.BYN),
                100.rub,
                isFailure<DiscountCalculationError.CurrencyDiffers>()
            ),
            arrayOf(
                100.rub,
                Money(BigDecimal.TEN, Currency.BYN),
                isFailure<DiscountCalculationError.CurrencyDiffers>()
            ),
            arrayOf(
                Money(BigDecimal.TEN, Currency.UNKNOWN),
                Money(BigDecimal.ONE, Currency.UNKNOWN),
                isFailure<DiscountCalculationError.CurrencyUnknown>()
            ),
            arrayOf(
                Money(BigDecimal.TEN, Currency.UNKNOWN),
                Money(BigDecimal.ONE, Currency.UNKNOWN),
                isFailure<DiscountCalculationError.CurrencyUnknown>()
            )
        )

        private fun isSuccess(value: Float): Matcher<Result> {
            return hasSuccessValue(isCloseTo(value, 0.001f))
        }

        private inline fun <reified T : DiscountCalculationError> isFailure(): Matcher<Result> {
            return hasFailureValue(instanceOf<DiscountCalculationError>(T::class))
        }
    }
}