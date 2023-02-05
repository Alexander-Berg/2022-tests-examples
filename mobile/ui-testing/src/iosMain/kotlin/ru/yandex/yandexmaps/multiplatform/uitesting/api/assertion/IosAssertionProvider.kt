package ru.yandex.yandexmaps.multiplatform.uitesting.api.assertion

import platform.XCTest.xctAssert
import platform.XCTest.xctFail
import ru.yandex.yandexmaps.multiplatform.uitesting.api.AssertionProvider

public class IosAssertionProvider() : AssertionProvider {

    public override fun assert(expression: Boolean, message: String) {
        xctAssert(expression, "[Assert] message: '$message'")
    }

    public override fun <T> assertEqual(value: T, expectation: T, message: String) {
        xctAssert(value == expectation, "[AssertEqual] actual: '$value' expected: '$expectation' message: '$message'")
    }

    public override fun assertNull(obj: Any?, message: String) {
        xctAssert(obj == null, "[AssertNull] message: '$message'")
    }

    public override fun assertNonNull(obj: Any?, message: String) {
        xctAssert(obj != null, "[AssertNonNull] message: '$message'")
    }

    override fun fail(message: String): Nothing {
        xctFail("[Fail] message: '$message'")
        error("This code is unreachable")
    }
}
