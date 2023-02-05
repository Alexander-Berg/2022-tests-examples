package ru.yandex.market.test.rule

import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR2
import androidx.test.runner.screenshot.Screenshot
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import timber.log.Timber

class ScreenshotRule : TestWatcher() {

    override fun starting(description: Description?) {}

    override fun failed(e: Throwable, description: Description) = try {
        takeScreenshot(description.className)
    } catch (t: Throwable) {
        Timber.e(t)
    }

    private fun takeScreenshot(fileNamePrefix: String) {
        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR2) {
            Screenshot
                .capture()
                .setName(fileNamePrefix)
                .process()
        }
    }
}