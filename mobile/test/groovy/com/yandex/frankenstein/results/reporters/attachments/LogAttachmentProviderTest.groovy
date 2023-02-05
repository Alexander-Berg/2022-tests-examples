package com.yandex.frankenstein.results.reporters.attachments

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class LogAttachmentProviderTest {

    final File deviceLogsDir = File.createTempDir()
    final File runnerLogsDir = File.createTempDir()
    final String testClass = "SomeTestClass"
    final String testName = "SomeTestName"
    final String logFileName = "${testClass}.${testName}.log.zip"

    @Test
    void testGetAttachments() {
        final File deviceLogFile = new File(deviceLogsDir, logFileName)
        deviceLogFile.createNewFile()
        final File runnerLogFile = new File(runnerLogsDir, logFileName)
        runnerLogFile.createNewFile()
        final LogAttachmentProvider logAttachmentProvider = new LogAttachmentProvider(deviceLogsDir, runnerLogsDir)
        final List<Attachment> allureAttachmentList = logAttachmentProvider.getAttachments(testClass, testName)
        assertThat(allureAttachmentList*.toMap()).containsExactlyInAnyOrder(
                new Attachment('device log', deviceLogFile.absolutePath).toMap(),
                new Attachment('runner log', runnerLogFile.absolutePath).toMap()
        )
    }

    @Test
    void testGetAttachmentsWithoutFiles() {
        final LogAttachmentProvider logAttachmentProvider = new LogAttachmentProvider(deviceLogsDir, runnerLogsDir)
        final List<Attachment> allureAttachmentList = logAttachmentProvider.getAttachments(testClass, testName)
        assertThat(allureAttachmentList).isEmpty()
    }
}
