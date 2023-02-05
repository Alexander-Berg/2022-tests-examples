package com.yandex.frankenstein.description.receivers

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class IgnoredTestDescriptionReceiverTest {

    final File mTestInfoFile = File.createTempFile("temp", "temp")

    @Test
    void testAccept() {
        final IgnoredTestDescriptionReceiver receiver = new IgnoredTestDescriptionReceiver(mTestInfoFile)
        receiver.accept("FRANKENSTEIN IGNORED AUTOTEST: some info")
        assertThat(mTestInfoFile.text).isEqualTo("some info\n")
    }

    @Test
    void testAcceptIfNoTag() {
        final IgnoredTestDescriptionReceiver receiver = new IgnoredTestDescriptionReceiver(mTestInfoFile)
        receiver.accept("wrong tag: some info")
        assertThat(mTestInfoFile.text).isEmpty()
    }
}
