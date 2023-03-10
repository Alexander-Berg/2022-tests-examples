package io.github.kakaocup.kakao.common.matchers

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.Description
import ru.yandex.market.ui.view.Spinner

/**
 * Matches Spinner with count of children
 *
 * @param size of children count in Spinner
 */
class SpinnerAdapterSizeMatcher(private val size: Int) : BoundedMatcher<View, Spinner>(Spinner::class.java) {

    private var itemCount: Int = 0

    override fun matchesSafely(view: Spinner?) = run {
        itemCount = (view as Spinner).size
        itemCount == size
    }

    override fun describeTo(description: Description) {
        description
            .appendText("Spinner with ")
            .appendValue(size)
            .appendText(" item(s), but got with ")
            .appendValue(itemCount)
    }

}