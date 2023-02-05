package ru.yandex.market.test.kakao.util

import android.view.View
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import org.hamcrest.Matcher
import ru.yandex.market.test.kakao.matchers.PositionMatcher
import ru.yandex.market.test.kakao.matchers.ScrollPositionAtEndMatcher
import ru.yandex.market.test.kakao.matchers.ScrollPositionAtStartMatcher

fun ViewBuilder.withPosition(position: Int) {
    withMatcher(PositionMatcher(position))
}

fun ViewBuilder.scrolledToStart() {
    withMatcher(ScrollPositionAtStartMatcher())
}

fun ViewBuilder.scrolledToEnd() {
    withMatcher(ScrollPositionAtEndMatcher())
}

fun (ViewBuilder.() -> Unit).asViewMatcher(): Matcher<View> = ViewBuilder().apply(this).getViewMatcher()

fun (ViewBuilder.() -> Unit).concatWith(function: ViewBuilder.() -> Unit): ViewBuilder.() -> Unit {
    return {
        invoke(this)
        function()
    }
}
