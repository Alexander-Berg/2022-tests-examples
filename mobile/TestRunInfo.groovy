package com.yandex.frankenstein.results.reporters

import com.yandex.frankenstein.description.TestCasesDescriptionProvider
import com.yandex.frankenstein.description.TestDescriptionDecoder
import com.yandex.frankenstein.description.TestDescriptionFileProvider
import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.description.ignored.IgnoredTestDescriptionDecoder
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.results.TaskDurationFileProvider
import com.yandex.frankenstein.results.TasksDurationParser
import com.yandex.frankenstein.results.TestRunResult
import com.yandex.frankenstein.results.XmlTestResultsParser
import com.yandex.frankenstein.results.testcase.TestCasesRunResult
import com.yandex.frankenstein.results.testcase.TestRunResultProcessor
import com.yandex.frankenstein.utils.ReportsPathProvider
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class TestRunInfo {

    final BuildInfo buildInfo
    final TestRunResult testRunResult
    final TestCasesRunResult testCasesRunResult
    final TestRunDescription testRunDescription

    TestRunInfo(final BuildInfo buildInfo,
                final TestRunResult testRunResult,
                final TestCasesRunResult testCasesRunResult,
                final TestRunDescription testRunDescription) {
        this.buildInfo = buildInfo
        this.testRunResult = testRunResult
        this.testCasesRunResult = testCasesRunResult
        this.testRunDescription = testRunDescription
    }

    static TestRunInfo load(final Project project,
                            final File testReportDir,
                            final File testCasesFile,
                            final BuildInfo buildInfo) {
        final TestRunDescription testRunDescription = new TestRunDescription()
        final TestDescriptionFileProvider testDescriptionFileProvider = new TestDescriptionFileProvider()

        final TestDescriptionDecoder testInfoDecoder = new TestDescriptionDecoder()
        final File testInfoDir = testDescriptionFileProvider.getTestRunDir(testReportDir)
        if (testInfoDir.exists()) {
            testInfoDir.eachFile { final File testInfoFile ->
                testInfoFile.eachLine { final String line ->
                    testRunDescription.addTestDescription(testInfoDecoder.decode(line))
                }
            }
        }

        final IgnoredTestDescriptionDecoder ignoredTestInfoDecoder = new IgnoredTestDescriptionDecoder()
        final File ignoredTestInfoDir = testDescriptionFileProvider.getIgnoredTestInfoDir(testReportDir)
        if (ignoredTestInfoDir.exists()) {
            ignoredTestInfoDir.eachFile { final File ignoredTestInfoFile ->
                ignoredTestInfoFile.eachLine { final String line ->
                    testRunDescription.addIgnoredTestDescription(ignoredTestInfoDecoder.decode(line))
                }
            }
        }

        final File testXmlReportDir = new ReportsPathProvider(testReportDir).getXmlReportDir()
        final XmlTestResultsParser runResultParser = new XmlTestResultsParser(project.logger, testXmlReportDir)
        final TestRunResult testRunResult = runResultParser.parseTestResults(testRunDescription)

        if (testCasesFile != null) {
            final TestCasesDescriptionProvider testCasesDescriptionDecoder = new TestCasesDescriptionProvider(testCasesFile)
            testCasesDescriptionDecoder.fillTestCasesDescription(testRunDescription)
        }

        final TasksDurationParser tasksDurationsParser = new TasksDurationParser(project.logger,
                new TaskDurationFileProvider(project.buildDir))
        testRunResult.tasksDurationsMillis = tasksDurationsParser.parseDurations()

        final TestRunResultProcessor testRunResultProcessor = new TestRunResultProcessor(project.logger, testRunDescription)
        final TestCasesRunResult testCaseRunResult = testRunResultProcessor.createTestCasesResult(testRunResult)

        return new TestRunInfo(buildInfo, testRunResult, testCaseRunResult, testRunDescription)
    }
}
