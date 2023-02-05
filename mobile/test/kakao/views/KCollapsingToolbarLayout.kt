package ru.yandex.market.test.kakao.views

import android.view.View
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import com.google.android.material.appbar.CollapsingToolbarLayout
import org.hamcrest.Matcher

class KCollapsingToolbarLayout : KBaseView<KCollapsingToolbarLayout> {

    constructor(function: ViewBuilder.() -> Unit) : super(function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    constructor(parent: DataInteraction, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun isCollapsed() {
        view.check(
            ViewAssertion { view, noViewFoundException ->
                if (view is CollapsingToolbarLayout) {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    if (location[1] >= 0) throw AssertionError("The CollapsingToolbarLayout location on screen Y is expected to be less than zero but is ${location[1]}")
                } else if (view != null) {
                    throw AssertionError("The view is expected to be CollapsingToolbarLayout but is ${view::class}")
                } else {
                    throw noViewFoundException
                }
            }
        )
    }

    fun isExpanded() {
        view.check(
            ViewAssertion { view, noViewFoundException ->
                if (view is CollapsingToolbarLayout) {
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    if (location[1] <= 0) throw AssertionError("The CollapsingToolbarLayout location on screen Y is expected to be greater than zero but is ${location[1]}")
                } else if (view != null) {
                    throw AssertionError("The view is expected to be CollapsingToolbarLayout but is ${view::class}")
                } else {
                    throw noViewFoundException
                }
            }
        )
    }

    fun hasTitle(title: String) {
        view.check(
            ViewAssertion { view, noViewFoundException ->
                if (view is CollapsingToolbarLayout) {
                    if (!view.isTitleEnabled) throw AssertionError("The CollapsingToolbarLayout title is disabled")
                    if (view.title != title) throw AssertionError("The CollapsingToolbarLayout title is expected to be $title but is ${view.title}")
                } else if (view != null) {
                    throw AssertionError("The view is expected to be CollapsingToolbarLayout but is ${view::class}")
                } else {
                    throw noViewFoundException
                }
            }
        )
    }

}