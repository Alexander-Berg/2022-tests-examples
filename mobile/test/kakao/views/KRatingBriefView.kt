package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import ru.yandex.market.uikit.raiting.RatingBriefView
import ru.yandex.market.test.kakao.matchers.RatingVisibleMatcher

class KRatingBriefView : KBaseView<KTextView> {
    constructor(function: ViewBuilder.() -> Unit) : super(function)
    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun checkHighlightedStarsCount(highlightedStarsCount: Float) {
        view.check(ViewAssertion { view, notFoundException ->
            if (view is RatingBriefView) {
                if (view.highlightedCount != highlightedStarsCount) {
                    throw AssertionError("Wrong number of stars, expected $highlightedStarsCount got ${view.highlightedCount}")
                }
            } else {
                notFoundException.let {
                    throw AssertionError(it)
                }
            }
        })
    }

    fun checkText(text: String) {
        view.check(ViewAssertion { view, noViewFoundException ->
            if (view is RatingBriefView) {
                if (view.text.toString() != text) {
                    throw AssertionError("Wrong text, expected $text got ${view.text}")
                }
            } else {
                noViewFoundException.let {
                    throw AssertionError(it)
                }
            }
        })
    }

    fun checkText(gradeValue: Float) {
        view.check(ViewAssertion { view, noViewFoundException ->
            if (view is RatingBriefView) {
                val gradeText = formatGradeValue(gradeValue)
                if (view.text.toString() != gradeText) {
                    throw AssertionError("Wrong text, expected $gradeText got ${view.text}")
                }
            } else {
                noViewFoundException.let {
                    throw AssertionError(it)
                }
            }
        })
    }

    fun containsText(text: String) {
        view.check(
            ViewAssertions.matches(
                ViewMatchers.withText(Matchers.containsString(text))
            )
        )
    }

    fun checkReviewsCount(reviewsCount: Int) {
        this.matches { withMatcher(RatingVisibleMatcher(reviewsCount)) }
    }

    private fun formatGradeValue(grade: Float): String {
        return when {
            grade <= 1 -> "Ужасный товар"
            grade <= 2 -> "Плохой товар"
            grade <= 3 -> "Обычный товар"
            grade <= 4 -> "Хороший товар"
            else -> "Отличный товар"
        }
    }
}