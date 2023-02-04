package ru.auto.lintrules

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFiles.xml
import com.android.tools.lint.checks.infrastructure.TestLintResult
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Issue


fun String.lintAsKotlinFile(vararg issues: Issue): TestLintResult =
    lint().ktFile(this)
        .allowDuplicates()
        .issues(*issues)
        .allowMissingSdk()
        .run()

fun String.lintAsLayoutFile(vararg issues: Issue): TestLintResult =
    lint().xmlFile("res/layout/dummy_layout.xml", this)
        .allowDuplicates()
        .issues(*issues)
        .allowMissingSdk()
        .run()

fun String.lintAsStringFile(vararg issues: Issue): TestLintResult =
    lint().xmlFile("res/values/strings.xml", this)
        .allowDuplicates()
        .issues(*issues)
        .allowMissingSdk()
        .run()

fun TestLintTask.xmlFile(filename: String, content: String): TestLintTask = files(xml(filename, content))
fun TestLintTask.ktFile(content: String): TestLintTask = files(kotlin(content))
