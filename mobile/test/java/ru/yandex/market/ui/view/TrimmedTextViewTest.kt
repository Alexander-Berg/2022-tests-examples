package ru.yandex.market.ui.view

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.uikit.text.TrimmedTextView

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class TrimmedTextViewTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val view = TrimmedTextView(context)

    @Test
    fun `Saving and restoring state works as expected`() {
        val text = "Hello World!" as CharSequence
        view.text = text
        view.expand()

        val savedState = view.onSaveInstanceState()
        view.onRestoreInstanceState(savedState)

        assertThat(view.fullText).isEqualTo(text)
        assertThat(view.isExpanded).isEqualTo(true)
    }
}