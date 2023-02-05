package ru.yandex.market.test.runner

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.internal.util.AndroidRunnerParams
import androidx.test.platform.app.InstrumentationRegistry
import com.athaydes.javanna.Javanna
import io.qameta.allure.android.annotations.DisplayName
import io.qameta.allure.android.annotations.Link
import org.junit.runner.Description.createTestDescription
import org.junit.runner.notification.RunNotifier
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import ru.yandex.market.test.TestCase
import ru.yandex.market.test.allure.TestCaseId

class TestCaseJUnit4ClassRunner(testClass: Class<out TestCase>) : AndroidJUnit4ClassRunner(
    testClass, AndroidRunnerParams(
        InstrumentationRegistry.getInstrumentation(),
        InstrumentationRegistry.getArguments(),
        0,
        false
    )
) {
    private val testCase by lazy { testClass.newInstance() }

    override fun runChild(method: FrameworkMethod?, notifier: RunNotifier?) {

        val displayName: DisplayName =
            Javanna.createAnnotation(
                DisplayName::class.java,
                mapOf("value" to testCase.description)
            )
        val testCaseId = testCase.javaClass.getAnnotation(TestCaseId::class.java)?.value ?: 0
        val testCaseLink: Link =
            Javanna.createAnnotation(
                Link::class.java,
                mapOf(
                    "name" to "TestCase #$testCaseId",
                    "value" to "TestCase #$testCaseId",
                    "url" to "https://testpalm.yandex-team.ru/testcase/bluemarketapps-$testCaseId",
                    "type" to "tms"
                )
            )

        val description = createTestDescription(
            testClass.javaClass,
            method?.name,
            displayName,
            testCaseLink
        )
        if (isIgnored(method)) {
            notifier?.fireTestIgnored(description)
        } else {
            runLeaf(methodBlock(method), description, notifier)
        }
    }

    override fun methodInvoker(method: FrameworkMethod?, test: Any?) = object : Statement() {

        override fun evaluate() {
            testCase.test()
        }
    }
}