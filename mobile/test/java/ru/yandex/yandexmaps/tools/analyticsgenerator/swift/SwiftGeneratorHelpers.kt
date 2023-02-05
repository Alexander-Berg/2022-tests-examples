package ru.yandex.yandexmaps.tools.analyticsgenerator.swift

import ru.yandex.yandexmaps.tools.analyticsgenerator.ParsedEvent
import ru.yandex.yandexmaps.tools.analyticsgenerator.SwiftCodeGenerator
import kotlin.test.assertEquals

private const val DEFAULT_FILE = "/src/commonMain/kotlin/ru/yandex/yandexmaps/multiplatform/analytics/GeneratedAppAnalytics.kt"

private fun contentTemplate(): String {

    return """
// This class has been generated automatically. Don't modify.

%s
""".trimStart()
}

fun performSwiftGeneratorTest(expectedBody: String, events: List<ParsedEvent>, filePath: String = DEFAULT_FILE) {
    val content = SwiftCodeGenerator.generate(events, filePath)
    val template = contentTemplate()
    val expected = template.format(expectedBody)

    assertEquals(expected, content)
}
