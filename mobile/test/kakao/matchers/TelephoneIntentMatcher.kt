package ru.yandex.market.test.kakao.matchers

import android.content.Intent
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class TelephoneIntentMatcher(private val phone: String) : TypeSafeMatcher<Intent>() {

    override fun describeTo(description: Description) {
        description.appendText("Share intent contains link : \"$phone\"")
    }

    override fun matchesSafely(intent: Intent): Boolean {
        return intent.data.toString() == phone
                && intent.action == Intent.ACTION_DIAL
                && intent.flags and (Intent.FLAG_ACTIVITY_NEW_TASK shl 1) - 1 == Intent.FLAG_ACTIVITY_NEW_TASK
    }
}