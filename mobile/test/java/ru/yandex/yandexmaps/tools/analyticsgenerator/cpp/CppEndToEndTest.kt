package ru.yandex.yandexmaps.tools.analyticsgenerator.cpp

import ru.yandex.yandexmaps.tools.analyticsgenerator.Language
import ru.yandex.yandexmaps.tools.analyticsgenerator.readResource
import ru.yandex.yandexmaps.tools.analyticsgenerator.resourcesIn
import ru.yandex.yandexmaps.tools.analyticsgenerator.rootCppNamespace
import ru.yandex.yandexmaps.tools.analyticsgenerator.testMetricsEvents
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class CppEndToEndTest {

    @Test
    fun `yaml to cpp`() {
        val expectedResult = CppCodeGenerator.Result(
            cppFile = "cpp/gena_report.cpp".readResource(),
            headerFiles = "cpp/headers/yandex/maps/navikit/report/gena".resourcesIn()
                .map { it.fileName.toString() to Files.readString(it) }
                .toMap(),
        )

        val result = CppCodeGenerator.generate(testMetricsEvents(Language.Cpp), rootCppNamespace)

        assertEquals(expected = expectedResult, actual = result)
    }
}
