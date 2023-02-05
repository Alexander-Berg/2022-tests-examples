package ru.yandex.market.test.kakao.matchers

import android.view.View
import android.widget.ImageView
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.utils.OPAQUE_INT
import ru.yandex.market.utils.SEMITRANSPARENT

class SmartCoinImageMatcher(
    private val isActive: Boolean
) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description) {
        description.appendText("Smart Coin image " + (if (isActive) "" else "not ") + "active")
    }

    override fun matchesSafely(view: View): Boolean {
        return if (isActive) {
            view is ImageView && view.colorFilter == null && view.imageAlpha == OPAQUE_INT
        } else {
            view is ImageView && view.colorFilter != null && view.alpha == SEMITRANSPARENT
        }
    }
}