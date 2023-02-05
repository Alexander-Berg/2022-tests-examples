package com.yandex.frankenstein.description

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic

@CompileStatic
class TestDescriptionDecoder {

    TestDescription decode(String line) {
        TestDescription test = new TestDescription()
        Map<String, ?> json = new JsonSlurper().parseText(line) as Map
        test.testName = json['name']
        test.testClass = json['class']
        test.testCaseId = json['case id'] as int
        return test
    }
}