package ru.yandex.market.clean.data.mapper.fapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.data.money.mapper.TermMapper
import ru.yandex.market.data.money.dto.CreditTermDto
import ru.yandex.market.data.money.dto.FapiTermDto

@RunWith(Parameterized::class)
class TermMapperTest(private val input: FapiTermDto?, private val expected: CreditTermDto?) {

    val mapper = TermMapper()

    @Test
    fun `Correctly map FapiTermDto to CreditTermDto`() {
        val mapped = mapper.map(input)
        assertThat(mapped).isEqualTo(expected)

    }

    companion object {

        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<Any?>> = listOf<Array<Any?>>(
            arrayOf(FapiTermDto(3, 8), CreditTermDto(3, 8)),
            arrayOf(FapiTermDto(null, 5), CreditTermDto(null, 5)),
            arrayOf(FapiTermDto(1, null), CreditTermDto(1, null)),
            arrayOf(FapiTermDto(null, null), CreditTermDto(null, null)),
            arrayOf(null, null)
        )
    }
}