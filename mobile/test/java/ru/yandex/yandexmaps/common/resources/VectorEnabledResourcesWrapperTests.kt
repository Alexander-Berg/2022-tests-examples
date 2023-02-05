package ru.yandex.yandexmaps.common.resources

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.InflateException
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.yandexmaps.common.R

class TestActivity : Activity() {

    companion object {
        const val ROOT_ID = 42
    }

    private val resourcesWrapper: VectorEnabledResourcesWrapper by lazy {
        VectorEnabledResourcesWrapper(this, super.getResources())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        root.id = ROOT_ID
        setContentView(root)
    }

    override fun getResources(): Resources {
        return resourcesWrapper.get()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.LOLLIPOP])
class VectorEnabledResourcesWrapperTests {
    @get:Rule
    val testActivityRule = ActivityScenarioRule(TestActivity::class.java)

    @Test(expected = InflateException::class)
    fun `On 21 inflating ProgressBar is expected to fail`() {
        testActivityRule.scenario.onActivity { activity ->
            activity.findViewById<FrameLayout>(TestActivity.ROOT_ID).apply {
                addView(LayoutInflater.from(activity).inflate(R.layout.test_progress_bar, this, false))
            }
        }
    }

    @Test
    fun `Substitute ProgressBar_indeterminateDrawable with null should fix ProgressBar inflation on 21`() {
        testActivityRule.scenario.onActivity { activity ->
            activity.setTheme(R.style.Test_ActivityTheme)
            activity.findViewById<FrameLayout>(TestActivity.ROOT_ID).apply {
                addView(LayoutInflater.from(activity).inflate(R.layout.test_progress_bar, this, false))
            }
        }
    }
}
