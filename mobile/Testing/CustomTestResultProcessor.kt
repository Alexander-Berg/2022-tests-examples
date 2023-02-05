package com.yandex.frankenstein.testing

import org.gradle.api.internal.tasks.testing.DefaultTestClassDescriptor
import org.gradle.api.internal.tasks.testing.TestCompleteEvent
import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.gradle.api.internal.tasks.testing.TestStartEvent
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestOutputEvent

class CustomTestResultProcessor(
    private val logger: Logger,
    private val testResultProcessor: TestResultProcessor,
    private val index: Int,
    private val testClassCompletedListener: TestClassCompletedListener
) : TestResultProcessor {

    private var currentTestClassId: String? = null

    override fun started(test: TestDescriptorInternal, event: TestStartEvent?) {
        if (test is DefaultTestClassDescriptor) {
            currentTestClassId = test.id.toString()
            logger.info("Started test class ${test.classDisplayName} with id ${test.id} on processor $index")
        }
        testResultProcessor.started(test, event)
    }

    override fun completed(testId: Any?, event: TestCompleteEvent?) {
        if (testId.toString() == currentTestClassId) {
            currentTestClassId = null
            logger.info("Finished test class with id $testId on processor $index")
            testClassCompletedListener.onTestClassCompleted(index)
        }
        testResultProcessor.completed(testId, event)
    }

    override fun output(testId: Any?, event: TestOutputEvent?) = testResultProcessor.output(testId, event)

    override fun failure(testId: Any?, result: Throwable?) = testResultProcessor.failure(testId, result)
}
