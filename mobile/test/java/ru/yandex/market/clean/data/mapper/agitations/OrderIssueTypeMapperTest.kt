package ru.yandex.market.clean.data.mapper.agitations

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.agitations.AgitationType
import ru.yandex.market.clean.domain.model.agitations.OrderIssueType

@RunWith(Parameterized::class)
class OrderIssueTypeMapperTest(
    private val agitationType: AgitationType,
    private val expectedResult: OrderIssueType
) {

    private lateinit var mapper: OrderIssueTypeMapper

    @Before
    fun setUp() {
        mapper = OrderIssueTypeMapper()
    }

    @Test
    fun `Test correct mapping between agitation type and order issue type`() {
        assertThat(mapper.map(agitationType)).isEqualTo(expectedResult)
    }

    companion object {
        @Parameterized.Parameters(name = "{index}: {0} -> {1}")
        @JvmStatic
        fun data(): Iterable<Array<*>> = listOf(
            arrayOf(
                AgitationType.ORDER_CANCELLED_BY_USER_EXTERNALLY,
                OrderIssueType.ORDER_CANCELLED_BY_USER_EXTERNALLY
            ),
            arrayOf(
                AgitationType.ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY,
                OrderIssueType.ORDER_DELIVERY_DATE_CHANGED_BY_USER_EXTERNALLY
            ),
            arrayOf(
                AgitationType.ORDER_DELIVERY_DATE_CHANGED_BY_SHOP,
                OrderIssueType.UNKNOWN
            ),
            arrayOf(
                AgitationType.ORDER_ITEM_REMOVAL,
                OrderIssueType.UNKNOWN
            ),
            arrayOf(
                AgitationType.UNKNOWN,
                OrderIssueType.UNKNOWN
            ),
        )
    }
}