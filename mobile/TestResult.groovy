package com.yandex.frankenstein.results

import groovy.transform.CompileStatic
import com.yandex.frankenstein.description.TestDescription

@CompileStatic
class TestResult {
    TestDescription testDescription
    TestStatus status
    String description

    @Override
    String toString() {
        return """
TestResult {
    testDescription: '${testDescription.toString()}'.
    status: '${status.toString()}',
    description: '${description}'
}
"""
    }
}
