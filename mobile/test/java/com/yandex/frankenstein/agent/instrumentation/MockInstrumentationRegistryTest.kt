package com.yandex.frankenstein.agent.instrumentation

import android.app.Application
import android.content.Context
import androidx.test.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class MockInstrumentationRegistryTest {

    private val application = mock(Application::class.java)
    private val applicationContext = mock(Context::class.java)

    init {
        `when`(application.applicationContext).thenReturn(applicationContext)
    }

    @Test
    fun testRegisterMockInstrumentation() {
        registerMockInstrumentation(application)

        assertThat(InstrumentationRegistry.getTargetContext()).isEqualTo(applicationContext)
    }
}
