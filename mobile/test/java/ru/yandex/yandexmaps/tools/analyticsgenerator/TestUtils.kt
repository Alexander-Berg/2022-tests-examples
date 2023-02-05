package ru.yandex.yandexmaps.tools.analyticsgenerator

import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.difflib.patch.Patch
import com.github.difflib.text.DiffRowGenerator
import ru.yandex.yandexmaps.tools.analyticsgenerator.cpp.CppEndToEndTest
import ru.yandex.yandexmaps.tools.analyticsgenerator.yaml.YamlParser
import ru.yandex.yandexmaps.tools.analyticsgenerator.yaml.toParsedEvents
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.toList

private val classLoader = CppEndToEndTest::class.java.classLoader

fun String.resourcesIn(): List<Path> {
    return Files.walk(Path.of(resourcePath()), 1).toList().filter { Files.isRegularFile(it) }
}

fun String.readResource(): String {
    return Files.readString(Path.of(resourcePath()))
}

fun String.resourcePath(): String {
    return classLoader.getResource(this)!!.path
}

fun testMetricsEvents(language: Language): List<ParsedEvent> {
    return YamlParser.parse("testmetrics.yaml".readResource()).toParsedEvents().filterLanguage(language)
}

const val rootCppNamespace = "yandex::maps::navikit::report::gena"

fun assertZeroDiff(expected: String, actual: String) {

    val patch = DiffUtils.diff(expected, actual, null)

    assert(patch.deltas.isEmpty()) {
        buildString {
            appendLine("Expecting zero diff but was:")

            UnifiedDiffUtils.generateUnifiedDiff("expected", "actual", expected.split("\n"), patch, 3).forEach(::appendLine)
        }
    }
}
fun parameters(params: List<Parameter>): Parameters.ParameterList = Parameters.ParameterList(params)
fun parameters(vararg params: Parameter): Parameters.ParameterList = parameters(params.asList())
