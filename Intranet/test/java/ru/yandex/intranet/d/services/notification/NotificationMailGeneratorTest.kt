package ru.yandex.intranet.d.services.notification

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.i18n.Locales
import ru.yandex.intranet.d.model.accounts.OperationErrorKind
import ru.yandex.intranet.d.services.notifications.NotificationMailGenerator
import java.util.*

/**
 * Notification mail generator test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
class NotificationMailGeneratorTest(@Autowired private val notificationMailGenerator: NotificationMailGenerator) {

    @Test
    fun testHtmlFailedOperationNotification() {
        val resultOne = notificationMailGenerator.generateHtmlFailedOperationNotification(UUID.randomUUID().toString(),
            null, OperationErrorKind.EXPIRED, Locales.ENGLISH)
        Assertions.assertNotNull(resultOne)
        val resultTwo = notificationMailGenerator.generateHtmlFailedOperationNotification(UUID.randomUUID().toString(),
            null, OperationErrorKind.EXPIRED, Locales.ENGLISH)
        Assertions.assertNotNull(resultTwo)
    }

    @Test
    fun testTextFailedOperationNotification() {
        val resultOne = notificationMailGenerator.generateTextFailedOperationNotification(UUID.randomUUID().toString(),
            UUID.randomUUID().toString(), OperationErrorKind.EXPIRED, Locales.ENGLISH)
        Assertions.assertNotNull(resultOne)
        val resultTwo = notificationMailGenerator.generateTextFailedOperationNotification(UUID.randomUUID().toString(),
            null, OperationErrorKind.EXPIRED, Locales.ENGLISH)
        Assertions.assertNotNull(resultTwo)
    }

}
