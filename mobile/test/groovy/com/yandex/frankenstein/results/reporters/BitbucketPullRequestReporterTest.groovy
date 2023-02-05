package com.yandex.frankenstein.results.reporters

import com.yandex.frankenstein.properties.info.BitbucketInfo
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import org.gradle.api.logging.Logger
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class BitbucketPullRequestReporterTest {

    final String expectedMessage = """FAILURE
Build name: testBuildName
Build url: test.com
OS: 1

tests: 85/95
(all: 100, failed: 10, skipped: 5)

test cases: 85/95
(all: 100, failed: 10, skipped: 0, filtered: 5/5)

all tests duration: 00h:03m:20s
task ':test' duration: 00h:00m:10s
"""

    TestRunResult mRunResult = new TestRunResult()
    TestCasesRunResult mTestCasesRunResult = new TestCasesRunResult()
    Logger dummyLogger = [
            info: {}
    ] as Logger
    BitbucketInfo mBitbucketInfo = new BitbucketInfo("projectKey", "repoName", "pullRequestId",
            "baseUrl", "token")
    BuildInfo mBuildInfo = new BuildInfo("test.com", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId")

    @Before
    void setUp() {
        mRunResult.allTests = 100
        mRunResult.testCases = 100
        mRunResult.tried = 95
        mRunResult.failed = 10
        mRunResult.passed = 85
        mRunResult.skipped = 5
        mRunResult.duration = 200

        mRunResult.tasksDurationsMillis['test'] = 10000L

        mTestCasesRunResult.total = 100
        mTestCasesRunResult.tried = 95
        mTestCasesRunResult.failed = 10
        mTestCasesRunResult.passed = 85
        mTestCasesRunResult.filtered = 5
        mTestCasesRunResult.hasBugs = 5
    }

    @Test
    void testReport() {
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, mBuildInfo) {
            protected String composeComment(final TestRunResult runResult,
                                            final TestCasesRunResult testCasesRunResult) {
                assertThat(runResult).isEqualTo(mRunResult)
                assertThat(testCasesRunResult).isEqualTo(mTestCasesRunResult)
                return super.composeComment(runResult, testCasesRunResult)
            }

            protected String sendComment(final String comment) {
                assertThat(comment).isEqualTo(expectedMessage)
                return "200"
            }
        }
        reporter.report(mRunResult, mTestCasesRunResult, null)
    }

    @Test
    void testComposeComment() {
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, mBuildInfo)
        final String message = reporter.composeComment(mRunResult, mTestCasesRunResult)
        assertThat(message).isEqualTo(expectedMessage)
    }

    @Test
    void testGetTemplateBinding() {
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, mBuildInfo)
        final Map<String, Object> binding = reporter.getTemplateBinding(mRunResult, mTestCasesRunResult)
        assertThat((binding.buildInfo as BuildInfo).toMap()).isEqualTo(mBuildInfo.toMap())
        assertThat(binding.runResult).isEqualTo(mRunResult)
        assertThat(binding.testCasesRunResult).isEqualTo(mTestCasesRunResult)
    }

    @Test
    void testGetHeaders() {
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, mBuildInfo)
        final Map headers = reporter.getHeaders()
        assertThat(headers).isEqualTo([
                'Authorization': "Basic ${"x-oauth-token:${mBitbucketInfo.token}".bytes.encodeBase64()}",
                'Content-Type': 'application/json',
                'Accept': 'application/json',
        ])
    }

    @Test
    void testGetCommentUrl() {
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, mBuildInfo)
        final String commentUrl = reporter.getCommentUrl()
        final String expectedUrl = "$mBitbucketInfo.baseUrl/rest/api/1.0/projects/$mBitbucketInfo.projectKey" +
                "/repos/$mBitbucketInfo.repoName/pull-requests/$mBitbucketInfo.pullRequestId/comments"
        assertThat(commentUrl).isEqualTo(expectedUrl)
    }

    @Test
    void testCanReport() {
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, mBuildInfo)
        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutProjectKey() {
        final BitbucketInfo bitbucketInfo = new BitbucketInfo("", "repoName", "pullRequestId", "baseUrl", "token")
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, bitbucketInfo, mBuildInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testCanReportWithoutRepoName() {
        final BitbucketInfo bitbucketInfo = new BitbucketInfo("projectKey", "", "pullRequestId", "baseUrl", "token")
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, bitbucketInfo, mBuildInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testCanReportWithoutPullRequestId() {
        final BitbucketInfo bitbucketInfo = new BitbucketInfo("projectKey", "repoName", "", "baseUrl", "token")
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, bitbucketInfo, mBuildInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testCanReportWithoutBaseUrl() {
        final BitbucketInfo bitbucketInfo = new BitbucketInfo("projectKey", "repoName", "pullRequestId", "", "token")
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, bitbucketInfo, mBuildInfo)

        assertThat(reporter.canReport()).isTrue()
    }

    @Test
    void testCanReportWithoutToken() {
        final BitbucketInfo bitbucketInfo = new BitbucketInfo("projectKey", "repoName", "pullRequestId", "baseUrl", "")
        final BitbucketPullRequestReporter reporter = new BitbucketPullRequestReporter(dummyLogger, bitbucketInfo, mBuildInfo)

        assertThat(reporter.canReport()).isFalse()
    }

    @Test
    void testGetFailureMessage() {
        final BitbucketPullRequestReporter bitbucketPullRequestReporter = new BitbucketPullRequestReporter(dummyLogger, mBitbucketInfo, null)
        assertThat(bitbucketPullRequestReporter.getFailureMessage()).contains(mBitbucketInfo.toString())
    }
}