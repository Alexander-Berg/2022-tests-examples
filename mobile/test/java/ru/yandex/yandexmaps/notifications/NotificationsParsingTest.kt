package ru.yandex.yandexmaps.notifications

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexmaps.common.jsonadapters.ColorAdapter
import ru.yandex.yandexmaps.common.utils.extensions.adapterForType
import ru.yandex.yandexmaps.common.utils.extensions.moshi.addCommonPointAdapter
import ru.yandex.yandexmaps.notifications.internal.NotificationJsonModel
import java.text.SimpleDateFormat
import java.util.*

internal class NotificationsParserTest {
    private lateinit var moshi: Moshi
    private lateinit var iso8601Format: SimpleDateFormat

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .addCommonPointAdapter()
            .add(ColorAdapter())
            .build()

        iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    @Test
    fun testParsingNotification() {
        val result = moshi.adapterForType<NotificationJsonModel>().fromJson(notificationJson)

        val startDate = iso8601Format.parse("2019-11-30T21:00:00.000Z")!!
        val endDate = iso8601Format.parse("2019-12-31T20:59:59.999Z")!!

        val expectedNotification = NotificationJsonModel(
            id = "d5a631601cc84k41ebwx0",
            endDate = endDate,
            startDate = startDate,
            bannerInner = NotificationJsonModel.Template(urlTemplate = "https://avatars.mdst.yandex.net/get-discovery-int/69165/2a0000016e64c06c51dab9a0b64026b3119e/%s"),
            actionInner = NotificationJsonModel.ActionImpl(url = "https://yandex.ru/maps/org/1025596695"),
            message = "Уведомление 1",
            typesInner = listOf(NotificationJsonModel.Type.DISCOVERY)
        )
        assertEquals(expectedNotification, result)
    }

    @Test
    fun testParsingNotificationWithoutAction() {
        val result = moshi.adapterForType<NotificationJsonModel>().fromJson(notificationWithoutActionJson)

        val startDate = iso8601Format.parse("2019-11-30T21:00:00.000Z")!!
        val endDate = iso8601Format.parse("2019-12-31T20:59:59.999Z")!!

        val expectedNotification = NotificationJsonModel(
            id = "d5a631601cc84k41ebwx0",
            endDate = endDate,
            startDate = startDate,
            bannerInner = NotificationJsonModel.Template(urlTemplate = "https://avatars.mdst.yandex.net/get-discovery-int/69165/2a0000016e64c06c51dab9a0b64026b3119e/%s"),
            actionInner = null,
            message = "Уведомление 1",
            typesInner = listOf(NotificationJsonModel.Type.DISCOVERY)
        )
        assertEquals(expectedNotification, result)
    }

    private val notificationJson = """
        {
              "id": "d5a631601cc84k41ebwx0",
              "action": {
                      "type": "oid",
                      "url": "https://yandex.ru/maps/org/1025596695"
                    },
              "endDate": "2019-12-31T20:59:59.999Z",
              "startDate": "2019-11-30T21:00:00.000Z",
              "bannerImage": {
                "urlTemplate": "https://avatars.mdst.yandex.net/get-discovery-int/69165/2a0000016e64c06c51dab9a0b64026b3119e/%s"
              },
              "description": "Уведомление 1",
              "types": ["discovery"]
        }
    """.trimIndent()

    private val notificationWithoutActionJson = """
        {
              "id": "d5a631601cc84k41ebwx0",
              "endDate": "2019-12-31T20:59:59.999Z",
              "startDate": "2019-11-30T21:00:00.000Z",
              "bannerImage": {
                "urlTemplate": "https://avatars.mdst.yandex.net/get-discovery-int/69165/2a0000016e64c06c51dab9a0b64026b3119e/%s"
              },
              "description": "Уведомление 1",
              "types": ["discovery"]
        }
    """.trimIndent()
}
