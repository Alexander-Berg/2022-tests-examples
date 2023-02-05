package com.yandex.frankenstein.results

import com.yandex.frankenstein.description.TestDescription
import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.description.ignored.IgnoredTestDescription
import org.gradle.api.logging.Logger
import org.junit.Before
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class XmlTestResultsParserTest {

    final String mXmlFileText = """<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="ActivateTest">
    <properties/>
    <testcase name="testFailed" classname="ActivateTest" time="1">
        <failure message="messageFailed1" type="error">
            failure message1
        </failure>
        <failure message="" type="error">
            failure message2
        </failure>
        <system-out><![CDATA[some text
        more text
        ]]></system-out>
    </testcase>
    <testcase name="testPassed" classname="ActivateTest" time="2"/>
    <testcase name="testPassedWithoutTestDescription" classname="ActivateTest" time="3"/>
    <testcase name="testSkipped" classname="ActivateTest" time="4">
        <skipped/>
    </testcase>
    <testcase name="testHasBugs" classname="ActivateTest" time="5">
        <skipped/>
    </testcase>
    <testcase name="testSkippedWithoutIgnoredTestDescription" classname="ActivateTest" time="6">
        <skipped/>
    </testcase>
    <system-out>system out log</system-out>
    <system-err>system err log</system-err>
</testsuite>
"""

    final String expected = """[
TestResult {
    testDescription: '
TestDescription {
    testName: 'testFailed',
    testClass: 'ActivateTest',
    testCaseId: '41'
}
'.
    status: 'FAILED',
    description: 'messageFailed1
            failure message1
        
            failure message2
        '
}
, 
TestResult {
    testDescription: '
TestDescription {
    testName: 'testPassed',
    testClass: 'ActivateTest',
    testCaseId: '42'
}
'.
    status: 'PASSED',
    description: ''
}
, 
TestResult {
    testDescription: '
TestDescription {
    testName: 'testPassedWithoutTestDescription',
    testClass: 'ActivateTest',
    testCaseId: '0'
}
'.
    status: 'PASSED',
    description: ''
}
, 
TestResult {
    testDescription: '
TestDescription {
    testName: 'testSkipped',
    testClass: 'ActivateTest',
    testCaseId: '43'
}
'.
    status: 'SKIPPED',
    description: ''
}
, 
TestResult {
    testDescription: '
TestDescription {
    testName: 'testHasBugs',
    testClass: 'ActivateTest',
    testCaseId: '44'
}
'.
    status: 'KNOWNBUG',
    description: ''
}
, 
TestResult {
    testDescription: '
TestDescription {
    testName: 'testSkippedWithoutIgnoredTestDescription',
    testClass: 'ActivateTest',
    testCaseId: '45'
}
'.
    status: 'SKIPPED',
    description: ''
}
]"""

    final String mWrongXmlFileText = """wrong xml"""

    final Logger mDummyLogger = [
            info: {},
            lifecycle: {},
    ] as Logger
    final File xmlLogDir = File.createTempDir()
    final TestRunDescription mTestRunDescription = new TestRunDescription()

    @Before
    void setUp() {
        mTestRunDescription.addTestDescription(new TestDescription(
                testName: "testFailed",
                testClass: "ActivateTest",
                testCaseId: 41
        ))
        mTestRunDescription.addTestDescription(new TestDescription(
                testName: "testPassed",
                testClass: "ActivateTest",
                testCaseId: 42
        ))
        mTestRunDescription.addTestDescription(new TestDescription(
                testName: "testSkipped",
                testClass: "ActivateTest",
                testCaseId: 43
        ))
        mTestRunDescription.addTestDescription(new TestDescription(
                testName: "testHasBugs",
                testClass: "ActivateTest",
                testCaseId: 44
        ))
        mTestRunDescription.addTestDescription(new TestDescription(
                testName: "testSkippedWithoutIgnoredTestDescription",
                testClass: "ActivateTest",
                testCaseId: 45
        ))

        mTestRunDescription.addIgnoredTestDescription(new IgnoredTestDescription(
                testCaseId: 43,
                reason: "no reason"
        ))
        mTestRunDescription.addIgnoredTestDescription(new IgnoredTestDescription(
                testCaseId: 44,
                reason: "has_bugs"
        ))
    }

    @Test
    void testParseTestResults() {
        final File xmlLog = new File(xmlLogDir, "testLog.xml")
        xmlLog << mXmlFileText
        final XmlTestResultsParser parser = new XmlTestResultsParser(mDummyLogger, xmlLogDir)
        final TestRunResult result = parser.parseTestResults(mTestRunDescription)
        assertThat(result.allTests).isEqualTo(6)
        assertThat(result.testCases).isEqualTo(5)
        assertThat(result.passed).isEqualTo(2)
        assertThat(result.tried).isEqualTo(5)
        assertThat(result.skipped).isEqualTo(2)
        assertThat(result.failed).isEqualTo(1)
        assertThat(result.filtered).isEqualTo(1)
        assertThat(result.hasBugs).isEqualTo(1)
        assertThat(result.duration).isEqualTo(new Double("21.0"))
        assertThat(result.testResults).hasSize(6)
        assertThat(result.testResults.toString()).isEqualTo(expected)
    }

    @Test
    void testParseTestResultsWithoutDirectory() {
        final XmlTestResultsParser parser = new XmlTestResultsParser(mDummyLogger, File.createTempFile("3.1415", "3.1415"))
        final TestRunResult result = parser.parseTestResults(mTestRunDescription)
        assertThat(result.allTests).isEqualTo(0)
        assertThat(result.testCases).isEqualTo(0)
        assertThat(result.passed).isEqualTo(0)
        assertThat(result.tried).isEqualTo(0)
        assertThat(result.skipped).isEqualTo(0)
        assertThat(result.failed).isEqualTo(0)
        assertThat(result.filtered).isEqualTo(0)
        assertThat(result.hasBugs).isEqualTo(0)
        assertThat(result.duration).isEqualTo(new Double("0"))
        assertThat(result.testResults).isEmpty()
    }

    @Test
    void testParseTestResultsWithWrongFile() {
        final File xmlLog = new File(xmlLogDir, "testLog.xml")
        xmlLog << mWrongXmlFileText
        final XmlTestResultsParser parser = new XmlTestResultsParser(mDummyLogger, xmlLogDir)
        final TestRunResult result = parser.parseTestResults(mTestRunDescription)
        assertThat(result.allTests).isEqualTo(0)
        assertThat(result.testCases).isEqualTo(5)
        assertThat(result.passed).isEqualTo(0)
        assertThat(result.tried).isEqualTo(0)
        assertThat(result.skipped).isEqualTo(0)
        assertThat(result.failed).isEqualTo(0)
        assertThat(result.filtered).isEqualTo(0)
        assertThat(result.hasBugs).isEqualTo(0)
        assertThat(result.duration).isEqualTo(new Double("0"))
        assertThat(result.testResults).isEmpty()
    }
}
