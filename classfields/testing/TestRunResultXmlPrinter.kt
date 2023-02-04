package com.yandex.mobile.realty.testing

import com.android.SdkConstants
import com.android.ddmlib.Log
import com.android.ddmlib.testrunner.TestIdentifier
import com.android.ddmlib.testrunner.TestResult
import com.android.ddmlib.testrunner.TestRunResult
import org.kxml2.io.KXmlSerializer
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class that prints [com.android.ddmlib.testrunner.TestRunResult] to Ant-style xml report format
 *
 * Implementation is borrowed from [com.android.ddmlib.testrunner.XmlTestRunListener]
 */
@Suppress("StringLiteralDuplication")
class TestRunResultXmlPrinter(
    private var mRunResult: TestRunResult,
    private var mReportFile: File,
) {

    companion object {
        private const val LOG_TAG = "XmlResultReporter"

        private const val TESTSUITE = "testsuite"
        private const val TESTCASE = "testcase"
        private const val FAILURE = "failure"
        private const val SKIPPED_TAG = "skipped"
        private const val ATTR_NAME = "name"
        private const val ATTR_TIME = "time"
        private const val ATTR_ERRORS = "errors"
        private const val ATTR_FAILURES = "failures"
        private const val ATTR_SKIPPED = "skipped"
        private const val ATTR_TESTS = "tests"

        private const val ATTR_CLASSNAME = "classname"
        private const val TIMESTAMP = "timestamp"
        private const val HOSTNAME = "hostname"
    }

    /** the XML namespace  */
    private val ns: String? = null

    private var mHostName = "localhost"

    /*
     * Creates a report file and populates it with the report data from the completed tests.
     */
    fun generateDocument() {
        val timestamp = getTimestamp()
        var stream: OutputStream? = null
        try {
            stream = BufferedOutputStream(FileOutputStream(mReportFile))
            val serializer = KXmlSerializer()
            serializer.setOutput(stream, SdkConstants.UTF_8)
            serializer.startDocument(SdkConstants.UTF_8, null)
            serializer.setFeature(
                "http://xmlpull.org/v1/doc/features.html#indent-output", true)
            printTestResults(serializer, timestamp)
            serializer.endDocument()
            val msg = String.format("XML test result file generated at %s. %s",
                mReportFile.absolutePath, mRunResult.textSummary)
            Log.logAndDisplay(Log.LogLevel.INFO, LOG_TAG, msg)
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Failed to generate report data")
        } finally {
            if (stream != null) {
                try {
                    stream.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    /**
     * Return the current timestamp as a [String].
     */
    private fun getTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault())
        val gmt = TimeZone.getTimeZone("UTC")
        dateFormat.timeZone = gmt
        dateFormat.isLenient = true
        return dateFormat.format(Date())
    }

    @Throws(IOException::class)
    private fun printTestResults(serializer: KXmlSerializer, timestamp: String?) {
        serializer.startTag(ns, TESTSUITE)
        val name = mRunResult.name
        if (name != null) {
            serializer.attribute(ns, ATTR_NAME, name)
        }
        serializer.attribute(ns, ATTR_TESTS, Integer.toString(mRunResult.numTests))
        serializer.attribute(ns, ATTR_FAILURES, Integer.toString(
            mRunResult.numAllFailedTests))
        // legacy - there are no errors in JUnit4
        serializer.attribute(ns, ATTR_ERRORS, "0")
        serializer.attribute(ns, ATTR_SKIPPED, Integer.toString(mRunResult.getNumTestsInState(
            TestResult.TestStatus.IGNORED)))
        serializer.attribute(ns, ATTR_TIME, java.lang.Double.toString(mRunResult.elapsedTime.toDouble() / 1000f))
        serializer.attribute(ns, TIMESTAMP, timestamp)
        serializer.attribute(ns, HOSTNAME, mHostName)
        val testResults = mRunResult.testResults
        for ((key, value) in testResults) {
            print(serializer, key, value)
        }
        serializer.endTag(ns, TESTSUITE)
    }

    @Throws(IOException::class)
    private fun print(serializer: KXmlSerializer, testId: TestIdentifier, testResult: TestResult) {
        serializer.startTag(ns, TESTCASE)
        serializer.attribute(ns, ATTR_NAME, testId.testName)
        serializer.attribute(ns, ATTR_CLASSNAME, testId.className)
        val elapsedTimeMs = testResult.endTime - testResult.startTime
        serializer.attribute(ns, ATTR_TIME, java.lang.Double.toString(elapsedTimeMs.toDouble() / 1000f))
        when (testResult.status) {
            TestResult.TestStatus.FAILURE -> printFailedTest(serializer, FAILURE, testResult.stackTrace)
            TestResult.TestStatus.ASSUMPTION_FAILURE -> printFailedTest(serializer, SKIPPED_TAG, testResult.stackTrace)
            TestResult.TestStatus.IGNORED -> {
                serializer.startTag(ns, SKIPPED_TAG)
                serializer.endTag(ns, SKIPPED_TAG)
            }
        }
        serializer.endTag(ns, TESTCASE)
    }

    @Throws(IOException::class)
    private fun printFailedTest(serializer: KXmlSerializer, tag: String, stack: String) {
        serializer.startTag(ns, tag)
        serializer.text(sanitize(stack))
        serializer.endTag(ns, tag)
    }

    /**
     * Returns the text in a format that is safe for use in an XML document.
     */
    private fun sanitize(text: String): String = text.replace("\u0000", "<\\0>")
}
