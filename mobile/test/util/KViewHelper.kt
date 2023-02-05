package ru.yandex.market.test.util

import android.view.View
import io.github.kakaocup.kakao.common.views.KBaseView

inline fun <reified T : View> KBaseView<*>.withView(crossinline action: (T) -> AssertionResult) {
    view.check { view, notFoundException ->
        if (view is T) {
            when (val result = action.invoke(view)) {
                is AssertionResult.Failure -> throw AssertionError(result.message)
                AssertionResult.Success -> Unit
            }
        } else {
            notFoundException.let { throw AssertionError(it) }
        }
    }
}

inline fun assertThat(value: Boolean, lazyMessage: () -> String): AssertionResult {
    return if (value) {
        AssertionResult.Success
    } else {
        AssertionResult.Failure(lazyMessage.invoke())
    }
}

fun createErrorMessage(element: String, expected: Any? = null, actual: Any? = null): String {
    return when {
        expected != null && actual != null -> {
            "$element value expected to be: $expected, but actual value is: $actual"
        }
        actual != null -> {
            "$element assertion failed actual value is: $actual"
        }
        expected != null -> {
            "$element assertion failed expected value is: $expected"
        }
        else -> "$element assertion failed"
    }
}

sealed class AssertionResult {
    object Success : AssertionResult()
    class Failure(val message: String) : AssertionResult()
}