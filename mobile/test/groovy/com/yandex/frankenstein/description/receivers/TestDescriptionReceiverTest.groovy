package com.yandex.frankenstein.description.receivers

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestDescriptionReceiverTest {

    final File mTestInfoFile = File.createTempFile("temp", "temp")

    @Test
    void testAccept() {
        final TestDescriptionReceiver receiver = new TestDescriptionReceiver(mTestInfoFile)
        receiver.accept("FRANKENSTEIN AUTOTEST: some info")
        assertThat(mTestInfoFile.text).isEqualTo("some info\n")
    }

    @Test
    void testAcceptIfNoTag() {
        final TestDescriptionReceiver receiver = new TestDescriptionReceiver(mTestInfoFile)
        receiver.accept("wrong tag: some info")
        assertThat(mTestInfoFile.text).isEmpty()
    }
}
