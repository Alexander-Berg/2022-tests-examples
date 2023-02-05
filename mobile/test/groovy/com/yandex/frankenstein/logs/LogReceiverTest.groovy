package com.yandex.frankenstein.logs

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class LogReceiverTest {

    final File tmpDir = File.createTempDir()
    final String prefix = "fileName"
    final String message = "string"

    @Test
    void testAccept() {
        final LogReceiver receiver = new LogReceiver(tmpDir, prefix)
        receiver.accept(message)

        String fileText = (new File(tmpDir, LogReceiver.composeFilename(prefix))).text
        assertThat(fileText).isEqualTo(message)
    }

    @Test
    void testAcceptIfMessageIsNull() {
        final LogReceiver receiver = new LogReceiver(tmpDir, prefix)
        receiver.accept(null)

        String fileText = (new File(tmpDir, LogReceiver.composeFilename(prefix))).text
        assertThat(fileText).isEqualTo("null")
    }

    @Test
    void testAcceptIfPrefixIsNull() {
        final LogReceiver receiver = new LogReceiver(tmpDir, null)
        receiver.accept(message)

        String fileText = (new File(tmpDir, LogReceiver.composeFilename(null))).text
        assertThat(fileText).isEqualTo(message)
    }

    @Test
    void testComposeFileName() {
        final String fileName = LogReceiver.composeFilename(prefix)
        assertThat(fileName).isEqualTo(prefix + ".log")
    }

    @Test
    void testComposeFileNameIfPrefixIsNull() {
        final String fileName = LogReceiver.composeFilename(null)
        assertThat(fileName).isEqualTo("null.log")
    }

    @Test
    void testComposeFileNameIfPrefixIsEmpty() {
        final String fileName = LogReceiver.composeFilename("")
        assertThat(fileName).isEqualTo(".log")
    }

    @Test
    void testGetOutputFile() {
        final File tempDir = File.createTempDir()
        final String dir = "/some_extra_path"
        final String filename = "some_filename"
        final File outputFile = LogReceiver.getOutputFile(new File(tempDir, dir), filename)
        assertThat(outputFile.exists()).isEqualTo(true)
        assertThat(outputFile.absolutePath).isEqualTo(tempDir.absolutePath + dir + "/" + LogReceiver.composeFilename(filename))
        assertThat(outputFile.text).isEmpty()
    }

    @Test
    void testGetOutputFileIfFileExists() {
        final File tempDir = File.createTempDir()
        final String filename = "some_filename"
        final File logsFile = new File(tempDir, filename + ".log")
        logsFile.write("some string")
        final File outputFile = LogReceiver.getOutputFile(tempDir, filename)
        assertThat(outputFile.exists()).isEqualTo(true)
        assertThat(outputFile.absolutePath).isEqualTo(logsFile.absolutePath)
        assertThat(outputFile.text).isEmpty()
    }
}
