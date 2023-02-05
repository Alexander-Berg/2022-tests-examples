package ru.yandex.market.clean.data.fapi.mapper

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.market.clean.data.fapi.dto.OrderCourierTrackingDto

class CourierTrackingUrlDtoMapperTest {

    private val mapper = CourierTrackingUrlDtoMapper()

    @Test
    fun `Map properly formatted dtos`() {
        val testTrackingDtos = listOf(
            OrderCourierTrackingDto(
                orderId = TEST_ORDER_ID,
                trackingUrl = TEST_TRACKING_URL
            )
        )

        val actualUrl = mapper.map(TEST_ORDER_ID, testTrackingDtos)

        assertEquals(TEST_TRACKING_URL, actualUrl)
    }

    @Test(expected = IllegalStateException::class)
    fun `Map dtos with empty url`() {
        val testTrackingDtos = listOf(
            OrderCourierTrackingDto(
                orderId = TEST_ORDER_ID,
                trackingUrl = ""
            )
        )

        mapper.map(TEST_ORDER_ID, testTrackingDtos)
    }

    @Test(expected = IllegalStateException::class)
    fun `Map dtos with wrong orderId`() {
        val testTrackingDtos = listOf(
            OrderCourierTrackingDto(
                orderId = WRONG_TEST_ORDER_ID,
                trackingUrl = TEST_TRACKING_URL
            )
        )
        mapper.map(TEST_ORDER_ID, testTrackingDtos)
    }

    companion object {
        private const val TEST_ORDER_ID = "1234"
        private const val WRONG_TEST_ORDER_ID = "4321"
        private const val TEST_TRACKING_URL = "https://some/url"
    }
}