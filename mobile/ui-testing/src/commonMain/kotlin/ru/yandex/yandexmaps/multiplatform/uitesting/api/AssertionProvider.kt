package ru.yandex.yandexmaps.multiplatform.uitesting.api

public interface AssertionProvider {
    public fun assert(expression: Boolean, message: String = "")
    public fun <T> assertEqual(value: T, expectation: T, message: String = "")
    public fun assertNull(obj: Any?, message: String = "")
    public fun assertNonNull(obj: Any?, message: String = "")
    public fun fail(message: String): Nothing
}
