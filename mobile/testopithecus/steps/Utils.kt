package com.yandex.mail.testopithecus.steps

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.DataInteraction
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.core.internal.deps.guava.base.Predicate
import androidx.test.espresso.core.internal.deps.guava.collect.Iterables
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.util.TreeIterables
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.yandex.mail.BuildConfig
import com.yandex.mail.R
import com.yandex.mail.espresso.ViewActions.waitForExistance
import com.yandex.mail.testopithecus.whileMax
import com.yandex.mail.util.UiUtils.isTablet
import io.qameta.allure.kotlin.Allure
import junit.framework.AssertionFailedError
import org.hamcrest.BaseMatcher
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert
import java.util.concurrent.TimeUnit

const val DEFAULT_TIMEOUT: Long = 8000
const val SHORT_TIMEOUT: Long = 1000
const val ACCEPT_DIALOG_BUTTON_ID_STRING = "button1"
const val DECLINE_DIALOG_BUTTON_ID_STRING = "button2"
const val OPTIMAL_SWIPE_SPEED_IN_PIXELS = 150
const val OPTIMAL_SWIPE_SPEED_IN_STEPS = 50

fun UiDevice.find(resourceId: String, order: Int = 0, timeout: Long = DEFAULT_TIMEOUT): UiObject2 {
    val list = findMany(resourceId, timeout)
    if (list.size <= order) {
        throw AssertionError("No element by resourceName '$resourceId' and order $order")
    }
    return list[order]
}

fun UiDevice.findByText(text: String, order: Int = 0, timeout: Long = DEFAULT_TIMEOUT): UiObject2 {
    val list = findManyByText(text, timeout)
    if (list.size <= order) {
        throw AssertionError("No element by text '$text' and order $order")
    }
    return list[order]
}

fun UiDevice.findMany(resourceId: String, timeout: Long = DEFAULT_TIMEOUT): List<UiObject2> {
    val resourceName = "${BuildConfig.APPLICATION_ID}:id/$resourceId"
    return wait(Until.findObjects(By.res(resourceName)), timeout) ?: return listOf()
}

fun UiDevice.findManyByFullResourceName(resourceName: String, order: Int = 0, timeout: Long = DEFAULT_TIMEOUT): UiObject2? {
    val list = wait(Until.findObjects(By.res(resourceName)), timeout) ?: listOf()

    if (list.size <= order) {
        throw AssertionError("No element by resourceName '$resourceName' and order $order")
    }
    return list[order]
}

fun UiDevice.findManyByText(text: String, timeout: Long = DEFAULT_TIMEOUT): List<UiObject2> {
    return wait(Until.findObjects(By.text(text)), timeout) ?: return listOf()
}

fun UiDevice.findManyAndroid(resourceId: String, timeout: Long = DEFAULT_TIMEOUT): List<UiObject2> {
    val resourceName = "android:id/$resourceId"
    return wait(Until.findObjects(By.res(resourceName)), timeout) ?: return listOf()
}

fun UiDevice.isDialogShown(): Boolean {
    return this.findManyAndroid(ACCEPT_DIALOG_BUTTON_ID_STRING, SHORT_TIMEOUT).isNotEmpty()
}

fun UiDevice.acceptDialog() {
    this.findManyAndroid(ACCEPT_DIALOG_BUTTON_ID_STRING, SHORT_TIMEOUT)[0].click()
}

fun UiDevice.declineDialog() {
    this.findManyAndroid(DECLINE_DIALOG_BUTTON_ID_STRING, SHORT_TIMEOUT)[0].click()
}

fun UiObject2.find(resourceId: String, order: Int = 0): UiObject2? {
    val list = findMany(resourceId)
    if (list.size <= order) {
        return null
    }
    return list[order]
}

fun UiObject2.findMany(resourceId: String, timeout: Long = DEFAULT_TIMEOUT): List<UiObject2> {
    val resourceName = "${BuildConfig.APPLICATION_ID}:id/$resourceId"
    return findObjects(By.res(resourceName)) ?: listOf()
}

fun UiDevice.has(resourceId: String, timeout: Long = DEFAULT_TIMEOUT): Boolean {
    return findMany(resourceId, timeout).isNotEmpty()
}

fun UiDevice.gone(resourceId: String, timeout: Long = DEFAULT_TIMEOUT): Boolean {
    val resourceName = "${BuildConfig.APPLICATION_ID}:id/$resourceId"
    return wait(Until.gone(By.res(resourceName)), timeout)
}

fun UiDevice.count(resourceId: String, timeout: Long = DEFAULT_TIMEOUT): Int {
    return findMany(resourceId, timeout).size
}

fun UiDevice.toastShown(): Boolean {
    return (has("snackbar_text") && has("snackbar_action"))
}

fun UiDevice.clickOnRecyclerItemByText(text: String) {
    getRecyclerItemByText(text).click()
}

fun UiDevice.clickOnRecyclerItemByTextInUiObject2(resourceIdUiObject2: String, text: String) {
    scrollToObjectIfNeeded(text)
    find(resourceIdUiObject2).findObject(By.text(text)).click()
}

fun UiDevice.getRecyclerItemByText(text: String): UiObject2 {
    scrollToObjectIfNeeded(text)
    return findByText(text)
}

fun scrollToObjectIfNeeded(text: String) {
    UiScrollable(
        UiSelector().scrollable(true)
    ).scrollIntoView(
        UiSelector().text(text)
    )
}

fun scrollToTop(maxSwipes: Int = 7) {
    UiScrollable(
        UiSelector().scrollable(true)
    ).flingToBeginning(maxSwipes)
}

fun scrollToBottom(maxSwipes: Int = 7) {
    UiScrollable(
        UiSelector().scrollable(true)
    ).flingToEnd(maxSwipes)
}

fun formatEmail(email: String): String {
    return email.replaceFirst(".", "-")
}

fun openDrawer() {
    onView(withId(R.id.drawer_layout))
        .check(matches(isClosed()))
        .perform(DrawerActions.open())
}

fun closeDrawer() {
    onView(withId(R.id.drawer_layout))
        .check(matches(isOpen()))
        .perform(DrawerActions.close())
}

fun shouldSee(
    timeout: Long,
    timeUnit: TimeUnit,
    vararg viewMatcher: Matcher<View?>
) {
    Allure.step("Должны видеть вьюхи $viewMatcher") {
        for (item in viewMatcher) {
            val startTime = SystemClock.elapsedRealtime()
            val endTime = startTime + timeUnit.toMillis(timeout)
            var findAgain: Boolean // go to find other view or not to go
            onView(isRoot()).perform(waitForExistance(item, 5, TimeUnit.SECONDS))
            onView(item).perform(scrollTo())
            do {
                if (onView(item).isMatchesAssertion(matches(ViewMatchers.isDisplayed()))) {
                    findAgain = true
                    break
                } else { // View is not displayed yet
                    findAgain = false
                }
            } while (SystemClock.elapsedRealtime() < endTime)
            if (!findAgain) {
                Assert.fail(String.format("View %s is not displayed!", *viewMatcher))
            }
        }
    }
}

fun shouldNotSee(viewMatcher: Matcher<View?>, timeout: Long = DEFAULT_TIMEOUT, timeUnit: TimeUnit = TimeUnit.MILLISECONDS) {
    Allure.step("Не должны видеть вьюху $viewMatcher") {
        val startTime = SystemClock.elapsedRealtime()
        val endTime = startTime + timeUnit.toMillis(timeout)
        do {
            if (
                !Espresso.onView(viewMatcher).isMatchesAssertion(
                    ViewAssertions.matches(
                        Matchers.allOf(
                            ViewMatchers.isDisplayed(),
                            ViewMatchers.isEnabled()
                        )
                    )
                )
            ) {
                return@step
            }
        } while (SystemClock.elapsedRealtime() < endTime)
        Assert.fail(String.format("View %s is still exists!", viewMatcher))
    }
}

fun clickOn(viewMatcher: Matcher<View?>) {
    Allure.step("Кликаем на вьюху $viewMatcher") {
        waitForExistance(viewMatcher, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
        if (!onView(
                allOf(
                        viewMatcher,
                        ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE),
                        isDescendantOfA(
                                Matchers.anyOf(
                                        isAssignableFrom(ScrollView::class.java),
                                        isAssignableFrom(HorizontalScrollView::class.java),
                                        isAssignableFrom(ListView::class.java)
                                    )
                            )
                    )
            ).isMatchesAssertion(doesNotExist())
        ) {
            onView(viewMatcher).perform(scrollTo())
        }
        onView(viewMatcher).perform(androidx.test.espresso.action.ViewActions.click())
    }
}

fun clickAtActionMenu(id: Int) {
    clickAtActionMenu(getTextFromResources(id))
}

fun clickAtActionMenu(text: String) {
    shouldSee(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS, ViewMatchers.withText(text))
    Thread.sleep(2000)
    clickOn(ViewMatchers.withText(text))
}

fun isInActionMenu(id: Int): Boolean {
    try {
        Espresso.onView(ViewMatchers.withText(id))
    } catch (e: AssertionError) {
        return false
    }
    return true
}

fun getText(matcher: DataInteraction): String {
    var text = String()
    matcher.perform(object : ViewAction {
        override fun getDescription(): String {
            return "Text of the view"
        }

        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(TextView::class.java)
        }

        override fun perform(uiController: UiController?, view: View?) {
            val tv = view as TextView
            text = tv.text.toString()
        }
    })

    return text
}

fun getCountFromListView(listViewId: Int): Int {
    var count = 0

    val matcher = object : TypeSafeMatcher<View?>() {
        override fun describeTo(description: Description?) {
        }

        override fun matchesSafely(item: View?): Boolean {
            count = (item as ListView).getCount()
            return true
        }
    }
    Espresso.onView(ViewMatchers.withId(listViewId)).check(ViewAssertions.matches(matcher))

    val result = count
    count = 0
    return result
}

fun getCountFromRecyclerView(listViewId: Int): Int {

    val recyclerView = onView(withId(listViewId)) as RecyclerView
    val adapter = recyclerView.adapter
    val rowCount = adapter!!.itemCount

    return rowCount
}

fun getElementsCount(viewMatcher: Matcher<View>, countLimit: Int = 50): Int {
    var actualViewsCount = 0
    do {
        try {
            onView(isRoot())
                .check(matches(withElementsNumber(viewMatcher, actualViewsCount)))
            return actualViewsCount
        } catch (ignored: Error) {
        }
        actualViewsCount++
    } while (actualViewsCount < countLimit)
    throw Exception("Counting $viewMatcher was failed. Count limit exceeded")
}

fun withElementsNumber(viewMatcher: Matcher<View>, expectedCount: Int): Matcher<View?>? {
    return object : TypeSafeMatcher<View?>() {
        var actualCount = -1
        override fun describeTo(description: Description) {
            if (actualCount >= 0) {
                description.appendText("With expected number of items: $expectedCount")
                description.appendText("\n With matcher: ")
                viewMatcher.describeTo(description)
                description.appendText("\n But got: $actualCount")
            }
        }

        override fun matchesSafely(root: View?): Boolean {
            actualCount = 0
            val iterable = TreeIterables.breadthFirstViewTraversal(root)
            actualCount = Iterables.filter(iterable, matcherToPredicate(viewMatcher)).count()
            return actualCount == expectedCount
        }
    }
}

private fun matcherToPredicate(matcher: Matcher<View>): Predicate<View?>? {
    return Predicate<View?> { view -> matcher.matches(view) }
}

fun getElementFromMatchAtPosition(matcher: Matcher<View>, position: Int): Matcher<View?>? {
    return object : BaseMatcher<View?>() {
        var counter = 0

        override fun matches(item: Any): Boolean = matcher.matches(item) && counter++ == position

        override fun describeTo(description: Description) {
            description.appendText("Element at hierarchy position $position")
        }
    }
}

fun UiDevice.scrollDrawerToTop() {
    whileMax({ !onView(withId(R.id.account_switcher_gallery)).isMatchesAssertion(matches(ViewMatchers.isDisplayed())) }) {
        swipe((displayWidth * .2).toInt(), (displayHeight * .2).toInt(), (displayWidth * .2).toInt(), displayHeight, 10)
    }
}

fun ViewInteraction.performMany(vararg viewActions: ViewAction): ViewInteraction {
    for (va in viewActions) {
        this.perform(va)
        Thread.sleep(500)
    }
    return this
}

// fun ViewInteraction.waitUntil(assertion: ViewAssertion): ViewInteraction = this.apply {
//    retry(check(assertion))
// }

fun getTextFromResources(@StringRes id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

fun getNeighboringElementByParent(element: UiObject2, resourceId: String): UiObject2 {
    val parent = element.parent
    val listChildren = parent.children

    if (listChildren.size == 1) {
        throw AssertionError("No another children in the parent element '${element.resourceName}'")
    }

    for (child in listChildren) {
        if (child.resourceName == resourceId)
            return child
    }
    throw AssertionError("No children with $resourceId in the parent element '${element.resourceName}'")
}

fun getElementInListByText(listElement: List<UiObject2>, text: String): UiObject2 {
    for (item in listElement) {
        if (item.text == text)
            return item
    }
    throw AssertionError("No element with $text in the list of element")
}

fun ViewInteraction.isMatchesAssertion(assertion: ViewAssertion): Boolean {
    try {
        this.check(assertion)
        return true
    } catch (e: AssertionFailedError) {
        return false
    } catch (e: NoMatchingViewException) {
        return false
    }
}

fun isTablet(): Boolean {
    return isTablet(InstrumentationRegistry.getInstrumentation().targetContext)
}

fun setViewVisibility(value: Boolean): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return isAssignableFrom(View::class.java)
        }

        override fun perform(uiController: UiController, view: View) {
            view.visibility = if (value) View.VISIBLE else View.INVISIBLE
        }

        override fun getDescription(): String {
            return "Show / Hide View"
        }
    }
}

fun nthChildOf(parentMatcher: Matcher<View?>, childPosition: Int): Matcher<View?>? {
    return object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("with $childPosition child view of type parentMatcher")
        }

        override fun matchesSafely(view: View): Boolean {
            if (view.parent !is ViewGroup) {
                return parentMatcher.matches(view.parent)
            }
            val group: ViewGroup = view.parent as ViewGroup
            return parentMatcher.matches(view.parent) && group.getChildAt(childPosition).equals(view)
        }
    }
}
