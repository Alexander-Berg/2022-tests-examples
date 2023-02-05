package ru.yandex.market.test.kakao.actions

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import com.google.android.material.appbar.AppBarLayout
import org.hamcrest.Matcher

class ExpandAppBarLayout : ViewAction {

    override fun getDescription() = "Expand AppBarLayout"

    override fun getConstraints(): Matcher<View> = isAssignableFrom(AppBarLayout::class.java)

    override fun perform(uiController: UiController?, view: View?) {
        val appBarLayout = view as? AppBarLayout
        appBarLayout?.setExpanded(true)
        uiController?.loopMainThreadUntilIdle()
    }
}