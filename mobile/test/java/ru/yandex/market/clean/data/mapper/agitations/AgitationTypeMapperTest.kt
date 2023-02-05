package ru.yandex.market.clean.data.mapper.agitations

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.agitations.AgitationType
import java.util.Locale

@RunWith(Parameterized::class)
class AgitationTypeMapperTest(
    private val dtoString: String?,
    private val expectedResult: AgitationType
) {

    private lateinit var mapper: AgitationTypeMapper

    @Before
    fun setUp() {
        mapper = AgitationTypeMapper()
    }

    @Test
    fun `Test agitation type is mapped correctly from dto string`() {
        assertThat(mapper.map(dtoString)).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters(name = "{index}: \"{0}\" -> {1}")
        @JvmStatic
        fun data(): List<Array<*>> {
            return AgitationType.values().flatMap { listOf(arrayOf(it.name, it),
                arrayOf(it.name.lowercase(Locale.getDefault()), it)) } +
                    listOf<Array<*>>(
                        arrayOf("", AgitationType.UNKNOWN),
                        arrayOf(null, AgitationType.UNKNOWN),
                        arrayOf("ORDER_CANCELLED_BY_USER_EXTERNALLY", AgitationType.ORDER_CANCELLED_BY_USER_EXTERNALLY),
                        arrayOf("order_cancelled_by_user_externally", AgitationType.ORDER_CANCELLED_BY_USER_EXTERNALLY)
                    )
        }
    }
}