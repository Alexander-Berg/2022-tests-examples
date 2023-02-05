package com.yandex.mail.metrica

import android.content.Context
import android.content.res.Resources
import android.view.View
import com.yandex.mail.metrica.MockYandexMailMetricaModule.TestYandexMailMetrica
import com.yandex.mail.runners.IntegrationTestRunner
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(IntegrationTestRunner::class)
class LogInfoExtractorTest {

    lateinit var metrica: TestYandexMailMetrica

    lateinit var context: Context

    @Before
    fun beforeEachTest() {
        val app = IntegrationTestRunner.app()
        context = app
        metrica = app.getApplicationComponent().metrica() as TestYandexMailMetrica
    }

    @Test
    fun `ViewInfoExtractor should crash if no id found in debug`() {
        val extractor = ViewInfoExtractor()

        val expectedMessage = "Unable to retrieve resource name by resId: -1"

        val view = View(context)
        assertThatThrownBy { extractor.extractInfo(view) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining(expectedMessage)
            .hasCauseInstanceOf(Resources.NotFoundException::class.java)

        metrica.assertLastEvent(expectedMessage)
    }

    @Test
    fun `ResIdTagExtractor should crash if not found in debug`() {
        val extractor = ResIdTagExtractor(0)
        val view = View(context)
        assertThatThrownBy { extractor.extractInfo(view) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("Unable to retrieve resource name by resId: 0")
            .hasCauseInstanceOf(Resources.NotFoundException::class.java)
    }

    @Test
    fun `getResourceName should crash if required but not found in debug`() {
        val expectedMessage = "Unable to retrieve resource name by resId: 0"

        assertThatThrownBy { getResourceName(context, 0, idRequired = true) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining(expectedMessage)
            .hasCauseInstanceOf(Resources.NotFoundException::class.java)

        metrica.assertLastEvent(expectedMessage)
    }
}
