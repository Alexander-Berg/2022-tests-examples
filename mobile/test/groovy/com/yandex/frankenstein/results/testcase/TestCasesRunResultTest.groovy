package com.yandex.frankenstein.results.testcase

import com.yandex.frankenstein.description.TestDescription
import com.yandex.frankenstein.results.TestResult
import com.yandex.frankenstein.results.TestStatus
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestCasesRunResultTest {
    final int passed = 1
    final int failed = 2
    final int skipped = 3
    final int tried = 4
    final int total = 5
    final int filtered = 6
    final int hasBugs = 7

    final TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()

    final TestResult oldTestResult = new TestResult(
            testDescription: new TestDescription(
                    testName: "old_testName",
                    testClass: "old_testClass",
                    testCaseId: 1),
            status: TestStatus.PASSED,
            description: "old_description"
    )
    final TestResult newTestResult = new TestResult(
            testDescription: new TestDescription(
                    testName: "new_testName",
                    testClass: "new_testClass",
                    testCaseId: 1),
            status: TestStatus.PASSED,
            description: "new_description"
    )

    @Test
    void testToString() {
        final TestCasesRunResult testCasesRunResult = new TestCasesRunResult(
                passed: passed,
                failed: failed,
                skipped: skipped,
                tried: tried,
                total: total,
                filtered: filtered,
                hasBugs: hasBugs
        )
        final String testCasesRunResultString = "*$passed*/$tried _(all: $total, failed: $failed, " +
                "skipped: $skipped, filtered: $hasBugs/$filtered)_"
        assertThat(testCasesRunResult.toString()).isEqualTo(testCasesRunResultString)
    }

    @Test
    void testAddTestCaseResult() {
        addTestCasesResult(mTestCasesRunResult, newTestResult)
        assertThat(mTestCasesRunResult.testCasesResults).containsExactly(newTestResult)
    }

    @Test
    void testAddTestCaseResultWithOldResult() {
        newTestResult.status = TestStatus.FAILED
        addTestCasesResult(mTestCasesRunResult, oldTestResult, newTestResult)
        assertThat(mTestCasesRunResult.testCasesResults).containsExactly(newTestResult)
    }

    @Test
    void testAddTestCaseResultWithOldFailedResult() {
        oldTestResult.status = TestStatus.FAILED
        addTestCasesResult(mTestCasesRunResult, oldTestResult, newTestResult)
        assertThat(mTestCasesRunResult.testCasesResults).containsExactly(oldTestResult)
    }

    @Test
    void testAddTestCaseResultWithNewPassedResult() {
        newTestResult.status = TestStatus.PASSED
        addTestCasesResult(mTestCasesRunResult, oldTestResult, newTestResult)
        assertThat(mTestCasesRunResult.testCasesResults).containsExactly(oldTestResult)
    }

    static void addTestCasesResult(final TestCasesRunResult testCasesRunResult, final TestResult... testResults) {
        testResults.each { testCasesRunResult.addTestCaseResult(it) }
    }
}
