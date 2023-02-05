package com.yandex.launcher.setup.importlayout

import android.os.Handler
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.mockito.kotlin.*
import org.junit.Test

class OperationTest {

    @Test
    fun cancelTest() {
        val handler = mock<Handler>()
        val action = mock<Runnable>()
        val operation = Operation(handler, action)

        assertThat(operation.isCancelled, equalTo(false))
        operation.cancel()
        assertThat(operation.isCancelled, equalTo(true))
        verify(handler).removeCallbacks(eq(action))
    }
}
