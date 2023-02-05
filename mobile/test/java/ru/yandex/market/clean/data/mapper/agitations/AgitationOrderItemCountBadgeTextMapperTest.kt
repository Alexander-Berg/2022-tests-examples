package ru.yandex.market.clean.data.mapper.agitations

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AgitationOrderItemCountBadgeTextMapperTest(
    private val inputInt: Int?,
    private val expectedResult: String
) {
    private lateinit var mapper: AgitationOrderItemCountBadgeTextMapper

    @Before
    fun setUp() {
        mapper = AgitationOrderItemCountBadgeTextMapper()
    }

    @Test
    fun `Test order item count is mapped correctly to the badge text`() {
        assertThat(mapper.map(inputInt)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
        @JvmStatic
        fun data(): List<Array<*>> {
            return listOf<Array<*>>(
                arrayOf(0, ""),
                arrayOf(1, ""),
                arrayOf(2, "2"),
                arrayOf(123, "123"),
                arrayOf(null, "")
            )
        }
    }
}