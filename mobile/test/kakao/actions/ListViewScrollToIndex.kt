package ru.yandex.market.test.kakao.actions

import android.view.View
import android.widget.ListView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers

class ListViewScrollToIndex(private val index: Int) : ViewAction {
    override fun getDescription() = "Scroll ListView to end"

    override fun getConstraints() = CoreMatchers.allOf(
        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
        ViewMatchers.isAssignableFrom(ListView::class.java)
    )

    override fun perform(uiController: UiController?, view: View?) {
        if (view is ListView) {
            view.setSelection(index)
        }
    }
}