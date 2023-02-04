package com.yandex.mobile.realty.testing

import com.yandex.mobile.realty.testing.reporter.ReportType
import com.yandex.mobile.realty.testing.reporter.TestReport
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.io.File
import javax.inject.Inject

/**
 * @author rogovalex on 2019-10-30.
 */
open class GradleReportTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val outputDir: File,
    private val xmlResultsDir: File
) : DefaultTask() {

    @TaskAction
    fun run() {
        val workQueue = workerExecutor.noIsolation()
        workQueue.await()

        val reportDir = File(outputDir, "html")
        reportDir.mkdirs()
        val report = TestReport(ReportType.SINGLE_FLAVOR, xmlResultsDir, reportDir)
        report.generateReport()
    }
}
