package com.yandex.mail.testopithecus.feature.impl

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.R
import com.yandex.mail.testopithecus.steps.has
import com.yandex.mail.ui.adapters.StoriesAdapter
import com.yandex.xplat.testopithecus.StoriesBlock
import org.hamcrest.Matcher

class StoriesBlockImpl(private val device: UiDevice) : StoriesBlock {
    override fun hideStories() {
        onView(withId(R.id.stories_list))
            .perform(RecyclerViewActions.scrollToHolder<StoriesAdapter.ViewHolder>(withTitle("Скрыть марки")))

        onView(withText("Скрыть марки")).perform(click())
    }

    override fun openStory(position: Int) {
        onView(withId(R.id.stories_list))
            .perform(RecyclerViewActions.scrollToPosition<StoriesAdapter.ViewHolder>(position), click())
    }

    override fun isHidden(): Boolean {
        return !device.has("stories_list")
    }

    private fun withTitle(title: String): Matcher<StoriesAdapter.ViewHolder?> {
        return object : BoundedMatcher<StoriesAdapter.ViewHolder?, StoriesAdapter.ViewHolder>(StoriesAdapter.ViewHolder::class.java) {

            override fun matchesSafely(item: StoriesAdapter.ViewHolder): Boolean {
                return item.title.text.toString().equals(title, true)
            }

            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("view holder with title: $title")
            }
        }
    }
}
