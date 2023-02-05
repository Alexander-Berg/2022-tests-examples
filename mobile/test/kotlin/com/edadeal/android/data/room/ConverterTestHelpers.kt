package com.edadeal.android.data.room

import okio.ByteString
import okio.ByteString.Companion.decodeHex
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

val s0: ByteString = "fffe00".decodeHex()
val s1: ByteString = "0102fffe".decodeHex()
val s2: ByteString = "74726f6c6f6c6f313131212121".decodeHex()

inline fun <reified T> makeMatcher(
    expected: T,
    crossinline isEquals: (T, T) -> Boolean
) = object : TypeSafeMatcher<T>() {

    override fun describeTo(description: Description?) {
        description?.appendValue(expected)
    }

    override fun matchesSafely(item: T): Boolean {
        return item?.let { isEquals(expected, it) } == true
    }
}
