package ru.yandex.market.clean.data.mapper.notifications.notification

import junit.framework.TestCase
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationType

class SubscriptionNotificationTypeMapperTest : TestCase() {

    private val mapper = SubscriptionNotificationTypeMapper()

    @Test
    fun `test map dto to model`() {
        val expected = SubscriptionNotificationType.values().toList()

        val actual = listOf("push_setting").map { mapper.map(it).throwError() }

        assertEquals(expected, actual)
    }

    @Test
    fun `test map model to dto`() {
        val expected = listOf("push_setting")

        val actual = SubscriptionNotificationType.values().map { mapper.map(it) }

        assertEquals(expected, actual)
    }

    @Test
    fun `test map dto to model error`() {
        assertThrows(IllegalArgumentException::class.java) {
            mapper.map("some invalid string").throwError()
        }
    }
}