package ru.yandex.market.clean.presentation.feature.notifications.fragment.formatter

import org.junit.Test

import org.junit.Assert.*
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotification
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationEntity
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationStatus
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationTrailType
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationType
import ru.yandex.market.clean.presentation.feature.notifications.fragment.viewobject.NotificationToggleVo
import java.util.Date

class NotificationToggleFormatterTest {

    private val formatter = NotificationToggleFormatter()

    @Test
    fun format() {
        val expected = NotificationToggleVo(
            id = ID,
            isEnabled = IS_ENABLED,
            status = status,
            subtitle = SUBTITLE,
            title = TITLE
        )

        val actual = formatter.format(
            SubscriptionNotification(
                id = ID,
                entity = SubscriptionNotificationEntity.SUBSCRIPTION,
                status = status,
                subtitle = SUBTITLE,
                title = TITLE,
                trailType = SubscriptionNotificationTrailType.SWITCH,
                type = SubscriptionNotificationType.PUSH,
                updated = Date()
            ),
            isNotificationEnabled = true
        )

        assertEquals(expected, actual)
    }

    private companion object {
        const val ID = 1L
        const val IS_ENABLED = true
        val status = SubscriptionNotificationStatus.ENABLED
        const val TITLE = "some title"
        const val SUBTITLE = "some subtitle"
    }
}