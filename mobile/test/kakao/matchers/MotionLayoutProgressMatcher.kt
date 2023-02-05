package ru.yandex.market.test.kakao.matchers

import androidx.constraintlayout.motion.widget.MotionLayout
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class MotionLayoutProgressMatcher(
    private val progress: Float
) : TypeSafeMatcher<MotionLayout>() {

    override fun describeTo(description: Description) {
        description.appendText("Motion layout progress $progress")
    }

    override fun matchesSafely(motionLayout: MotionLayout): Boolean {
        return motionLayout.progress == progress
    }
}