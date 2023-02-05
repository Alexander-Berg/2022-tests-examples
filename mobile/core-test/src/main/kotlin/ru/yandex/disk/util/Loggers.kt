package ru.yandex.disk.util

import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.not
import ru.yandex.disk.event.Event
import ru.yandex.disk.event.EventLogger
import ru.yandex.disk.test.Assert2.assertThat

fun <T: Event> EventLogger.assertHasEvent(matcher: Matcher<T>) {
    val events = findAllByClass(Event::class.java)
    assertThat(events, Matchers.hasItem(matcher))
}

fun <T: Event> EventLogger.assertNoEvent(matcher: Matcher<T>) {
    val events = findAllByClass(Event::class.java)
    assertThat(events, not(Matchers.hasItem(matcher)))
}

inline fun <reified T: Event> EventLogger.assertHasEvent() {
    assertHasEvent(instanceOf(T::class.java))
}

inline fun <reified T: Event> EventLogger.assertNoEvent() {
    assertNoEvent(instanceOf(T::class.java))
}