package com.yandex.mail.testopithecus.steps

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.steps.ActionList
import org.hamcrest.core.AllOf.allOf

class ShortSwipeBaseActionsImpl(private val device: UiDevice) {

    val OPTIMAL_PERCENT_FOR_SHORT_SWIPE = 0.7f
    val OPTIMAL_PERCENT_FOR_SHORT_SWIPE_LANDSCAPE = 0.7f

    fun shortSwipeMenuAction(order: Int, action: ActionList) {
        shortSwipeLeft(order)
        device.find("swipe_action_menu").click()
        device.find("message_action_text_view")
        onView(allOf(withId(R.id.message_action_text_view), withText(action.actionName))).perform(click())
    }

    fun openShortSwipeMenu(order: Int) {
        shortSwipeLeft(order)
        device.find("swipe_action_menu").click()
        device.find("message_action_text_view")
    }

    fun performShortSwipeMenuAction(action: ActionList) {
        onView(allOf(withId(R.id.message_action_text_view), withText(action.actionName))).perform(click())
    }

    fun shortSwipeLeft(order: Int) {
        device.find("sender", order).swipe(Direction.LEFT, OPTIMAL_PERCENT_FOR_SHORT_SWIPE, OPTIMAL_SWIPE_SPEED_IN_PIXELS)
    }

    fun shortSwipeLeftLandscape(order: Int) {
        device.find("sender", order).swipe(Direction.LEFT, OPTIMAL_PERCENT_FOR_SHORT_SWIPE_LANDSCAPE, OPTIMAL_SWIPE_SPEED_IN_PIXELS)
    }

    fun shortSwipeRight(order: Int) {
        device.find("sender", order).swipe(Direction.RIGHT, OPTIMAL_PERCENT_FOR_SHORT_SWIPE, OPTIMAL_SWIPE_SPEED_IN_PIXELS)
    }
}
