package com.yandex.mobile.realty.testing

import com.android.ddmlib.testrunner.TestIdentifier
import com.android.ddmlib.testrunner.TestResult.TestStatus.ASSUMPTION_FAILURE
import com.android.ddmlib.testrunner.TestResult.TestStatus.FAILURE
import com.android.ddmlib.testrunner.TestResult.TestStatus.IGNORED
import com.android.ddmlib.testrunner.TestResult.TestStatus.INCOMPLETE
import com.android.ddmlib.testrunner.TestResult.TestStatus.PASSED
import com.android.ddmlib.testrunner.TestRunResult

class TestRunResultAggregator {
    val testRuns = mutableListOf<TestRunResult>()

    fun getLatestFailedTests(): Set<TestIdentifier> = testRuns
        .last()
        .testResults
        .filter { it.value.status == FAILURE || it.value.status == ASSUMPTION_FAILURE }
        .keys
        .toHashSet()

    fun aggregateTestRunResult() = TestRunResult().apply {
        testRunStarted(testRuns.first().name, testRuns.first().numTests)
        for ((index, testRun) in testRuns.withIndex()) {
            for (test in testRun.testResults) {
                this.testStarted(test.key)
                when (test.value.status) {
                    FAILURE -> this.testFailed(test.key, test.value.stackTrace)
                    PASSED -> this.testEnded(test.key, setRetryCount(test.value.metrics, index))
                    ASSUMPTION_FAILURE -> this.testAssumptionFailure(test.key, test.value.stackTrace)
                    IGNORED -> this.testIgnored(test.key)
                    null, INCOMPLETE -> Unit // do nothing
                }
            }
        }
        testRunEnded(
            testRuns.sumOf { it.elapsedTime },
            testRuns.fold(mutableMapOf()) { acc, x -> acc.apply { putAll(x.runMetrics) } }
        )
    }

    private fun setRetryCount(metrics: Map<String, String>, count: Int): Map<String, String> = metrics.toMutableMap().apply {
        putIfAbsent("retryCount", count.toString())
    }
}
