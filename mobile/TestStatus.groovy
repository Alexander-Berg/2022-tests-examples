package com.yandex.frankenstein.results

import groovy.transform.CompileStatic

@CompileStatic
enum TestStatus {
    STARTED, PASSED, SKIPPED, FAILED, KNOWNBUG
}
