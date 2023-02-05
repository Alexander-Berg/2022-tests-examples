package com.yandex.mail.metrica

import android.content.Context
import android.view.View
import com.yandex.mail.metrica.MockYandexMailMetricaModule.TestYandexMailMetrica
import com.yandex.mail.runners.IntegrationTestRunner
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
    fun `ViewInfoExtractor should not crash if no id found in release`() {
        val extractor = ViewInfoExtractor()
        extractor.extractInfo(View(context))
        metrica.assertLastEvent("Unable to retrieve resource name by resId: -1")
    }

    @Test
    fun `ResIdTagExtractor should not crash if not found in release`() {
        val extractor = ResIdTagExtractor(0)
        extractor.extractInfo(View(context))
    }

    @Test
    fun `getResourceName should not crash if required but not found in release`() {
        getResourceName(context, 0, idRequired = true)
        metrica.assertLastEvent("Unable to retrieve resource name by resId: 0")
    }
}
