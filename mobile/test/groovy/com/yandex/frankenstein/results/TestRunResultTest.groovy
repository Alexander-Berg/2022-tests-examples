package com.yandex.frankenstein.results

import com.yandex.frankenstein.description.TestDescription
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestRunResultTest {

    final TestRunResult mTestRunResult = new TestRunResult()
    final TestDescription mTestDescription = new TestDescription(
                    testName: "app_metrica_test_name1",
                    testClass: "app_metrica_test_class1",
                    testCaseId: 1,
            )
    final TestDescription mFaliedTestDescription = new TestDescription(
            testName: "app_metrica_test_name2",
            testClass: "app_metrica_test_class2",
            testCaseId: 2,
    )
    final TestStatus mTestResultStatus = TestStatus.PASSED
    final String mDescription = "description1"

    @Test
    void testAddTestResult() {
        mTestRunResult.addTestResult(mTestDescription, mTestResultStatus, mDescription)
        assertThat(mTestRunResult.testResults).hasSize(1)
        final TestResult testResult = mTestRunResult.testResults.first()
        assertThat(testResult.testDescription).isEqualTo(mTestDescription)
        assertThat(testResult.status).isEqualTo(mTestResultStatus)
        assertThat(testResult.description).isEqualTo(mDescription)
    }

    @Test
    void testAddTestResultWithoutDescription() {
        mTestRunResult.addTestResult(mTestDescription, mTestResultStatus)
        assertThat(mTestRunResult.testResults).hasSize(1)
        final TestResult testResult = mTestRunResult.testResults.first()
        assertThat(testResult.testDescription).isEqualTo(mTestDescription)
        assertThat(testResult.status).isEqualTo(mTestResultStatus)
        assertThat(testResult.description).isEqualTo('')
    }

    @Test
    void testGetFailedTests() {
        mTestRunResult.addTestResult(mFaliedTestDescription, TestStatus.FAILED)
        mTestRunResult.addTestResult(mTestDescription, TestStatus.PASSED)
        final List<TestResult> failedTests = mTestRunResult.getFailedTests()
        assertThat(failedTests).hasSize(1)
        final TestResult testResult = failedTests.first()
        assertThat(testResult.testDescription).isEqualTo(mFaliedTestDescription)
        assertThat(testResult.status).isEqualTo(TestStatus.FAILED)
    }

    @Test
    void testGetFormattedAllTestsTime() {
        mTestRunResult.duration = 31415.926
        final String formattedDuration = mTestRunResult.getFormattedAllTestsTime()
        assertThat(formattedDuration).isEqualTo("08h:43m:35s")
    }

    @Test
    void testGetFormattedTaskDuration() {
        final String taskName = "task_name1"
        mTestRunResult.tasksDurationsMillis.put(taskName, 31415926)
        final String formattedDuration = mTestRunResult.getFormattedTaskDuration(taskName)
        assertThat(formattedDuration).isEqualTo("08h:43m:35s")
    }

    @Test
    void testGetFormattedTaskDurationIfEmpty() {
        final String taskName = "task_name1"
        final String formattedDuration = mTestRunResult.getFormattedTaskDuration(taskName)
        assertThat(formattedDuration).isEqualTo("00h:00m:-1s")
    }

    @Test
    void testToString() {
        mTestRunResult.allTests = 1
        mTestRunResult.testCases = 2
        mTestRunResult.passed = 3
        mTestRunResult.tried = 4
        mTestRunResult.skipped = 5
        mTestRunResult.failed = 6
        mTestRunResult.filtered = 7
        mTestRunResult.hasBugs = 8
        mTestRunResult.duration = 3.1415
        final String testRunResultString = mTestRunResult.toString()
        final String expected = """
TestRunResult {
    allTests='1'
    testCases='2'
    passed='3'
    tried='4'
    skipped='5'
    failed='6'
    filtered='7'
    hasBugs='8'
    duration='3.1415'
'}
"""
        assertThat(testRunResultString).isEqualTo(expected)
    }
}
