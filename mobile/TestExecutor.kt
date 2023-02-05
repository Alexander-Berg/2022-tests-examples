package com.yandex.frankenstein.runner

import com.yandex.frankenstein.Log
import com.yandex.frankenstein.filters.ExecutionFilter
import com.yandex.frankenstein.filters.FilterProvider
import com.yandex.frankenstein.runner.listener.TestInfoRunListener
import com.yandex.frankenstein.steps.MockWebServerRule
import com.yandex.frankenstein.steps.MockWebServerStepProvider
import org.junit.internal.AssumptionViolatedException
import org.junit.internal.runners.model.EachTestNotifier
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.rules.Timeout
import org.junit.runner.Description
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement

class TestExecutor internal constructor(
    private val testParameters: List<Any?>,
    private val executionFilter: ExecutionFilter,
    private val testExecutorDelegate: TestExecutorDelegate,
    private val testInfoRunListeners: List<RunListener>,
    private val mockWebServerStepProvider: MockWebServerStepProvider
) {

    constructor(
        testParameters: List<Any?>,
        filterProvider: FilterProvider = DummyFilterProvider(),
        delegate: TestExecutorDelegate = DummyTestExecutorDelegate(),
        mockWebServerStepProvider: MockWebServerStepProvider = object : MockWebServerStepProvider {}
    ) : this(
        testParameters,
        ExecutionFilter(filterProvider),
        delegate,
        listOf(TestInfoRunListener(testParameters)),
        mockWebServerStepProvider
    )

    fun getTestRules(testRules: MutableList<TestRule>): List<TestRule> {
        val resultRules = mutableListOf<TestRule>()
        if (testRules.none { it is Timeout }) {
            val timeout = getSystemIntProperty("frankenstein.test.timeout.seconds").toLong()
            resultRules.add(Timeout.seconds(timeout))
        }
        resultRules.addAll(testRules.filterNot { it is MockWebServerRule<*> })

        val mockWebServerSteps = mockWebServerStepProvider.createMockWebServerSteps() + testRules.filterIsInstance<MockWebServerRule<*>>()
        resultRules.add(wrapMockWebServerRules(mockWebServerSteps))

        return resultRules
    }

    private fun wrapMockWebServerRules(rules: List<MockWebServerRule<*>>): TestRule {
        var chain = RuleChain.emptyRuleChain()
        rules.forEach { chain = chain.around(it) }
        rules.forEach {
            chain = chain.around(
                object : ExternalResource() {
                    override fun before() {
                        it.updateData()
                    }
                }
            )
        }
        chain = chain.around(object : ExternalResource() {
            override fun before() = testExecutorDelegate.before()

            override fun after() = testExecutorDelegate.after()
        })
        return chain
    }

    fun getChildren(children: List<FrameworkMethod?>): List<FrameworkMethod> {
        val repetitionsCount = getSystemIntProperty("frankenstein.test.repetitions.count")
        return executionFilter.filterChildren(children, testParameters)
            .flatMap { method ->
                listOf(method, *Array(repetitionsCount) { RepeatedFrameworkMethod(method.method, it + 1) })
            }
    }

    fun runChild(
        method: FrameworkMethod,
        description: Description,
        notifier: RunNotifier,
        ignoredByRunner: Boolean,
        statement: Statement
    ) {
        Log.i(TAG, "Trying to run child ${getTestName(description)}")
        notifier.addCustomListeners()
        if (executionFilter.isIgnored(method, testParameters, ignoredByRunner)) {
            Log.i(TAG, "Test ${getTestName(description)} was ignored due to filter")
            notifier.fireTestIgnored(description)
        } else {
            Log.i(TAG, "Test ${getTestName(description)} was not ignored")
            val eachNotifier = EachTestNotifier(notifier, description).apply { fireTestStarted() }
            executeSafely(eachNotifier) {
                Log.i(TAG, "Preparing ${getTestName(description)}")
                testExecutorDelegate.prepare(method, testParameters, description)
                Log.i(TAG, "Evaluating ${getTestName(description)}")
                statement.evaluate()
            }
            Log.i(TAG, "Test ${getTestName(description)} is finished")
            eachNotifier.fireTestFinished()
        }
        notifier.removeCustomListeners()
    }

    private fun executeSafely(notifier: EachTestNotifier, action: () -> Unit) {
        try {
            action()
        } catch (e: AssumptionViolatedException) {
            Log.e(TAG, e.message, e)
            notifier.addFailedAssumption(e)
        } catch (e: Throwable) {
            Log.e(TAG, e.message, e)
            notifier.addFailure(e)
        }
    }

    private fun RunNotifier.addCustomListeners() = testInfoRunListeners.forEach { addListener(it) }

    private fun RunNotifier.removeCustomListeners() = testInfoRunListeners.forEach { removeListener(it) }

    private fun getSystemIntProperty(key: String): Int {
        return System.getProperty(key, "0").toInt()
    }

    private fun getTestName(description: Description) = "${description.className}.${description.methodName}"

    interface Factory {
        fun createTestExecutor(testParameters: List<Any>): TestExecutor
    }

    companion object {
        private val TAG = TestExecutor::class.java.simpleName
    }
}
