package ru.yandex.market.clean.data.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.data.order.options.PromoType
import java.util.Locale

@RunWith(Parameterized::class)
class PromoTypeMapperTest(
    private val inputString: String?,
    private val expectedResult: PromoType
) {

    private val mapper = PromoTypeMapper()

    @Test
    fun `Check actual mapped result matches expected`() {
        assertThat(mapper.map(inputString)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
        @JvmStatic
        fun data(): List<Array<*>> {
            return PromoType.values().flatMap { listOf(arrayOf(it.name, it),
                arrayOf(it.name.lowercase(Locale.getDefault()), it)) } +
                    listOf<Array<*>>(
                        arrayOf("", PromoType.UNKNOWN),
                        arrayOf("", PromoType.UNKNOWN),
                        arrayOf(null, PromoType.UNKNOWN),
                        arrayOf("PRICE_DROP_AS_YOU_SHOP", PromoType.PRICE_DROP),
                        arrayOf("price_drop_as_you_shop", PromoType.PRICE_DROP)
                    )
        }
    }
}