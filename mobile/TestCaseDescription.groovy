package com.yandex.frankenstein.description

import groovy.transform.CompileStatic

@CompileStatic
class TestCaseDescription {
    final int testCaseId
    final List<String> hasBugs
    final List<String> priority
    final List<String> functionality
    final List<Map<String, String>> issues

    TestCaseDescription(final int testCaseId) {
        this(testCaseId, [], [], [], [])
    }

    TestCaseDescription(final int testCaseId, final List<String> hasBugs, final List<String> priority,
                        final List<String> functionality, final List<Map<String, String>> issues) {
        this.testCaseId = testCaseId
        this.hasBugs = hasBugs
        this.priority = priority
        this.functionality = functionality
        this.issues = issues
    }
}
