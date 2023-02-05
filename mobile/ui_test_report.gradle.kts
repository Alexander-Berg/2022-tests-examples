import uitestreport.*

abstract class UiTestCoverageTask : DefaultTask() {

    @ExperimentalStdlibApi
    @TaskAction
    fun report() {
        val automatedTestCases = UiTestCaseGrabber().grab(project)
        val reportHtml = UiTestCoverageReportBuilder().build(automatedTestCases)
        val reportXmlFile = File("ui-test-coverage-report.html")
        reportXmlFile.createNewFile()
        reportXmlFile.writeText(reportHtml)
    }
}

abstract class UiTestSummaryTask : DefaultTask() {

    @ExperimentalStdlibApi
    @TaskAction
    fun report() {
        val automatedTestCases = UiTestCaseGrabber().grab(project)
        val reportJson = UiTestSummaryReportBuilder().build(automatedTestCases)
        val reportJsonFile = File("ui-test-summary-report.json")
        reportJsonFile.createNewFile()
        reportJsonFile.writeText(reportJson)
    }
}

tasks.register<UiTestCoverageTask>("uiTestCoverageTask")
tasks.register<UiTestSummaryTask>("uiTestSummaryTask")