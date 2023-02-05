package com.yandex.frankenstein.description

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class TestCasesDescriptionProvider {

    private final File mTestCasesDescriptionFile

    TestCasesDescriptionProvider(final File testCasesDescriptionFile) {
        mTestCasesDescriptionFile = testCasesDescriptionFile
    }

    void fillTestCasesDescription(final TestRunDescription testRunDescription) {
        if (mTestCasesDescriptionFile.exists()) {
            final Map<Integer, TestCaseDescription> attributes = readAttributes()
            attributes.values().each { final TestCaseDescription testCaseDescription ->
                testRunDescription.addTestCaseDescription(testCaseDescription)
            }
        }
    }

    private Map<Integer, TestCaseDescription> readAttributes() {
        final List<Map<String, ?>> testCases = new JsonSlurper().parseText(mTestCasesDescriptionFile.text) as List
        final Map<Integer, TestCaseDescription> result = testCases.collectEntries { final Map<String, ?> testCase -> [
                (testCase.id): new TestCaseDescription(
                        testCase.id as int,
                        testCase.attributes['has_bugs'] as List<String>,
                        testCase.attributes['priority'] as List<String>,
                        testCase.attributes['functionality'] as List<String>,
                        testCase.bugs as List<Map<String, String>>,
                )
        ]}
        return result
    }
}
