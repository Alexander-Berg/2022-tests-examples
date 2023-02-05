package ru.yandex.market.test.kakao.matchers

import android.content.Intent
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

class ShareIntentMatcher(private val shareLink: String) : TypeSafeMatcher<Intent>() {

    override fun describeTo(description: Description) {
        description.appendText("Share intent contains link : \"$shareLink\"")
    }

    override fun matchesSafely(intent: Intent): Boolean {
        val chooserIntent = intent.extras?.get(Intent.EXTRA_INTENT)
        return intent.action == Intent.ACTION_CHOOSER
                && chooserIntent is Intent
                && chooserIntent.action == Intent.ACTION_SEND
                && chooserIntent.extras?.get(Intent.EXTRA_TEXT) == shareLink
    }
}