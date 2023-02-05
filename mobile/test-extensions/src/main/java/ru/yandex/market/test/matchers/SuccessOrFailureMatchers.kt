package ru.yandex.market.test.matchers

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.Failure
import ru.yandex.market.utils.Success
import ru.yandex.market.utils.SuccessOrFailure

fun <S, F> hasSuccessValue(valueMatcher: Matcher<S>): Matcher<SuccessOrFailure<S, F>> {
    return SuccessValueMatcher(valueMatcher)
}

fun <S, F> hasFailureValue(valueMatcher: Matcher<F>): Matcher<SuccessOrFailure<S, F>> {
    return FailureValueMatcher(valueMatcher)
}

private class SuccessValueMatcher<S, F>(
    private val valueMatcher: Matcher<S>
) : TypeSafeMatcher<SuccessOrFailure<S, F>>() {

    override fun describeMismatchSafely(item: SuccessOrFailure<S, F>?, mismatchDescription: Description?) {
        mismatchDescription?.apply {
            when (item) {
                null -> appendText("item is null")

                is Success -> {
                    appendText("value ")
                    valueMatcher.describeMismatch(item.successValue, mismatchDescription)
                }

                else -> {
                    appendText("item is $item")
                }
            }
        }
    }

    override fun describeTo(description: Description?) {
        description?.apply {
            appendText("Success with value that is a ")
            appendDescriptionOf(valueMatcher)
        }
    }

    override fun matchesSafely(item: SuccessOrFailure<S, F>?): Boolean {
        return item is Success && valueMatcher.matches(item.successValue)
    }
}

private class FailureValueMatcher<S, F>(
    private val valueMatcher: Matcher<F>
) : TypeSafeMatcher<SuccessOrFailure<S, F>>() {

    override fun describeMismatchSafely(item: SuccessOrFailure<S, F>?, mismatchDescription: Description?) {
        mismatchDescription?.apply {
            when (item) {
                null -> appendText("item is null")

                is Failure -> {
                    appendText("value ")
                    valueMatcher.describeMismatch(item.failureValue, mismatchDescription)
                }

                else -> {
                    appendText("item is $item")
                }
            }
        }
    }

    override fun describeTo(description: Description?) {
        description?.apply {
            appendText("Failure value that is a ")
            appendDescriptionOf(valueMatcher)
        }
    }

    override fun matchesSafely(item: SuccessOrFailure<S, F>?): Boolean {
        return item is Failure && valueMatcher.matches(item.failureValue)
    }
}