package com.yandex.frankenstein.results.reporters

import com.yandex.frankenstein.description.TestDescription
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.properties.info.SlackInfo
import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.results.TestResult
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.TestStatus
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import com.yandex.frankenstein.utils.UrlShortener
import com.yandex.frankenstein.utils.Utils
import org.gradle.api.logging.Logger
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class SlackReporterTest {

    final String expectedMessageBody = """test case: baseUiUrl/testcase/projectId-1
### testClass1.testName1
```
description1
```
test case: baseUiUrl/testcase/projectId-2
### testClass2.testName2
```
description2
```
"""

    final String expectedMessageHeader = """*testBuildName* 
*Failure*
> tests: *85*/95
> _(all: 100, failed: 10, skipped: 5, filtered: 0/0)_

> test cases: *85*/95
> _(all: 100, failed: 10, skipped: 0, filtered: 5/5)_

> _all tests duration: 00h:03m:20s_
> _task ':test' duration: 00h:00m:10s_
> _reported at: ${Utils.getFormattedDate(new Date())}_
> os api level: 1"""

    final TestRunResult mRunResult = new TestRunResult()
    final TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()
    final Logger dummyLogger = [:] as Logger
    final SlackInfo mSlackInfo = new SlackInfo("token","channelId", "", "")
    final BuildInfo mBuildInfo = new BuildInfo("", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId")
    final TestPalmInfo mTestPalmInfo = new TestPalmInfo("projectId", "suiteid",
            "mVersionId", "token", "baseUrl", "baseUiUrl", "one")

    @Before
    void setUp() {
        mRunResult.allTests = 100
        mRunResult.tried = 95
        mRunResult.failed = 10
        mRunResult.passed = 85
        mRunResult.skipped = 5
        mRunResult.duration = 200

        mRunResult.tasksDurationsMillis['test'] = 10000L
        mRunResult.testResults = [
                new TestResult(
                        testDescription: new TestDescription(testName: "testName1", testClass: "testClass1", testCaseId: 1),
                        status: TestStatus.FAILED,
                        description: "description1"
                ),
                new TestResult(
                        testDescription: new TestDescription(testName: "testName2", testClass: "testClass2", testCaseId: 2),
                        status: TestStatus.FAILED,
                        description: "description2"
                ),
                new TestResult(
                        testDescription: new TestDescription(testName: "testName3", testClass: "testClass3", testCaseId: 3),
                        status: TestStatus.PASSED,
                        description: "description3"
                )
        ] as List<TestResult>

        mTestCasesRunResult.total = 100
        mTestCasesRunResult.tried = 95
        mTestCasesRunResult.failed = 10
        mTestCasesRunResult.passed = 85
        mTestCasesRunResult.filtered = 5
        mTestCasesRunResult.hasBugs = 5
    }

    @Test
    void testReportTestResults() {
        final SlackReporter reporter = new SlackReporter(dummyLogger, mSlackInfo, mBuildInfo, mTestPalmInfo)
        final Map<String, String> results = reporter.getTestResults(mRunResult, mTestCasesRunResult)

        assertThat(results).isEqualTo([
                token: mSlackInfo.token,
                channels: mSlackInfo.channelId,
                filename: 'report.md',
                content: expectedMessageBody,
                filetype: 'markdown',
                title: 'Report',
                initial_comment: expectedMessageHeader,
                mrkdwn_in: '["initial_comment"]',
        ])
    }

    @Test
    void testCanReport() {
        final SlackReporter reporter = new SlackReporter(dummyLogger, mSlackInfo, mBuildInfo, mTestPalmInfo)

        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutToken() {
        final SlackInfo slackInfo = new SlackInfo("", "channelId", "", "")
        final SlackReporter reporter = new SlackReporter(dummyLogger, slackInfo, mBuildInfo, mTestPalmInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testCanReportWithoutChannelId() {
        final SlackInfo slackInfo = new SlackInfo("token", "", "", "")
        final SlackReporter reporter = new SlackReporter(dummyLogger, slackInfo, mBuildInfo, mTestPalmInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testComposeBuildUrl() {
        final BuildInfo buildInfo = new BuildInfo("test.com", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId")
        final String expectedUrl = "shortened.com"
        final UrlShortener urlShortener = [
                shortenUrl: { expectedUrl }
        ] as UrlShortener

        final SlackReporter reporter = new SlackReporter(dummyLogger, mSlackInfo, buildInfo, mTestPalmInfo)

        assertThat(reporter.composeBuildUrl(buildInfo.buildUrl, urlShortener)).isEqualTo(expectedUrl)
    }

    @Test
    void testComposeBuildUrlEmpty() {
        final SlackReporter reporter = new SlackReporter(dummyLogger, mSlackInfo, mBuildInfo, mTestPalmInfo)

        assertThat(reporter.composeBuildUrl(mBuildInfo.buildUrl, [:] as UrlShortener)).isEmpty()
    }

    @Test
    void testGetFailureMessage() {
        final SlackReporter reporter = new SlackReporter(dummyLogger, mSlackInfo, mBuildInfo, mTestPalmInfo)
        assertThat(reporter.getFailureMessage()).contains(mSlackInfo.toString())
    }
}
