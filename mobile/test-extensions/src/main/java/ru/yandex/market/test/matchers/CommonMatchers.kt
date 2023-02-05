package ru.yandex.market.test.matchers

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import kotlin.math.abs

fun isCloseTo(value: Float, range: Float): Matcher<Float> {
    return IsCloseTo(value, range)
}

fun <T> hasFeature(expectation: String, predicate: T.() -> Boolean): Matcher<T> {
    return FeatureByPredicateMatcher(expectation, predicate)
}

fun <T, R> hasFeature(getter: T.() -> R, matcher: Matcher<R>, description: String): Matcher<T> {
    return FeatureMatcher(description, matcher, getter)
}

class IsCloseTo(private val value: Float, private val delta: Float) : TypeSafeMatcher<Float>() {

    public override fun matchesSafely(item: Float?): Boolean {
        return item != null && actualDelta(item) <= 0.0
    }

    public override fun describeMismatchSafely(item: Float?, mismatchDescription: Description) {
        mismatchDescription.apply {
            if (item != null) {
                appendValue(item)
                    .appendText(" differed by ")
                    .appendValue(actualDelta(item))
            } else {
                appendText("value is null")
            }
        }
    }

    override fun describeTo(description: Description) {
        description.appendText("a numeric value within ")
            .appendValue(delta)
            .appendText(" of ")
            .appendValue(value)
    }

    private fun actualDelta(item: Float): Float {
        return abs(item - value) - delta
    }
}

private class FeatureMatcher<T, R>(
    private val featureDescription: String,
    private val subMatcher: Matcher<R>,
    private val getter: T.() -> R
) : TypeSafeMatcher<T>() {

    override fun describeTo(description: Description?) {
        description?.apply {
            appendText(featureDescription)
            appendText(" ")
            appendDescriptionOf(subMatcher)
        }
    }

    override fun matchesSafely(item: T): Boolean {
        return subMatcher.matches(getter(item))
    }
}

private class FeatureByPredicateMatcher<T>(
    private val expectationsDescription: String,
    private val predicate: T.() -> Boolean
) : TypeSafeMatcher<T>() {

    override fun describeTo(description: Description?) {
        description?.appendText(expectationsDescription)
    }

    override fun matchesSafely(item: T): Boolean {
        return predicate(item)
    }
}