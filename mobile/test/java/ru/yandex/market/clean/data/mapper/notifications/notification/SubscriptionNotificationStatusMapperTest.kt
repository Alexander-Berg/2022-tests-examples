package ru.yandex.market.clean.data.mapper.notifications.notification

import junit.framework.TestCase
import org.junit.Assert.assertThrows
import org.junit.Test
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationStatus
import kotlin.IllegalArgumentException

class SubscriptionNotificationStatusMapperTest : TestCase() {

    private val mapper = SubscriptionNotificationStatusMapper()

    @Test
    fun `test map dto to model`() {
        val listOfValues = listOf("enabled", "disabled", "pending")
        val expected = SubscriptionNotificationStatus.values().toList()

        val actual = listOfValues.map { mapper.map(it).throwError() }

        assertEquals(expected, actual)
    }

    @Test
    fun `test map model to dto`() {
        val listOfValues = SubscriptionNotificationStatus.values().toList()
        val expected = listOf("enabled", "disabled", "pending")

        val actual = listOfValues.map { mapper.map(it) }

        assertEquals(expected, actual)
    }

    @Test
    fun `test map dto to model error`() {
        assertThrows(IllegalArgumentException::class.java) {
            mapper.map("some invalid string").throwError()
        }
    }
}