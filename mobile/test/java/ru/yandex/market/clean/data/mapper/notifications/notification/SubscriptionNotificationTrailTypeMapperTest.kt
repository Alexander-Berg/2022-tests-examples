package ru.yandex.market.clean.data.mapper.notifications.notification

import junit.framework.TestCase
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationTrailType

class SubscriptionNotificationTrailTypeMapperTest : TestCase() {

    private val mapper = SubscriptionNotificationTrailTypeMapper()

    @Test
    fun `test map dto to model`() {
        val expected = SubscriptionNotificationTrailType.values().toList()

        val actual = listOf("switch").map { mapper.map(it).throwError() }

        assertEquals(expected, actual)
    }

    @Test
    fun `test map dto to model error`() {
        assertThrows(IllegalArgumentException::class.java) {
            mapper.map("some invalid string").throwError()
        }
    }
}