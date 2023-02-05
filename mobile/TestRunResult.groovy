package com.yandex.frankenstein.results

import groovy.transform.CompileStatic
import com.yandex.frankenstein.description.TestDescription

import java.time.Duration

@CompileStatic
class TestRunResult {

    int allTests
    int testCases
    int passed
    int tried
    int skipped
    int failed
    int filtered
    int hasBugs
    double duration
    Map<String, Long> tasksDurationsMillis = [:]
    List<TestResult> testResults = []

    TestRunResult() {}

    void addTestResult(final TestDescription testDescription, final TestStatus status, final String description = '') {
        testResults << new TestResult(testDescription: testDescription, status: status, description: description)
    }

    List<TestResult> getFailedTests() {
        testResults.findAll { final TestResult testResult ->
            testResult.status == TestStatus.FAILED
        }
    }

    String getFormattedAllTestsTime() {
        final long allSeconds = (long) duration
        return formatDuration(Duration.ofSeconds(allSeconds))
    }

    String getFormattedTaskDuration(final String taskName) {
        final long allMillis =  tasksDurationsMillis.getOrDefault(taskName, -1L)
        return formatDuration(Duration.ofMillis(allMillis))
    }

    @Override
    String toString() {
        return """
TestRunResult {
    allTests='$allTests'
    testCases='$testCases'
    passed='$passed'
    tried='$tried'
    skipped='$skipped'
    failed='$failed'
    filtered='$filtered'
    hasBugs='$hasBugs'
    duration='$duration'
'}
"""
    }

    private String formatDuration(final Duration duration) {
        final Duration minutes = duration.minusHours(duration.toHours())
        final Duration seconds = minutes.minusMinutes(minutes.toMinutes())
        return String.format('%02dh:%02dm:%02ds', duration.toHours(), minutes.toMinutes(), seconds.seconds)
    }
}