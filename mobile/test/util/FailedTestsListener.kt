package ru.yandex.market.test.util

import androidx.test.internal.runner.listener.InstrumentationRunListener
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure

class FailedTestsListener : InstrumentationRunListener() {

    private val delegate = FailedTestsDelegate

    override fun testRunStarted(description: Description?) {
        delegate.testRunStarted(instrumentation)
    }

    override fun testFailure(failure: Failure?) {
        delegate.testFailure(failure)
    }

    override fun testRunFinished(result: Result?) {
        delegate.testRunFinished()
    }
}