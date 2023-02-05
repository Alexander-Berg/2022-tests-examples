package com.yandex.frankenstein.logs

import com.yandex.frankenstein.utils.FileUtils
import org.gradle.api.internal.tasks.testing.DefaultTestDescriptor
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.testing.TestDescriptor
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestsLogReceiverTest {

    private final static String MESSAGE = "string"

    private final File mTmpDir = File.createTempDir()
    private final Logger mLogger = [:] as Logger
    private final TestsLogReceiver mReceiver = new TestsLogReceiver(mTmpDir, mLogger)
    private final TestDescriptor mDescriptor = new DefaultTestDescriptor(new Object(), "className", "name")
    private final File mLogFile = new File(mTmpDir, LogReceiver.composeFilename(TestsLogReceiver.getLogFilename(mDescriptor)))

    @Test
    void testAccept() {
        mReceiver.accept(mDescriptor, MESSAGE)

        assertThat(mLogFile.text).isEqualTo(MESSAGE)
    }

    @Test
    void testAcceptIfMessageIsNull() {
        mReceiver.accept(mDescriptor, null)

        assertThat(mLogFile.text).isEqualTo("null")
    }

    @Test
    void testGetLogFilename() {
        final String logFilename = TestsLogReceiver.getLogFilename(mDescriptor)
        assertThat(logFilename).isEqualTo(mDescriptor.className + "." + mDescriptor.name)
    }

    @Test
    void testAcceptTwice() {
        mReceiver.accept(mDescriptor, MESSAGE)
        mReceiver.accept(mDescriptor, MESSAGE)

        assertThat(mLogFile.text).isEqualTo(MESSAGE + MESSAGE)
    }

    @Test
    void testZipFileAndDeleteZipFileCreated() {
        final File zipLogFile  = new File(mTmpDir, "${mLogFile.name}.zip")
        mReceiver.accept(mDescriptor, MESSAGE)

        mReceiver.zipAndDeleteLogFile(mDescriptor)

        assertThat(mLogFile).doesNotExist()
        assertThat(zipLogFile).exists()
    }

    @Test
    void testZipFileAndDeleteZipFileContents() {
        final TestsLogReceiver receiver = new TestsLogReceiver(mTmpDir, mLogger)
        final File zipLogFile  = new File(mTmpDir, "${mLogFile.name}.zip")
        receiver.accept(mDescriptor, MESSAGE)

        receiver.zipAndDeleteLogFile(mDescriptor)
        FileUtils.unarchiveZip(zipLogFile, mTmpDir)

        assertThat(mLogFile.text).isEqualTo(MESSAGE)
    }
}
