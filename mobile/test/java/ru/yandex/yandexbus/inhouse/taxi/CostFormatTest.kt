package ru.yandex.yandexbus.inhouse.taxi

import android.content.res.Resources
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.R
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.whenever

@RunWith(Parameterized::class)
class CostFormatTest(private val testData: TestData) : BaseTest() {

    @Mock
    private lateinit var resources: Resources

    override fun setUp() {
        super.setUp()

        whenever(resources.getString(eq(R.string.taxi_estimated_price), any())).thenAnswer {
            "from ${it.arguments[1]}"
        }

        whenever(resources.getString(eq(R.string.value_currency), any(), any())).thenAnswer {
            "${it.arguments[1]} ${it.arguments[2]}"
        }

        whenever(resources.getString(eq(R.string.value_currency_in_rub), any())).thenAnswer {
            "${it.arguments[1]} $CURRENCY_SYMBOL"
        }
    }

    @Test
    fun test() {
        Assert.assertEquals(testData.expectedFormat, CostFormat.format(resources, testData.cost, testData.approximately))
    }

    data class TestData(val cost: Cost, val approximately: Boolean, val expectedFormat: String)

    private companion object {
        private const val CURRENCY_SYMBOL = "\u20BD"

        @JvmStatic
        @Parameterized.Parameters
        fun testData(): Collection<TestData> = listOf(
            TestData(Cost(3.0, "BYN", "3 byn"), true, "from 3 byn"),
            TestData(Cost(3.0, "BYN", "3 byn"), false, "3 byn"),
            TestData(
                Cost(99.0, "RUB", "shouldn't be used"),
                true,
                "from 99 $CURRENCY_SYMBOL"
            ),
            TestData(
                Cost(99.123, "RUB", "shouldn't be used"),
                true,
                "from 99.1 $CURRENCY_SYMBOL"
            ),
            TestData(Cost(123.0, null, "from 123"), true, "from 123")
        )
    }
}