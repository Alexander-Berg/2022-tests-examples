package ru.yandex.market.clean.data.mapper.notifications.notification

import junit.framework.TestCase

import org.junit.Test

import org.junit.Assert.*
import ru.yandex.market.clean.data.model.dto.notifications.notification.SubscriptionNotificationDto
import ru.yandex.market.clean.data.model.dto.notifications.notification.SubscriptionNotificationTextDto
import ru.yandex.market.clean.data.model.dto.notifications.notification.SubscriptionNotificationTrailTypeDto
import ru.yandex.market.clean.data.model.dto.notifications.request.UpdateSubscriptionNotificationRequestDto
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotification
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationEntity
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationStatus
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationTrailType
import ru.yandex.market.clean.domain.model.notifications.notification.SubscriptionNotificationType
import java.util.Date

class SubscriptionNotificationMapperTest : TestCase() {

    private val mapper = SubscriptionNotificationMapper(
        SubscriptionNotificationEntityMapper(),
        SubscriptionNotificationStatusMapper(),
        SubscriptionNotificationTypeMapper(),
        SubscriptionNotificationTrailTypeMapper()
    )

    @Test
    fun `test map dto to model`() {
        val expected = SubscriptionNotification(
            id = ID,
            entity = SubscriptionNotificationEntity.SUBSCRIPTION,
            status = SubscriptionNotificationStatus.ENABLED,
            subtitle = SUBTITLE,
            title = TITLE,
            trailType = SubscriptionNotificationTrailType.SWITCH,
            type = SubscriptionNotificationType.PUSH,
            updated = Date(UPDATED_TIME)
        )

        val actual = mapper.map(
            SubscriptionNotificationDto(
                id = ID,
                entity = ENTITY,
                status = STATUS,
                style = STYLE,
                text = SubscriptionNotificationTextDto(
                    title = TITLE,
                    subtitle = SUBTITLE
                ),
                trailType = SubscriptionNotificationTrailTypeDto(type = TRAIL_TYPE),
                type = TYPE,
                updatedTime = UPDATED_TIME
            )
        ).throwError()

        assertEquals(expected, actual)
    }

    @Test
    fun `test map model to update dto`() {
        val expected = UpdateSubscriptionNotificationRequestDto(
            id = ID,
            entity = ENTITY,
            status = STATUS,
            type = TYPE,
            updatedTime = UPDATED_TIME
        )

        val actual = mapper.map(
            SubscriptionNotification(
                id = ID,
                entity = SubscriptionNotificationEntity.SUBSCRIPTION,
                status = SubscriptionNotificationStatus.ENABLED,
                subtitle = SUBTITLE,
                title = TITLE,
                trailType = SubscriptionNotificationTrailType.SWITCH,
                type = SubscriptionNotificationType.PUSH,
                updated = Date(UPDATED_TIME)
            )
        )

        assertEquals(expected, actual)
    }

    private companion object {
        const val ID = 1L
        const val ENTITY = "subscription"
        const val STATUS = "enabled"
        const val SUBTITLE = "some subtitle"
        const val TITLE = "some title"
        const val STYLE = "some style"
        const val TRAIL_TYPE = "switch"
        const val TYPE = "push_setting"
        const val UPDATED_TIME = 0L
    }
}