package com.edadeal.android.data.room

import com.edadeal.android.model.entity.CalculatedPrice
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.TypeSafeMatcher
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ConverterCalculatedPriceListToBytesTest(private val original: List<CalculatedPrice>) {

    companion object {
        private val cp0 = CalculatedPrice(price = 1.1f, quantity = 1.2f, quantityUnit = "хз", isDefault = true)
        private val cp1 = CalculatedPrice(price = .1f, quantity = .2f, quantityUnit = "нт", isDefault = true)
        private val cp2 = CalculatedPrice(price = 1f / 3f, quantity = 2f / 3f, quantityUnit = "", isDefault = false)

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<List<CalculatedPrice>> = listOf(
            emptyList(),
            listOf(CalculatedPrice.EMPTY),
            listOf(cp0),
            listOf(cp1, cp0, cp2)
        )
    }

    @Test
    fun `assert that conversion does not change data`() {
        assertThat(original, containsInAnyOrder(convert(original)))
    }

    private fun convert(items: List<CalculatedPrice>): List<TypeSafeMatcher<CalculatedPrice>> {
        val isEquals = { a: CalculatedPrice, b: CalculatedPrice ->
            a.quantity.toBits() == b.quantity.toBits() && a.price.toBits() == b.price.toBits() &&
                a.quantityUnit == b.quantityUnit && a.isDefault == b.isDefault
        }
        val converter = Converter()
        return converter.bytesToCalculatedPriceList(converter.calculatedPriceListToBytes(items)).orEmpty()
            .map { makeMatcher(it, isEquals) }
    }
}
