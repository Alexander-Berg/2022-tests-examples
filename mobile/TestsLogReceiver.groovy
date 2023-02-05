package com.yandex.frankenstein.logs

import com.yandex.frankenstein.utils.FileUtils
import groovy.transform.CompileStatic
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestDescriptor

import java.util.function.Consumer

@CompileStatic
class TestsLogReceiver {

    private final Map<TestDescriptor, Consumer<String>> mLogReceivers
    private final File mLogsDir
    private final Logger mLogger

    TestsLogReceiver(final File logsDir, final Logger logger) {
        mLogsDir = logsDir
        mLogReceivers = [:]
        mLogger = logger
    }

    void accept(final TestDescriptor descriptor, final String message) {
        final Consumer<String> logReceiver = getLogReceiver(descriptor)
        logReceiver.accept(message)
    }

    Consumer<String> getLogReceiver(final TestDescriptor descriptor) {
        return mLogReceivers.computeIfAbsent(descriptor) { final TestDescriptor testDescriptor ->
            final String logFilename = getLogFilename(descriptor)
            new LogReceiver(mLogsDir, logFilename)
        }
    }

    void zipAndDeleteLogFile(final TestDescriptor descriptor) {
        final String logFilename = getLogFilename(descriptor)
        final File logFile = new File(mLogsDir, "${logFilename}.log")

        try {
            FileUtils.archiveFileToZip(logFile)
            logFile.delete()
        } catch (IOException e) {
            mLogger.warn("Failed to zip log file ${logFilename}", e)
        }
    }

    static String getLogFilename(final TestDescriptor descriptor) {
        return "$descriptor.className.$descriptor.name"
    }
}
