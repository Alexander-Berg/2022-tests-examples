package ru.yandex.yandexmaps.tools.testpalm.impl.formatter

import ru.yandex.yandexmaps.tools.testpalm.impl.TestCase

interface TestCaseFormatter {
    fun printTestCase(testCase: TestCase): String
    fun getHash(str: String): Int?
    fun printHash(hash: Int): String
}

object MapsTestCaseFormatter : TestCaseFormatter {
    private val hashPrefix = "// testcase hash: "
    private val tab = "    "

    private fun printPrepareStep(description: String, nestingLevel: Int): String {
        return printFunctionCall("perform", description, nestingLevel) + "\n${tab.repeat(nestingLevel)}}\n"
    }

    private fun printExpect(description: String, nestingLevel: Int): String {
        return printFunctionCall("assert", description, nestingLevel) + "\n${tab.repeat(nestingLevel)}}\n"
    }

    private fun printStep(step: TestCase.Step, nestingLevel: Int): String {
        var ret = printFunctionCall("perform", step.description, nestingLevel) + "\n"
        if (step.expect != null) {
            ret += printExpect(step.expect, nestingLevel + 1)
        }
        ret += "${tab.repeat(nestingLevel)}}\n"
        return ret
    }

    override fun printTestCase(testCase: TestCase): String {
        var ret = """package ru.yandex.yandexmaps.multiplatform.uitesting.internal.testcases

import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Scope
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.Status
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseDsl
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseBasedTest
import ru.yandex.yandexmaps.multiplatform.uitesting.internal.TestCaseLink

"""
        ret += "// https://testpalm.yandex-team.ru/testcase/${testCase.projectId}-${testCase.id}\n"
        ret += "private val TEST_CASE_LINK = TestCaseLink(\"TEST-CASE-${testCase.id}\", \"https://testpalm.yandex-team.ru/testcase/${testCase.projectId}-${testCase.id}\")\n\n"

        ret += "internal class ${testCase.codeName} : TestCaseBasedTest(\"${testCase.name}\", TEST_CASE_LINK) {\n"
        ret += tab.repeat(1) + "override fun TestCaseDsl.run() {\n\n"

        if (testCase.preconditions != null) {
            ret += printPrepareStep(testCase.preconditions, 2) + "\n"
        }

        ret += testCase.steps.map { printStep(it, 2) }.joinToString("\n")
        ret += tab.repeat(1) + "}\n\n"

        ret += tab.repeat(1) + "override fun status() = Status.UNSTABLE\n\n"
        ret += tab.repeat(1) + "override fun scopes(): List<Scope> = listOf(Scope.NIGHT, Scope.RELEASE, Scope.PR)\n"

        ret += "}\n"
        return ret
    }

    override fun getHash(str: String): Int? =
        if (str.startsWith(hashPrefix)) { str.removePrefix(hashPrefix).toInt(16) } else { null }

    override fun printHash(hash: Int): String = "$hashPrefix${hash.toString(16)}"

    private fun printFunctionCall(functionName: String, description: String, nestingLevel: Int): String {
        var ret = tab.repeat(nestingLevel) + functionName + "(${printDescription(description, nestingLevel + 1)}"
        if (description.hasLinebreak()) {
            ret += "\n${tab.repeat(nestingLevel)}"
        }
        ret += ") {\n"
        return ret
    }

    private fun printDescription(description: String, nestingLevel: Int): String {
        val tabulated = description.split("\n").joinToString("\n${tab.repeat(nestingLevel)}")
        return if (description.hasLinebreak()) {
            "\"\"\"\n${tab.repeat(nestingLevel)}$tabulated\"\"\""
        } else {
            "\"$tabulated\""
        }
    }
}

private fun String.hasLinebreak(): Boolean = contains("\n")
