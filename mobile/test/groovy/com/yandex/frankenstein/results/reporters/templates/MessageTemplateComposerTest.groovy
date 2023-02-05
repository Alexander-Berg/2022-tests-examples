package com.yandex.frankenstein.results.reporters.templates

import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class MessageTemplateComposerTest {

    final String templateName = "some_name"
    final TestRunResult mTestRunResult = new TestRunResult(
            allTests: 123,
            testCases: 1,
            passed: 2,
            tried: 3,
            skipped: 4,
            failed: 5,
            filtered: 6,
            hasBugs: 7,
            duration: 3.1415,
    )

    @Test
    void testComposeBody() {
        final MessageTemplateComposer messageTemplateComposer =
                new MessageTemplateComposer("appmetrica")

        assertThat(messageTemplateComposer.composeBody(["runResult": mTestRunResult]))
                .isEqualTo("\"testrun\" : {\n" +
                           "    \"status\": \"failed\",\n" +
                           "    \"total\": 123,\n" +
                           "    \"passed\": 2,\n" +
                           "    \"failed\": 5,\n" +
                           "    \"skipped\": 4,\n" +
                           "    \"test cases\": 1,\n" +
                           "    \"duration\": \"00h:00m:03s\"\n" +
                           "}\n")
    }

    @Test
    void testComposeHeader() {
        final MessageTemplateComposer messageTemplateComposer =
                new MessageTemplateComposer("header")

        assertThat(messageTemplateComposer.composeHeader([
                "testCasesRunResult": [
                        passed: 0,
                        failed: 1,
                        skipped: 2,
                        tried: 3,
                        total: 4,
                        filtered: 5,
                        hasBugs: 6
                ] as TestCasesRunResult,
                "buildInfo": new BuildInfo("testBuildUrl", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId"),
                "runResult": mTestRunResult
        ])).isEqualTo("FAILURE\n" +
                "Build name: testBuildName\n" +
                "Build url: testBuildUrl\n" +
                "OS: 1\n" +
                "\n" +
                "tests: 2/3\n" +
                "(all: 123, failed: 5, skipped: 4)\n" +
                "\n" +
                "test cases: 0/3\n" +
                "(all: 4, failed: 1, skipped: 2, filtered: 6/5)\n" +
                "\n" +
                "all tests duration: 00h:00m:03s\n" +
                "task ':test' duration: 00h:00m:-1s\n")
    }

    @Test
    void testComposeHeaderWithoutTestCases() {
        final MessageTemplateComposer messageTemplateComposer =
                new MessageTemplateComposer("header")

        mTestRunResult.testCases = 0
        assertThat(messageTemplateComposer.composeHeader([
                "testCasesRunResult": [
                        passed: 0,
                        failed: 0,
                        skipped: 0,
                        tried: 0,
                        total: 0,
                        filtered: 0,
                        hasBugs: 0
                ] as TestCasesRunResult,
                "buildInfo": new BuildInfo("testBuildUrl", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId"),
                "runResult": mTestRunResult
        ])).isEqualTo("FAILURE\n" +
                "Build name: testBuildName\n" +
                "Build url: testBuildUrl\n" +
                "OS: 1\n" +
                "\n" +
                "tests: 2/3\n" +
                "(all: 123, failed: 5, skipped: 4)\n" +
                "\n" +
                "all tests duration: 00h:00m:03s\n" +
                "task ':test' duration: 00h:00m:-1s\n")
    }

    @Test
    void testComposeIfNoTemplate() {
        final TemplateResolvingStrategy templateResolvingStrategy = new TemplateResolvingStrategy() {
            InputStream getTemplateInputStream(final String type, final String name) {
                return null
            }
        }
        final MessageTemplateComposer messageTemplateComposer = new MessageTemplateComposer(
                templateName, templateResolvingStrategy)

        assertThat(messageTemplateComposer.composeHeader([:])).isEmpty()
    }
}
