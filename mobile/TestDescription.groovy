package com.yandex.frankenstein.description

import groovy.transform.CompileStatic

@CompileStatic
class TestDescription {
    String testName
    String testClass
    int testCaseId

    @Override
    String toString() {
        return """
TestDescription {
    testName: '${testName}',
    testClass: '${testClass}',
    testCaseId: '${testCaseId}'
}
"""
    }
}