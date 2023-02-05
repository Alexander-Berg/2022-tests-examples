package ru.yandex.market

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class AndroidFrameworkTests {

    /**
     * Поэтому в разметке compound-вьюх всегда нужнео использовать id с префиксами, иначе можно
     * случайно получить конфликт с id вьюх в Activity и долго ломать голову почему что-то работает
     * не совсем так как надо.
     */
    @Test
    fun `Find view by id traversal view hierarchy using depth search`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generatedId = View.generateViewId()
        val viewGroup = FrameLayout(context)
        val nestedViewGroup = FrameLayout(context)
        val nestedChild = TextView(context).apply { id = generatedId }
        val child = TextView(context).apply { id = generatedId }
        nestedViewGroup.addView(nestedChild)
        viewGroup.addView(nestedViewGroup)
        viewGroup.addView(child)

        val foundChild = viewGroup.findViewById<View>(generatedId)

        assertThat(foundChild).isSameAs(nestedChild as View)
    }
}
