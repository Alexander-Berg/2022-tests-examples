package com.yandex.mail.testopithecus

import android.util.Log
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.junit4.AllureJunit4
import io.qameta.allure.kotlin.model.Status
import io.qameta.allure.kotlin.util.ResultsUtils
import org.junit.Assume
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.io.File

class RetryTestRule(private val retryCount: Int = 3) : TestRule {
    private val TAG = RetryTestRule::class.java.simpleName

    override fun apply(base: Statement, description: Description): Statement {
        return statement(base, description)
    }

    private fun statement(base: Statement, description: Description): Statement {
        return object : Statement() {
            @Throws(Throwable::class)
            override fun evaluate() {
                var caughtThrowable: Throwable? = null
                Log.e(TAG, description.displayName + ": starting with retry rule")
                var testName = description.methodName.toString()
                testName = testName.substring(testName.indexOf(":") + 2, testName.length - 1)
                var isPassed = false

                for (i in 0 until retryCount) {
                    try {
                        if (i > 0) {
                            Log.e(TAG, "Starting again")
                            AllureJunit4().testStarted(description)
                        }
                        base.evaluate()
                        isPassed = true
                        return
                    } catch (t: Throwable) {
                        if ((!isTestAlreadyPassed(testName) && !isTestAlreadyFailed(testName)) || isTestAlreadyPassed(testName)) {
                            Assume.assumeNoException(t)
                        }
                        caughtThrowable = t
                        Log.e(TAG, description.displayName + ": run ${i + 1} failed")
                        Log.e(TAG, "Caught an error: $t")
                        Allure.lifecycle.updateTestCase {
                            it.status = ResultsUtils.getStatus(t)
                            Log.e(TAG, ResultsUtils.getStatus(t).toString())
                            it.statusDetails = ResultsUtils.getStatusDetails(t)
                            Log.e(TAG, ResultsUtils.getStatusDetails(t).toString())
                        }
                    } finally {
                        val uid = Allure.lifecycle.getCurrentTestCase()
                        val isAlreadyPassed = isTestAlreadyPassed(testName)
                        val isAlreadyFailed = isTestAlreadyFailed(testName)

                        Log.e(TAG, "Current test uid: $uid")
                        if (uid !== null) {
                            Allure.lifecycle.updateTestCase {
                                val e = Log.e(TAG, "Current test status: ${it.status}")
                                when {
                                    it.status === null -> {
                                        it.status = Status.PASSED
                                    }
                                    (!isAlreadyPassed && !isAlreadyFailed) -> {
                                        it.status = Status.SKIPPED
                                    }
                                }
                            }
                            Allure.lifecycle.stopTestCase(uid)
                            Allure.lifecycle.writeTestCase(uid)
                        }
                        when {
                            isPassed -> {
                                val fileTestPassed = File("/sdcard/allure-results/test-passed.txt")
                                if (!fileTestPassed.exists()) {
                                    fileTestPassed.createNewFile()
                                }
                                fileTestPassed.appendText("$testName\n")
                                Log.e(TAG, "Test '$testName' was written in TestPassed file")
                            }
                            isAlreadyPassed -> {
                                Log.e(TAG, "Test '$testName' was written in TestPassed file early, this is empty retry")
                                deleteResult(uid.toString())
                            }
                            !isAlreadyFailed -> {
                                val fileTestFailed = File("/sdcard/allure-results/test-failed.txt")
                                if (!fileTestFailed.exists()) {
                                    fileTestFailed.createNewFile()
                                }
                                fileTestFailed.appendText("$testName\n")
                                Log.e(TAG, "Test '$testName' was written in TestFailed file")
                                deleteResult(uid.toString())
                            }
                        }
                    }
                }
                Log.e(TAG, description.displayName + ": giving up after $retryCount failures")
                throw caughtThrowable!!
            }
        }
    }

    private fun deleteResult(uid: String) {
        val fileResult = File("/sdcard/allure-results/" + uid + "-result.json")
        if (fileResult.exists()) {
            fileResult.delete()
        }
    }
}
