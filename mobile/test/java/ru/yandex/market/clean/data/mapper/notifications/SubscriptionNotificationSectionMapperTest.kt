package ru.yandex.market.clean.data.mapper.notifications

import junit.framework.TestCase
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.mapper.notifications.notification.SubscriptionNotificationMapper
import ru.yandex.market.clean.data.model.dto.notifications.notification.subscriptionNotificationDtoTestInstance
import ru.yandex.market.clean.data.model.dto.notifications.section.SubscriptionNotificationSectionDto
import ru.yandex.market.clean.data.model.dto.notifications.section.SubscriptionNotificationSectionLeadDto
import ru.yandex.market.clean.domain.model.notifications.notification.subscriptionNotificationTestInstance
import ru.yandex.market.clean.domain.model.notifications.section.SubscriptionNotificationSection
import ru.yandex.market.safe.Safe

class SubscriptionNotificationSectionMapperTest : TestCase() {

    private val notificationDtoMock = subscriptionNotificationDtoTestInstance()
    private val notificationMock = subscriptionNotificationTestInstance()
    private val notificationMapper = mock<SubscriptionNotificationMapper>() {
        on {
            map(notificationDtoMock)
        } doReturn Safe {
            notificationMock
        }
    }
    private val mapper = SubscriptionNotificationSectionMapper(notificationMapper)

    @Test
    fun `test map dto to model`() {
        val expected = SubscriptionNotificationSection(
            title = TITLE,
            subtitle = SUBTITLE,
            notifications = listOf(notificationMock)
        )

        val actual = mapper.map(
            SubscriptionNotificationSectionDto(
                sectionInfo = SubscriptionNotificationSectionLeadDto(
                    title = TITLE,
                    subtitle = SUBTITLE,
                    style = STYLE
                ),
                notifications = listOf(notificationDtoMock)
            )
        ).throwError()

        assertEquals(expected, actual)
    }

    private companion object {
        const val TITLE = "some title"
        const val SUBTITLE = "some subtitle"
        const val STYLE = "some style"
    }
}