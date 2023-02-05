package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.FrameLayout
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.beru.android.R
import ru.yandex.market.uikit.button.ProgressButton

class ProgressButtonMatcher(private val text: CharSequence) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("ProgressButton checking text")
    }

    override fun matchesSafely(progressButtonFrameLayout: View): Boolean {
        return if (progressButtonFrameLayout is FrameLayout) {
            val confirmButtonView = progressButtonFrameLayout.findViewById<ProgressButton>(R.id.confirmButton)
            confirmButtonView?.buttonText?.contentEquals(text) ?: false
        } else {
            false
        }
    }

}