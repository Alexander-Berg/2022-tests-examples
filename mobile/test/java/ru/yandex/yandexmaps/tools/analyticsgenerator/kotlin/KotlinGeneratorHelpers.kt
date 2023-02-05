package ru.yandex.yandexmaps.tools.analyticsgenerator.kotlin

import ru.yandex.yandexmaps.tools.analyticsgenerator.ClassNameExtractor
import ru.yandex.yandexmaps.tools.analyticsgenerator.KotlinCodeGenerator
import ru.yandex.yandexmaps.tools.analyticsgenerator.ParsedEvent
import kotlin.test.assertEquals

private const val DEFAULT_FILE = "/src/commonMain/kotlin/ru/yandex/yandexmaps/multiplatform/analytics/GeneratedAppAnalytics.kt"

private fun contentTemplate(filePath: String = DEFAULT_FILE, hasBody: Boolean): String {
    val nameExtractor = ClassNameExtractor(filePath)
    val ending = if (hasBody) " {\n%s\n}" else ""

    return """
package ${nameExtractor.packageName}
import ru.yandex.yandexmaps.multiplatform.analytics.tracker.AnalyticsEventTracker

/**
 *
 *     This class has been generated automatically. Don't modify.
 *
 *     ./gradlew :multiplatform:analytics:gena
 */
class GeneratedAppAnalytics(
    private val eventTracker: AnalyticsEventTracker
)$ending

    """.trimIndent()
}

fun performGeneratorTest(expectedBody: String, events: List<ParsedEvent>, filePath: String = DEFAULT_FILE) {
    val content = KotlinCodeGenerator.generate(events, filePath, "ru.yandex.yandexmaps.multiplatform.analytics.tracker")
    val template = contentTemplate(filePath, hasBody = events.isNotEmpty())
    val expected = template.format(expectedBody)

    assertEquals(expected, content)
}
