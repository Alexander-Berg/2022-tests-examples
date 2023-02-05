package ru.yandex.market.test.kakao.actions


import android.view.View
import android.view.ViewParent
import android.widget.FrameLayout
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.util.HumanReadables
import io.github.kakaocup.kakao.common.actions.NestedScrollToAction
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

class NestedScrollTo(private val original: ScrollToAction = ScrollToAction()) : ViewAction by original {

    override fun getConstraints(): Matcher<View> = CoreMatchers.anyOf(
        CoreMatchers.allOf(
            withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
            isDescendantOfA(isAssignableFrom(NestedScrollView::class.java))
        ),
        original.constraints
    )

    override fun perform(uiController: UiController, view: View) {
        try {
            NestedScrollToAction().perform(uiController, view)
        } catch (e: PerformException) {
            fixedPerform(view)
        }
        uiController.loopMainThreadUntilIdle()
    }

    // Лечит:
    // PerformException: Error performing 'scroll to' on view 'Animations or transitions are enabled on the target device.
    // Source:
    // https://stackoverflow.com/questions/39642631/espresso-testing-nestedscrollview-error-performing-scroll-to-on-view-with
    private fun fixedPerform(view: View) {
        try {
            val nestedScrollView = findFirstParentLayoutOfClass(
                view,
                NestedScrollView::class.java
            ) as NestedScrollView?
            nestedScrollView?.scrollTo(0, view.top) ?: throw Exception("Unable to find NestedScrollView parent.")
        } catch (e: Exception) {
            throw PerformException.Builder()
                .withActionDescription(this.description)
                .withViewDescription(HumanReadables.describe(view))
                .withCause(e)
                .build()
        }
    }

    private fun findFirstParentLayoutOfClass(view: View, parentClass: Class<out View>): View {
        var parent: ViewParent = FrameLayout(view.context)
        var incrementView: ViewParent? = null
        var i = 0

        while (parent.javaClass != parentClass) {
            parent = if (i == 0) {
                findParent(view)
            } else {
                findParent(requireNotNull(incrementView))
            }
            incrementView = parent
            i++
        }
        return parent as View
    }

    private fun findParent(view: View): ViewParent {
        return view.parent
    }

    private fun findParent(view: ViewParent): ViewParent {
        return view.parent
    }
}