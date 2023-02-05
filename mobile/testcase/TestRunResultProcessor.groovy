package com.yandex.frankenstein.results.testcase

import com.yandex.frankenstein.results.TestResult
import com.yandex.frankenstein.results.TestStatus
import com.yandex.frankenstein.results.TestRunResult
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.description.ignored.IgnoredTestDescription

@CompileStatic
class TestRunResultProcessor {

    private final TestRunDescription mTestRunDescription
    private final Logger mLogger

    TestRunResultProcessor(final Logger logger, final TestRunDescription testRunDescription) {
        mTestRunDescription = testRunDescription
        mLogger = logger
    }

    TestCasesRunResult createTestCasesResult(final TestRunResult testRunResult) {
        mLogger.info "Processing test run results"

        final TestCasesRunResult testCasesRunResult = new TestCasesRunResult()
        if (testRunResult.testCases) {
            testRunResult.testResults.each { TestResult testResult ->
                testCasesRunResult.addTestCaseResult(testResult)
            }

            testCasesRunResult.testCasesResults.each { TestResult testResult ->
                testCasesRunResult.total++
                switch (testResult.status) {
                    case TestStatus.FAILED:
                        testCasesRunResult.failed++
                        testCasesRunResult.tried++
                        break
                    case TestStatus.SKIPPED:
                        testCasesRunResult.tried++
                        testCasesRunResult.skipped++
                        break
                    case TestStatus.PASSED:
                        testCasesRunResult.tried++
                        testCasesRunResult.passed++
                        break
                    case TestStatus.KNOWNBUG:
                        testCasesRunResult.hasBugs++
                        testCasesRunResult.filtered++
                        break
                }
            }

            mLogger.info("Processing test run results finished")
            mLogger.info(testCasesRunResult.toString())
        }

        return testCasesRunResult
    }
}