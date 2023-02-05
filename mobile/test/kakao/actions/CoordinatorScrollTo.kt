package ru.yandex.market.test.kakao.actions

import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

class CoordinatorScrollTo(private val original: ScrollToAction = ScrollToAction()) : ViewAction by original {

    override fun getConstraints(): Matcher<View> = CoreMatchers.anyOf(
        CoreMatchers.allOf(
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            isDescendantOfA(isAssignableFrom(CoordinatorLayout::class.java))
        ),
        original.constraints
    )
}