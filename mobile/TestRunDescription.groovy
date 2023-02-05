package com.yandex.frankenstein.description

import com.yandex.frankenstein.description.ignored.IgnoredTestDescription
import groovy.transform.CompileStatic

@CompileStatic
class TestRunDescription {
    private final Map<String, TestDescription> mTestCases = [:]
    private final Map<Integer, IgnoredTestDescription> mIgnoredTestCases = [:]
    private final Map<Integer, TestCaseDescription> mTestCasesDescription = [:]
    final Set<Integer> testCaseIds = []

    TestDescription getTestDescription(final String testClass, final String testName) {
        return mTestCases.get(fullTestName(testClass, testName)) ?:
                new TestDescription(testClass: testClass, testName: testName)
    }

    IgnoredTestDescription getIgnoredTestDescription(final Integer id) {
        return mIgnoredTestCases[id]
    }

    TestCaseDescription getTestCaseDescription(final Integer id) {
        return mTestCasesDescription.getOrDefault(id, new TestCaseDescription(id))
    }

    void addTestDescription(final TestDescription testDescription) {
        mTestCases[fullTestName(testDescription)] = testDescription
        testCaseIds.add(testDescription.testCaseId)
    }

    void addIgnoredTestDescription(final IgnoredTestDescription testDescription) {
        mIgnoredTestCases[testDescription.testCaseId] = testDescription
    }

    void addTestCaseDescription(final TestCaseDescription testCaseDescription){
        mTestCasesDescription[testCaseDescription.testCaseId] = testCaseDescription
    }

    private String fullTestName(final String testClass, final String testName) {
        return "$testClass#$testName"
    }

    private String fullTestName(final TestDescription testDescription) {
        return fullTestName(testDescription.testClass, testDescription.testName)
    }
}