package ru.yandex.yandexmaps.tools.analyticsgenerator.swift

import org.junit.Test
import ru.yandex.yandexmaps.tools.analyticsgenerator.Language
import ru.yandex.yandexmaps.tools.analyticsgenerator.SwiftCodeGenerator
import ru.yandex.yandexmaps.tools.analyticsgenerator.assertZeroDiff
import ru.yandex.yandexmaps.tools.analyticsgenerator.readResource
import ru.yandex.yandexmaps.tools.analyticsgenerator.testMetricsEvents
import kotlin.test.assertEquals

class SwiftEndToEndTest {

    @Test
    fun `yaml to swift`() {
        val swiftFilePath = "/src/commonMain/kotlin/ru/yandex/yandexmaps/multiplatform/analytics/GeneratedAppAnalytics.swift"

        val result = SwiftCodeGenerator.generate(testMetricsEvents(Language.Swift), swiftFilePath)
        val expected = "GenaMetricsEventTracker+Generated.swift.expected".readResource()

        assertZeroDiff(expected, result)
    }
}
