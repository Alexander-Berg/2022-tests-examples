package ru.yandex.yandexmaps.tools.analyticsgenerator.kotlin

import org.junit.Test
import ru.yandex.yandexmaps.tools.analyticsgenerator.KotlinCodeGenerator
import ru.yandex.yandexmaps.tools.analyticsgenerator.Language
import ru.yandex.yandexmaps.tools.analyticsgenerator.assertZeroDiff
import ru.yandex.yandexmaps.tools.analyticsgenerator.readResource
import ru.yandex.yandexmaps.tools.analyticsgenerator.testMetricsEvents

class KotlinEndToEndTest {

    @Test
    fun `yaml to kt`() {
        val file = "ru/yandex/yandexmaps/multiplatform/analytics/GeneratedAppAnalytics.kt"
        val kotlinFilePath = "/src/commonMain/kotlin/$file"

        val result = KotlinCodeGenerator.generate(testMetricsEvents(Language.Kotlin), kotlinFilePath, "ru.yandex.yandexmaps.multiplatform.analytics.tracker")
        val expected = "$file.expected".readResource()

        assertZeroDiff(expected, result)
    }
}
