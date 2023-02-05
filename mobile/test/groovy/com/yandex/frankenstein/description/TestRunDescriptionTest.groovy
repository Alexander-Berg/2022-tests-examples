package com.yandex.frankenstein.description

import com.yandex.frankenstein.description.ignored.IgnoredTestDescription
import groovy.transform.CompileStatic
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

@CompileStatic
class TestRunDescriptionTest {

    private final String mTestName = 'testName'
    private final String mTestNameWithoutId = 'testNameWithoutId'
    private final String mTestClass = 'testClass'
    private final String mTestClassWithoutId = 'testClassWithoutId'
    private final Integer mTestCaseId = 1
    private final String mIgnoredReason = 'someReason'
    private final TestDescription mTestDescription =
            new TestDescription(testName: mTestName, testClass: mTestClass, testCaseId: mTestCaseId)
    private final IgnoredTestDescription mIgnoredTestDescription =
            new IgnoredTestDescription(testCaseId: mTestCaseId, reason: mIgnoredReason)
    private final TestCaseDescription mTestCaseDescription =
            new TestCaseDescription(mTestCaseId, [], [], [], [])
    final TestRunDescription mTestRunDescription = new TestRunDescription()

    @Test
    void testAddTestDescriptions() {
        mTestRunDescription.addTestDescription(mTestDescription)

        assertThat(mTestRunDescription.testCaseIds).containsExactly(mTestCaseId)
    }

    @Test
    void testGetTestDescriptions() {
        mTestRunDescription.addTestDescription(mTestDescription)
        final TestDescription testDescription = mTestRunDescription.getTestDescription(mTestClass, mTestName)

        assertThat(testDescription.testCaseId).isEqualTo(mTestCaseId)
    }

    @Test
    void testGetTestDescriptionsWithWrongName() {
        mTestRunDescription.addTestDescription(mTestDescription)
        final TestDescription testDescription = mTestRunDescription.getTestDescription(mTestName, mTestClassWithoutId)

        assertThat(testDescription.testCaseId).isEqualTo(0)
    }

    @Test
    void testGetTestDescriptionsWithWrongClass() {
        mTestRunDescription.addTestDescription(mTestDescription)
        final TestDescription testDescription = mTestRunDescription.getTestDescription(mTestNameWithoutId, mTestClass)

        assertThat(testDescription.testCaseId).isEqualTo(0)
    }

    @Test
    void testGetTestDescriptionsWithWrongNameAndClass() {
        mTestRunDescription.addTestDescription(mTestDescription)
        final TestDescription testDescription = mTestRunDescription.getTestDescription(mTestNameWithoutId, mTestClassWithoutId)

        assertThat(testDescription.testCaseId).isEqualTo(0)
    }

    @Test
    void testAddIgnoredTestDescriptions() {
        mTestRunDescription.addIgnoredTestDescription(mIgnoredTestDescription)

        assertThat(mTestRunDescription.testCaseIds).isEmpty()
    }

    @Test
    void testGetIgnoredTestDescriptions() {
        mTestRunDescription.addIgnoredTestDescription(mIgnoredTestDescription)
        final IgnoredTestDescription ignoredTestDescription = mTestRunDescription.getIgnoredTestDescription(mTestCaseId)

        assertThat(ignoredTestDescription.testCaseId).isEqualTo(mTestCaseId)
        assertThat(ignoredTestDescription.reason).isEqualTo(mIgnoredReason)
    }

    @Test
    void testAddTestCaseDescriptions() {
        mTestRunDescription.addTestCaseDescription(mTestCaseDescription)

        assertThat(mTestRunDescription.testCaseIds).isEmpty()
    }

    @Test
    void testGetTestCaseDescriptions() {
        mTestRunDescription.addTestCaseDescription(mTestCaseDescription)
        final TestCaseDescription testCaseDescription = mTestRunDescription.getTestCaseDescription(mTestCaseId)

        assertThat(testCaseDescription.testCaseId).isEqualTo(mTestCaseId)
    }
}