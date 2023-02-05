package ru.yandex.disk

import android.view.View
import androidx.core.view.updatePadding
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.junit.Test

class ViewExtTest {

    @Test
    fun `should set right padding`() {
        val view = mock<View>()

        view.updatePadding(right = 10)

        verify(view).setPadding(0, 0, 10, 0)
    }
}
