package com.yandex.mobile.realty.testing

import com.yandex.mobile.realty.testing.reporter.ReportType
import com.yandex.mobile.realty.testing.reporter.TestReport
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import java.io.File
import javax.inject.Inject

/**
 * @author rogovalex on 2019-10-30.
 */
open class GradleReportTask @Inject constructor(
    private val outputDir: File
) : DefaultTask() {

    @TaskAction
    fun run() {
        val xmlResultsDir = File(outputDir, "xml")
        val reportDir = File(outputDir, "html")
        reportDir.mkdirs()
        val report = TestReport(ReportType.SINGLE_FLAVOR, xmlResultsDir, reportDir)
        report.generateReport()

        val failuresRegex = "failures=\"(\\d+)\"".toRegex()
        var failures = 0
        val messageRegex = "value=\"([^\"]*)\"".toRegex()
        val messages = mutableListOf<String>()
        xmlResultsDir.listFiles { file ->
            file.name.startsWith("TEST-") && file.name.endsWith(".xml")
        }?.forEach { file ->
            val testsuiteLine = file.useLines { sequence ->
                sequence.first { it.startsWith("<testsuite") }
            }
            failuresRegex.find(testsuiteLine)?.let { matcher ->
                failures += matcher.groupValues[1].toInt()
            }
            val propertyLine = file.useLines { sequence ->
                sequence.firstOrNull { it.startsWith("<property name=\"testRunFailed\"") }
            }
            if (propertyLine != null) {
                messages += messageRegex.find(propertyLine)?.let { matcher ->
                    matcher.groupValues[1]
                } ?: propertyLine
            }
        }

        if (failures > 0 || messages.isNotEmpty()) {
            val errorMessage = buildString {
                if (failures > 0) {
                    append("Tests execution failed: $failures failure(s). ")
                }
                if (messages.isNotEmpty()) {
                    append("Tests run(s) failed: $messages.")
                }
            }
            state.addFailure(TaskExecutionException(this, RuntimeException(errorMessage)))
        }
    }
}
