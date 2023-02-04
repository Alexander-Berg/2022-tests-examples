package com.yandex.mobile.realty.testing

import com.android.builder.testing.api.DeviceConnector
import com.android.ddmlib.testrunner.TestIdentifier
import com.android.ddmlib.testrunner.TestRunResult
import io.qameta.allure.kotlin.AllureLifecycle
import io.qameta.allure.kotlin.model.Status
import io.qameta.allure.kotlin.model.StatusDetails
import io.qameta.allure.kotlin.model.TestResult
import io.qameta.allure.kotlin.util.ResultsUtils
import kotlin.collections.HashMap
import kotlin.collections.HashSet

typealias ErrorsFilter = (errorMessage: String) -> Boolean

@Suppress("UnstableApiUsage")
class ResultTestRunListener(
    device: DeviceConnector,
    private val index: Int,
    private val errorsFilter: ErrorsFilter,
    private val lifecycle: AllureLifecycle
) : TestRunResult() {

    private val serialNumber: String = device.serialNumber
    private val testsToRetry = HashSet<TestIdentifier>()

    override fun testStarted(test: TestIdentifier) {
        println("$serialNumber $test started")
        super.testStarted(test)
    }

    override fun testAssumptionFailure(test: TestIdentifier, trace: String) {
        println("$serialNumber $test assumed failure with trace =$trace")
        super.testAssumptionFailure(test, trace)
    }

    override fun testIgnored(test: TestIdentifier) {
        println("$serialNumber $test ignored")
        super.testIgnored(test)
    }

    override fun testEnded(test: TestIdentifier, testMetrics: MutableMap<String, String>) {
        println("$serialNumber $test ended")
        super.testEnded(test, testMetrics)
    }

    override fun testFailed(test: TestIdentifier, trace: String) {
        println("$serialNumber $test failed with trace= $trace")
        super.testFailed(test, trace)
        testsToRetry.add(test)
    }

    override fun testRunFailed(errorMessage: String) {
        println("$serialNumber TestRun failed with errorMessage= $errorMessage")
        super.testRunFailed(errorMessage)

        val trace = buildString {
            appendLine(errorMessage)
            testResults.entries.forEach {
                appendLine("${it.key.className}.${it.key.testName} has status ${it.value.status.name}")
            }
        }

        if (errorsFilter(errorMessage)) {
            val testIdentifier = TestIdentifier("${serialNumber}_$index", "TestRun")
            val uuid = testIdentifier.toString()
            val result = createTestResult(uuid, testIdentifier)
            lifecycle.scheduleTestCase(result)
            lifecycle.startTestCase(uuid)
            lifecycle.updateTestCase(uuid) { testResult: TestResult ->
                testResult.status = Status.BROKEN
                testResult.statusDetails = getStatusDetails(Throwable(trace))
            }
            lifecycle.stopTestCase(uuid)
            lifecycle.writeTestCase(uuid)

            super.testStarted(testIdentifier)
            super.testFailed(testIdentifier, trace)
            super.testEnded(testIdentifier, HashMap())
        }
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: MutableMap<String, String>) {
        super.testRunEnded(elapsedTime, runMetrics)
        if (numTests == 0) {
            testRunFailed("Test run is empty, tests not found")
        }

        if (testsToRetry.isNotEmpty()) {
            println("$serialNumber failed tests = ${testsToRetry.joinToString { it.toString() }}")
        }
    }

    private fun getHistoryId(test: TestIdentifier): String = ResultsUtils.md5(test.className + test.testName)

    private fun getPackage(test: TestIdentifier): String = test.className.replaceAfterLast(".", "")

    private fun createTestResult(uuid: String, test: TestIdentifier): TestResult {
        val className: String = test.className
        val methodName: String? = test.testName
        val name = methodName ?: className
        val fullName = if (methodName != null) "$className.$methodName" else className
        val suite: String = className
        val testResult: TestResult = TestResult(uuid).apply {
            this.historyId = getHistoryId(test)
            this.fullName = fullName
            this.name = name
        }
        testResult.labels.addAll(ResultsUtils.getProvidedLabels())
        testResult.labels.addAll(
            listOf(
                ResultsUtils.createPackageLabel(getPackage(test)),
                ResultsUtils.createTagLabel("${serialNumber}_${index}"),
                ResultsUtils.createTestClassLabel(className),
                ResultsUtils.createTestMethodLabel(name),
                ResultsUtils.createSuiteLabel(suite),
                ResultsUtils.createHostLabel(),
                ResultsUtils.createThreadLabel(),
                ResultsUtils.createFrameworkLabel("junit4"),
                ResultsUtils.createLanguageLabel("kotlin")
            )
        )
        return testResult
    }

    private fun getStatusDetails(throwable: Throwable?): StatusDetails? = throwable?.let {
        StatusDetails(
            message = it.message ?: it.javaClass.name,
            trace = it.stackTraceToString()
        )
    }


    companion object {
        private const val TEST_RUN_FAILED_MESSAGE = "Test run failed to complete"
        val filterFailedRunErrorsOnly : ErrorsFilter = { errorMessage -> !errorMessage.contains(TEST_RUN_FAILED_MESSAGE) }
        val filterFailedRunAndTimeout : ErrorsFilter = { errorMessage ->
            !errorMessage.contains(TEST_RUN_FAILED_MESSAGE) &&
                !errorMessage.contains("Failed to receive adb shell test output within")
        }
    }
}
