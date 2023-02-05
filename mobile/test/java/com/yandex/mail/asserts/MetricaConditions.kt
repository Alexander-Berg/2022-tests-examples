package com.yandex.mail.asserts

import com.yandex.mail.metrica.Event
import com.yandex.mail.metrica.MetricaConstns.Notification
import com.yandex.mail.metrica.MockYandexMailMetricaModule
import com.yandex.mail.util.Utils
import org.assertj.core.api.Condition
import java.util.Arrays

object MetricaConditions {

    fun notificationEvent(
        eventName: String,
        expectedUid: Long,
        vararg expectedMids: Long
    ): Condition<MockYandexMailMetricaModule.TestYandexMailMetrica> {
        return object : Condition<MockYandexMailMetricaModule.TestYandexMailMetrica>() {
            override fun matches(testYandexMailMetrica: MockYandexMailMetricaModule.TestYandexMailMetrica): Boolean {
                val event = testYandexMailMetrica.getEvents().findLast { (name) -> name == eventName } ?: return false
                val attributes = event.attributes ?: return false

                val uidExtraIsOk: Boolean
                val messageExtraIsOk: Boolean

                val actualUidParam = attributes[Notification.EXTRA_UID] as Long
                uidExtraIsOk = expectedUid == actualUidParam

                messageExtraIsOk =
                    if (expectedMids.isEmpty()) {
                        !attributes.containsKey(Notification.EXTRA_MESSAGE_IDS)
                    } else {
                        val expectedMidsParam = Arrays.toString(expectedMids)
                        val actualMidsParam = attributes[Notification.EXTRA_MESSAGE_IDS] as String
                        expectedMidsParam == actualMidsParam
                    }

                return uidExtraIsOk && messageExtraIsOk
            }

            override fun toString(): String {
                val expectedMidsParam =
                    if (expectedMids.isEmpty()) {
                        null
                    } else {
                        Arrays.toString(expectedMids)
                    }

                return String.format(
                    "Event with event name %s, and extra values %s = %d, %s = %s",
                    eventName,
                    Notification.EXTRA_UID,
                    expectedUid,
                    Notification.EXTRA_MESSAGE_IDS,
                    expectedMidsParam
                )
            }
        }
    }

    @JvmStatic
    fun notificationEventWithName(eventName: String): Condition<Event> {
        return object : Condition<Event>() {
            override fun matches(event: Event): Boolean {
                return event.name == eventName
            }

            override fun toString(): String {
                return String.format("Event with event name %s", eventName)
            }
        }
    }

    @JvmStatic
    fun notificationEventAttribute(key: String, value: Any): Condition<Event> {
        return object : Condition<Event>() {
            override fun matches(event: Event): Boolean {
                val attributes = event.attributes ?: return false
                val valueFromEvent = attributes[key] ?: return false
                return Utils.equals(valueFromEvent, value)
            }

            override fun toString(): String {
                return String.format("Event with attribute: key <%s>, value <%s>", key, value)
            }
        }
    }
}
