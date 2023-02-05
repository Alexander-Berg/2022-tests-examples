package ru.yandex.market.test.util

import android.app.Instrumentation
import android.os.Environment
import androidx.test.uiautomator.UiDevice
import org.junit.runner.notification.Failure
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

object FailedTestsDelegate {

    private val failedTests = mutableListOf<Failure>()

    fun testRunStarted(instrumentation: Instrumentation) {
        grantPermissions(instrumentation)
    }

    fun testFailure(failure: Failure?) {
        failure?.run { failedTests.add(this) }
    }

    fun testRunFinished() {
        try {
            val dir = File(Environment.getExternalStorageDirectory().absolutePath, "/failed")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, FAILED_TESTS_FILE_NAME)
            if (!file.exists()) {
                file.createNewFile()
            }
            val outputStreamWriter = OutputStreamWriter(FileOutputStream(file.absolutePath, true))
            failedTests.mapNotNull { it.description }.mapNotNull { it.testClass }.forEach {
                outputStreamWriter.write(it.name + "\n")
            }
            outputStreamWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun grantPermissions(instrumentation: Instrumentation) {
        with(UiDevice.getInstance(instrumentation)) {
            executeShellCommand("pm grant " + instrumentation.context.packageName + " android.permission.WRITE_EXTERNAL_STORAGE")
            executeShellCommand("pm grant " + instrumentation.targetContext.packageName + " android.permission.WRITE_EXTERNAL_STORAGE")
            executeShellCommand("pm grant " + instrumentation.context.packageName + " android.permission.READ_EXTERNAL_STORAGE")
            executeShellCommand("pm grant " + instrumentation.targetContext.packageName + " android.permission.READ_EXTERNAL_STORAGE")
        }
    }

    private const val FAILED_TESTS_FILE_NAME = "FailedTests.txt"
}