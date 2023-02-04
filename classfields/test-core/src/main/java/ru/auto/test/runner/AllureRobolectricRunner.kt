package ru.auto.test.runner

import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.AllureLifecycle
import io.qameta.allure.kotlin.junit4.AllureJunit4
import io.qameta.allure.kotlin.junit4.DisplayName
import io.qameta.allure.kotlin.model.Label
import io.qameta.allure.kotlin.model.Link
import io.qameta.allure.kotlin.model.Status
import io.qameta.allure.kotlin.model.StatusDetails
import io.qameta.allure.kotlin.model.TestResult
import io.qameta.allure.kotlin.util.AnnotationUtils
import io.qameta.allure.kotlin.util.ResultsUtils
import org.junit.Ignore
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.FrameworkMethod
import org.robolectric.RobolectricTestRunner
import org.robolectric.internal.bytecode.Sandbox
import java.lang.reflect.Method
import java.util.*

class AllureRobolectricRunner(clazz: Class<*>) : RobolectricTestRunner(clazz) {
    private val listener = AllureRunListener()
    private var currentCase: String? = null

    override fun run(notifier: RunNotifier) {
        notifier.addListener(listener)
        super.run(notifier)
    }

    override fun beforeTest(sandbox: Sandbox, method: FrameworkMethod, bootstrappedMethod: Method) {
        @Suppress("TooGenericExceptionCaught") // we need to catch them all
        try {
            super.beforeTest(sandbox, method, bootstrappedMethod)
        } catch (e: Exception) {
            listener.robolectricTestStarted(method.description())
            listener.testFailure(Failure(method.description(), e))
            listener.robolectricTestFinished()
            throw e
        }
        listener.robolectricTestStarted(method.description())
        currentCase = listener.lifecycle.getCurrentTestCase()
    }


    override fun afterTest(method: FrameworkMethod, bootstrappedMethod: Method) {
        super.afterTest(method, bootstrappedMethod)
        listener.robolectricTestFinished()
        currentCase = null
    }

    private fun FrameworkMethod.description() = Description.createTestDescription(
        testClass.javaClass,
        testName(this), *annotations
    )

    /**
     * Copied from [AllureJunit4]. Modified to share currentCase from parent class.
     * Junit synchronizes calls to listener by default, so no synchronization is needed.
     *
     */
    private inner class AllureRunListener @JvmOverloads constructor(val lifecycle: AllureLifecycle = Allure.lifecycle) :
        RunListener() {

        override fun testRunStarted(description: Description) { //do nothing
        }

        override fun testRunFinished(result: Result) { //do nothing
        }

        override fun testStarted(description: Description?) {
            // do nothing. this should be handled from robolectric thread
        }

        override fun testFinished(description: Description?) {
            // only trash current case. reporting is done from robolectric thread
            currentCase = null
        }

        fun robolectricTestStarted(description: Description) {
            val uuid = UUID.randomUUID().toString()
            currentCase = uuid
            val result = createTestResult(uuid, description)
            lifecycle.scheduleTestCase(result)
            lifecycle.startTestCase(uuid)
        }

        fun robolectricTestFinished() {
            val uuid = checkNotNull(currentCase)
            lifecycle.updateTestCase(uuid) { testResult: TestResult ->
                if (testResult.status == null) {
                    testResult.status = Status.PASSED
                }
            }
            lifecycle.stopTestCase(uuid)
            lifecycle.writeTestCase(uuid)
        }

        override fun testFailure(failure: Failure) {
            val uuid = checkNotNull(currentCase)
            lifecycle.updateTestCase(uuid) { testResult: TestResult ->
                with(testResult) {
                    status = ResultsUtils.getStatus(failure.exception)
                    statusDetails = ResultsUtils.getStatusDetails(failure.exception)
                }
            }
        }

        override fun testAssumptionFailure(failure: Failure) {
            val uuid = checkNotNull(currentCase)
            lifecycle.updateTestCase(uuid) { testResult: TestResult ->
                with(testResult) {
                    status = Status.SKIPPED
                    statusDetails = ResultsUtils.getStatusDetails(failure.exception)
                }
            }
        }

        override fun testIgnored(description: Description) {
            val uuid = UUID.randomUUID().toString()

            val result = createTestResult(uuid, description).apply {
                status = Status.SKIPPED
                statusDetails = getIgnoredMessage(description)
                start = System.currentTimeMillis()
            }
            lifecycle.scheduleTestCase(result)
            lifecycle.stopTestCase(uuid)
            lifecycle.writeTestCase(uuid)
        }

        private fun getDisplayName(result: Description): String? = result.getAnnotation(DisplayName::class.java)?.value

        private fun getDescription(result: Description): String? =
            result.getAnnotation(io.qameta.allure.kotlin.Description::class.java)?.value

        private fun extractLinks(description: Description): List<Link> {
            val result = ArrayList(AnnotationUtils.getLinks(description.annotations))
            description.testClass
                ?.let(AnnotationUtils::getLinks)
                ?.let(result::addAll)
            return result
        }

        private fun extractLabels(description: Description): List<Label> {
            val result = ArrayList(AnnotationUtils.getLabels(description.annotations))
            description.testClass
                ?.let(AnnotationUtils::getLabels)
                ?.let(result::addAll)
            return result
        }

        private fun getHistoryId(description: Description): String =
            ResultsUtils.md5(description.className + description.methodName)

        private fun getPackage(testClass: Class<*>?): String = testClass?.getPackage()?.name ?: ""

        private fun getIgnoredMessage(description: Description): StatusDetails {
            val ignore: Ignore? = description.getAnnotation(Ignore::class.java)
            val message = ignore?.value?.takeIf { it.isNotEmpty() } ?: "Test ignored (without reason)!"
            return StatusDetails(message = message)
        }

        private fun createTestResult(uuid: String, description: Description): TestResult {
            val className: String = description.className
            val methodName: String? = description.methodName
            val name = methodName ?: className
            val fullName = if (methodName != null) "$className.$methodName" else className
            val suite: String = description.testClass?.getAnnotation(DisplayName::class.java)?.value ?: className
            val testResult: TestResult = TestResult(uuid).apply {
                this.historyId = getHistoryId(description)
                this.fullName = fullName
                this.name = name
            }
            testResult.labels.addAll(ResultsUtils.getProvidedLabels())
            testResult.labels.addAll(
                listOf(
                    ResultsUtils.createPackageLabel(getPackage(description.testClass)),
                    ResultsUtils.createTestClassLabel(className),
                    ResultsUtils.createTestMethodLabel(name),
                    ResultsUtils.createSuiteLabel(suite),
                    ResultsUtils.createHostLabel(),
                    ResultsUtils.createThreadLabel(),
                    ResultsUtils.createFrameworkLabel("junit4"),
                    ResultsUtils.createLanguageLabel("kotlin")
                )
            )
            testResult.labels.addAll(extractLabels(description))
            testResult.links.addAll(extractLinks(description))
            getDisplayName(description)?.let {
                testResult.name = it
            }
            getDescription(description)?.let {
                testResult.description = it
            }
            return testResult
        }

    }
}
