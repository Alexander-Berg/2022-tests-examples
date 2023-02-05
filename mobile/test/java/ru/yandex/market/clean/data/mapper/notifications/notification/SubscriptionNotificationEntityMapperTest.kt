package ru.yandex.market.clean.data.mapper.notifications.notification

import junit.framework.TestCase
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationEntity

class SubscriptionNotificationEntityMapperTest : TestCase() {

    private val mapper = SubscriptionNotificationEntityMapper()

    @Test
    fun `test map dto to model`() {
        val expected = SubscriptionNotificationEntity.values().toList()

        val actual = listOf(mapper.map("subscription").throwError())

        assertEquals(expected, actual)
    }

    @Test
    fun `test map model to dto`() {
        val expected = listOf("subscription")

        val actual = SubscriptionNotificationEntity.values().map { mapper.map(it) }

        assertEquals(expected, actual)
    }

    @Test
    fun `test map dto to model error`() {
        assertThrows(IllegalArgumentException::class.java) {
            mapper.map("some invalid string").throwError()
        }
    }
}