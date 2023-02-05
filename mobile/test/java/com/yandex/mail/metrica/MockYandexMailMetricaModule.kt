package com.yandex.mail.metrica

import androidx.annotation.StringRes
import com.yandex.mail.BaseMailApplication
import com.yandex.mail.util.log.LogUtils
import com.yandex.metrica.IIdentifierCallback
import com.yandex.xplat.eventus.Eventus
import com.yandex.xplat.eventus.common.EventReporter
import com.yandex.xplat.eventus.common.EventusRegistry
import com.yandex.xplat.eventus.common.LoggingEvent
import dagger.Module
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import timber.log.Timber
import kotlin.collections.set

@Module
class MockYandexMailMetricaModule : YandexMetricaModule() {
    override fun provideYandexMailMetrica(context: BaseMailApplication): YandexMailMetrica {
        val metrica = TestYandexMailMetrica(context)
        Eventus.setup()
        EventusRegistry.setEventReporter(MockEventReporter(metrica))
        metrica.init()
        return metrica
    }

    class TestYandexMailMetrica internal constructor(private val context: BaseMailApplication) : YandexMailMetrica {

        private val events = ArrayList<Event>()

        private val statboxEvents = ArrayList<StatboxEvent>()

        private val mapAppEnvironment = HashMap<String, String?>()

        val lastEvent: Event?
            get() = events.lastOrNull()

        val lastStatboxEvent: StatboxEvent?
            get() = statboxEvents.lastOrNull()

        override fun init() {
            // no-op
        }

        override fun requestStartupIdentifiers(callback: IIdentifierCallback) {
            // no-op
        }

        override fun reportEvent(name: String) {
            Timber.tag(METRICA_TAG).i(name)
            addEvent(Event.create(name))
        }

        override fun reportEvent(@StringRes resId: Int) {
            addEvent(Event.create(getString(resId)))
        }

        override fun reportError(message: String, throwable: Throwable?) {
            Timber.tag(METRICA_TAG).e(throwable, message)
            addEvent(Event.create(message, throwable))
        }

        override fun reportError(groupIdentifier: String, message: String, throwable: Throwable?) {
            Timber.tag(METRICA_TAG).e(throwable, "$groupIdentifier: $message")
        }

        override fun reportWtf(message: String) {
            reportError(message, RuntimeException("Should not have happened"))
            LogUtils.crashIfDebugAndLogAnyway(message)
        }

        override fun reportWtf(message: String, throwable: Throwable) {
            Timber.e(throwable)
            reportError(message, throwable)
            LogUtils.crashIfDebugAndLogAnyway(message)
        }

        override fun reportWtf(groupIdentifier: String, message: String, throwable: Throwable?) {
            Timber.e(throwable)
            reportError(groupIdentifier, message, throwable)
            LogUtils.crashIfDebugAndLogAnyway(message)
        }

        override fun getString(@StringRes resId: Int, vararg args: Any): String = context.getString(resId, *args)

        override fun reportEvent(eventName: String, attributes: Map<String, Any>) {
            Timber.tag(METRICA_TAG).e("%s %s", eventName, attributes)
            addEvent(Event.create(eventName, attributes))
        }

        override fun reportAppOpen(deeplink: String) {
            Timber.tag(METRICA_TAG).v("reportAppOpen %s", deeplink)
        }

        override fun reportReferralUrl(deeplink: String) {
            Timber.tag(METRICA_TAG).v("reportReferralUrl %s", deeplink)
        }

        override fun getUUID() = "abcdef"

        override fun getDeviceId() = "abacaba"

        override fun putAppEnvironmentValue(key: String, value: String?) {
            mapAppEnvironment[key] = value
        }

        override fun reportStatboxEvent(key: String, value: String) {
            statboxEvents.add(StatboxEvent(key, value))
        }

        override fun reportStatboxEvent(key: String, attributes: Map<String, Any>) {
            statboxEvents.add(StatboxEvent(key, null, attributes))
        }

        override fun clearAppEnvironment() {
            mapAppEnvironment.clear()
        }

        fun getEvents() = events

        private fun addEvent(event: Event) {
            events.add(event)
        }

        fun assertNoEvents() {
            assertThat(lastEvent).isNull()
        }

        fun assertNoEvent(name: String) {
            assertThat(events).doNotHave(object : Condition<Event>() {
                override fun matches(event: Event) = event.name == name
            })
        }

        fun clearEvents() {
            events.clear()
        }

        @JvmOverloads
        fun assertEvent(name: String, expectedCount: Int = 1) {
            val actualCount = events.count { (name1) -> name1 == name }
            assertThat(actualCount).isEqualTo(expectedCount)
        }

        fun assertEvent(@StringRes nameRes: Int) {
            assertEvent(getString(nameRes))
        }

        fun assertLastEvent(name: String) {
            val lastEvent = lastEvent
            assertThat(lastEvent).isNotNull()
            assertThat(lastEvent!!.name).isEqualTo(name)
        }

        fun assertLastEvent(@StringRes nameRes: Int) {
            assertLastEvent(getString(nameRes))
        }

        fun assertLastEvent(name: String, expectedExtras: Map<String, Any>) {
            val lastEvent = lastEvent
            assertThat(lastEvent).isNotNull()
            assertThat(lastEvent!!.name).isEqualTo(name)

            val attributes = lastEvent.attributes
            assertThat(attributes).isNotNull

            for (expectedEntry in expectedExtras.entries) {
                assertThat(attributes).contains(expectedEntry)
            }
        }

        @JvmOverloads
        fun assertError(name: String, throwable: Throwable, expectedCount: Int = 1) {
            val actualCount = events
                .count { (eventName, _, eventThrowable) ->
                    eventThrowable != null &&
                        eventThrowable.message?.equals(throwable.message) ?: (throwable.message == null) &&
                        eventName == name
                }
            assertThat(actualCount).isEqualTo(expectedCount)
        }

        @JvmOverloads
        fun <T : Throwable> assertErrorInstanceOf(name: String, throwable: Class<T>, expectedCount: Int = 1) {
            val actualCount = events
                .count { (eventName, _, eventThrowable) ->
                    eventThrowable != null && throwable.isInstance(eventThrowable) && eventName == name
                }
            assertThat(actualCount).isEqualTo(expectedCount)
        }

        fun getStatboxEvents(): List<StatboxEvent> {
            return statboxEvents
        }

        private fun addStatboxEvent(event: StatboxEvent) {
            statboxEvents.add(event)
        }

        fun assertNoStatboxEvents() {
            assertThat(lastStatboxEvent).isNull()
        }

        fun clearStatboxEvents() {
            statboxEvents.clear()
        }

        @JvmOverloads
        fun assertStatboxEvent(name: String, expectedCount: Int = 1) {
            val actualCount = statboxEvents.count { (name1) -> name1 == name }
            assertThat(actualCount).isEqualTo(expectedCount)
        }

        fun assertStatboxEvent(name: String, value: String) {
            val event = statboxEvents.firstOrNull { statboxEvent -> statboxEvent.name.equals(name) && statboxEvent.value.equals(value) }
            assertThat(event).isNotNull()
        }

        fun assertLastStatboxEvent(name: String) {
            val lastEvent = lastStatboxEvent
            assertThat(lastEvent).isNotNull()
            assertThat(lastEvent!!.name).isEqualTo(name)
        }

        fun assertLastStatboxEvent(name: String, expectedExtras: Map<String, Any?>) {
            val lastEvent = lastStatboxEvent
            assertThat(lastEvent).isNotNull()
            assertThat(lastEvent!!.name).isEqualTo(name)

            val attributes = lastEvent.attributes
            assertThat(attributes).isNotNull

            for (expectedEntry in expectedExtras.entries) {
                assertThat(attributes).contains(expectedEntry)
            }
        }

        fun getMapAppEnvironment() = mapAppEnvironment

        fun clearEnvironment() {
            mapAppEnvironment.clear()
        }

        fun assertNoEnvironmentValues() {
            assertThat(mapAppEnvironment).isEmpty()
        }

        override fun toString() = "TestMetrica"

        companion object {

            private const val METRICA_TAG = "[METRICA] "
        }
    }

    class MockEventReporter(private val metrica: YandexMailMetrica) : EventReporter {

        override fun report(event: LoggingEvent) {
            metrica.reportEvent(event.name, event.attributes)
        }
    }
}
