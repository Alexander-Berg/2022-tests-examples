package ru.yandex.market.clean.presentation.feature.notifications.fragment.formatter

import org.junit.Test

import org.junit.Assert.*
import ru.yandex.market.clean.domain.model.notifications.section.SubscriptionNotificationSection
import ru.yandex.market.clean.presentation.feature.notifications.fragment.viewobject.NotificationSectionVo

class NotificationSectionFormatterTest {

    private val formatter = NotificationSectionFormatter()

    @Test
    fun format() {
        val expected = NotificationSectionVo(title = TITLE)

        val actual = formatter.format(
            SubscriptionNotificationSection(
                title = TITLE,
                subtitle = SUBTITLE,
                notifications = emptyList()
            )
        )

        assertEquals(expected, actual)
    }

    private companion object {
        const val TITLE = "some title"
        const val SUBTITLE = "some subtitle"
    }
}