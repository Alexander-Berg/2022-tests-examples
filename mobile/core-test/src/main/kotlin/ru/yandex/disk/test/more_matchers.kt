package ru.yandex.disk.test

import org.hamcrest.Matcher
import org.hamcrest.Matchers

inline fun <T, reified E> instanceOf(): Matcher<in T> {
    return Matchers.instanceOf(E::class.java)
}