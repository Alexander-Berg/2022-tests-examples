package com.yandex.frankenstein.results.testcase

import com.yandex.frankenstein.results.TestResult
import com.yandex.frankenstein.results.TestStatus
import groovy.transform.CompileStatic

@CompileStatic
class TestCasesRunResult {
    int passed
    int failed
    int skipped
    int tried
    int total
    int filtered
    int hasBugs
    List<TestResult> testCasesResults = []

    void addTestCaseResult(TestResult newTestResult) {
        TestResult oldTestResult = testCasesResults.find { TestResult testResult ->
            testResult.testDescription.testCaseId == newTestResult.testDescription.testCaseId
        }
        if (oldTestResult && newTestResult.status != TestStatus.PASSED && oldTestResult.status != TestStatus.FAILED) {
            testCasesResults.remove(oldTestResult)
            testCasesResults << newTestResult
        } else if (oldTestResult == null) {
            testCasesResults << newTestResult
        }
    }

    @Override
    String toString() {
        return "*$passed*/$tried _(all: $total, failed: $failed, " +
                "skipped: $skipped, filtered: $hasBugs/$filtered)_"
    }
}