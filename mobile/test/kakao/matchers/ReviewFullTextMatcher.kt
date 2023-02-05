package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.TextView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class ReviewFullTextMatcher(
    private val pros: String = "",
    private val cons: String = "",
    private val reviewText: String = ""
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText("(matches review with pros:[$pros] cons:[$cons] text:[$reviewText]")
    }

    override fun matchesSafely(item: View?): Boolean {
        return item is TextView && getFullCommentaryText().fold(true) { acc, comp ->
            acc && (item.text?.contains(comp) ?: false)
        }
    }

    private fun getFullCommentaryText(): List<String> {
        val commentaryComponents = mutableListOf<String>()
        listOf(pros, cons, reviewText)
            .zip(listOf("Достоинства:", "Недостатки:", "Комментарий:"))
            .forEach { (value, header) ->
                if (value.isNotBlank()) {
                    commentaryComponents.add("$header\n${value.capitalize()}")
                }
            }

        return commentaryComponents
    }
}