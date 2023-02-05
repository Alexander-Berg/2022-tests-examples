package ru.yandex.market.test.kakao.util

import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import io.github.kakaocup.kakao.common.assertions.BaseAssertions
import org.hamcrest.Matchers

fun BaseAssertions.isNotVisible() {
    view.check(
        ViewAssertions.matches(
            Matchers.anyOf(
                ViewMatchers.withEffectiveVisibility(
                    ViewMatchers.Visibility.GONE
                ),
                ViewMatchers.withEffectiveVisibility(
                    ViewMatchers.Visibility.INVISIBLE
                )
            )
        )
    )
}

fun BaseAssertions.isVisible(isVisible: Boolean) {
    if (isVisible) isVisible() else isNotVisible()
}

fun BaseAssertions.isEnabled(isEnabled: Boolean) {
    if (isEnabled) {
        isEnabled()
    } else {
        view.check(
            ViewAssertions.matches(
                ViewMatchers.isNotEnabled()
            )
        )
    }
}

fun BaseAssertions.isVisibleAndEnabled(isVisible: Boolean, isEnabled: Boolean) {
    if (isVisible) {
        isVisible()
        isEnabled(isEnabled)
    } else {
        isNotVisible()
    }
}
