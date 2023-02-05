package com.yandex.frankenstein.results.testcase

import com.yandex.frankenstein.description.TestDescription
import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.description.ignored.IgnoredTestDescription
import com.yandex.frankenstein.results.TestResult
import com.yandex.frankenstein.results.TestStatus
import com.yandex.frankenstein.results.TestRunResult
import org.gradle.api.logging.Logger
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestRunResultProcessorTest {

    final Logger dummyLogger = [
            info: {}
    ] as Logger

    final TestRunDescription mTestRunDescription = new TestRunDescription() {
        IgnoredTestDescription getIgnoredTestDescription(Integer id) {
            switch (id) {
                case 1: return new IgnoredTestDescription (
                        testCaseId: 1,
                        reason: "")
                case 2: return new IgnoredTestDescription (
                        testCaseId: 2,
                        reason: "")
                default: return null
            }
        }
    }

    final TestRunResult mTestRunResult = [
            testCases: 4,
            testResults: [
                    new TestResult(
                            testDescription: new TestDescription(
                                    testName: "testName1",
                                    testClass: "testClass1",
                                    testCaseId: 1),
                            status: TestStatus.KNOWNBUG,
                            description: "description1"
                    ),
                    new TestResult(
                            testDescription: new TestDescription(
                                    testName: "testName2",
                                    testClass: "testClass2",
                                    testCaseId: 2),
                            status: TestStatus.SKIPPED,
                            description: "description2"
                    ),
                    new TestResult(
                            testDescription: new TestDescription(
                                    testName: "testName3",
                                    testClass: "testClass3",
                                    testCaseId: 3),
                            status: TestStatus.FAILED,
                            description: "description3"
                    ),
                    new TestResult(
                            testDescription: new TestDescription(
                                    testName: "testName4",
                                    testClass: "testClass4",
                                    testCaseId: 4),
                            status: TestStatus.PASSED,
                            description: "description4"
                    )
            ]
    ] as TestRunResult

    @Test
    void testCreateTestCasesResult() {
        final TestRunResultProcessor testRunResultProcessor = new TestRunResultProcessor(dummyLogger, mTestRunDescription)
        TestCasesRunResult testCasesRunResult = testRunResultProcessor.createTestCasesResult(mTestRunResult)

        assertThat(testCasesRunResult.total).isEqualTo(4)
        assertThat(testCasesRunResult.failed).isEqualTo(1)
        assertThat(testCasesRunResult.tried).isEqualTo(3)
        assertThat(testCasesRunResult.hasBugs).isEqualTo(1)
        assertThat(testCasesRunResult.filtered).isEqualTo(1)
        assertThat(testCasesRunResult.passed).isEqualTo(1)
    }

    @Test
    void testCreateTestCasesResultWithoutCases() {
        final TestRunResultProcessor testRunResultProcessor = new TestRunResultProcessor(dummyLogger, mTestRunDescription)
        mTestRunResult.testCases = 0
        TestCasesRunResult testCasesRunResult = testRunResultProcessor.createTestCasesResult(mTestRunResult)

        assertThat(testCasesRunResult.total).isEqualTo(0)
        assertThat(testCasesRunResult.failed).isEqualTo(0)
        assertThat(testCasesRunResult.tried).isEqualTo(0)
        assertThat(testCasesRunResult.hasBugs).isEqualTo(0)
        assertThat(testCasesRunResult.filtered).isEqualTo(0)
        assertThat(testCasesRunResult.passed).isEqualTo(0)
    }
}