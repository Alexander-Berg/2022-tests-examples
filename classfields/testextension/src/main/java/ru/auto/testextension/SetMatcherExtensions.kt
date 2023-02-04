package ru.auto.testextension

import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.should

infix fun <T> Collection<T>?.shouldContainMatching(elementMatcher: Matcher<T>) = this should neverNullMatcher { collection ->
    val matchingElement = collection.find { elementMatcher.test(it).passed() }
    MatcherResult(
        passed = matchingElement != null,
        failureMessage = "Collection should have any elements that match ${elementMatcher}, but was ${collection}",
        negatedFailureMessage = "Collection should not contain any matching elements but we found one: ${matchingElement}"
    )
}
