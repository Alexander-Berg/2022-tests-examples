package ru.yandex.market.test.kakao.matchers

import android.view.View
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.uikit.utils.GradientColor
import ru.yandex.market.uikit.view.GradientCircularProgressBar

class CircularGradientProgressMatcher(
    private val progress: Int,
    private val color: GradientColor
) : TypeSafeMatcher<View>() {

    override fun describeTo(desc: Description) {
        desc.appendText("has progress $progress and color $color")
    }

    public override fun matchesSafely(view: View): Boolean {
        return view is GradientCircularProgressBar &&
                view.progress == progress &&
                view.getProgressColor() == color
    }
}
