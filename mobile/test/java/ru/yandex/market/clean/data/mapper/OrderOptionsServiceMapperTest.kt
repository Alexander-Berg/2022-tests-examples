package ru.yandex.market.clean.data.mapper

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import ru.yandex.market.clean.data.fapi.dto.OrderServiceDto
import ru.yandex.market.common.datetimeparser.DateTimeParser
import ru.yandex.market.data.order.OrderOptionsServiceDto

class OrderOptionsServiceMapperTest {
    private val dateTimeParser = mock<DateTimeParser>()
    private val serviceTimeIntervalMapper = mock<ServiceTimeIntervalMapper>()

    private val orderOptionsServiceMapper = OrderOptionsServiceMapper(
        dateTimeParser = dateTimeParser,
        serviceTimeIntervalMapper = serviceTimeIntervalMapper,
    )

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Test
    fun `test require service id`() {
        val res = orderOptionsServiceMapper.map(
            dto = OrderOptionsServiceDto(
                id = null,
                title = null,
                date = null,
                description = null,
                price = null,
                fromTime = null,
                toTime = null,
            ),
            currency = null,
        )
        assertThat(res).isNull()
    }

    @Test
    fun `test require service id 2`() {
        val res = orderOptionsServiceMapper.map(
            dto = OrderServiceDto(
                id = null,
                title = null,
                date = null,
                description = null,
                price = null,
                fromTime = null,
                toTime = null,
                orderId = null,
                status = null,
                count = null,
            ),
            currency = null,
        )
        assertThat(res).isNull()
    }
}