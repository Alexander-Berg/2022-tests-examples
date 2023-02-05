package ru.yandex.market.test.kakao.matchers

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import java.util.regex.Pattern

class StringRegexMatcher(private val pattern: String) : BaseMatcher<String>() {
    override fun describeTo(description: Description?) {
        description?.appendText("matches with pattern: ")?.appendText(pattern)
    }

    override fun matches(item: Any?): Boolean {
        return (item as? CharSequence)?.let { Pattern.matches(pattern, it) } == true
    }
}