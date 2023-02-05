package ru.yandex.market.util

import android.content.Context
import android.os.Build
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.annimon.stream.test.hamcrest.StreamMatcher.assertElements
import com.annimon.stream.test.hamcrest.StreamMatcher.assertIsEmpty
import org.hamcrest.Matchers.contains
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.utils.ViewUtils

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ViewUtilsTest {

    @Test
    fun `Child views stream return all child views`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = FrameLayout(context)
        val one = TextView(context)
        val two = Button(context)
        val three = ImageView(context)
        parent.addView(one)
        parent.addView(two)
        parent.addView(three)

        ViewUtils.getChildViewsStream(parent)
            .custom(assertElements(contains(one, two, three)))
    }

    @Test
    fun `Creates empty child views stream when there is no child views`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = FrameLayout(context)

        ViewUtils.getChildViewsStream(parent)
            .custom(assertIsEmpty())
    }
}