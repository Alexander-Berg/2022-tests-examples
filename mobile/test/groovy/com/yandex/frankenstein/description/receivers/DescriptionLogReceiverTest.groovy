package com.yandex.frankenstein.description.receivers

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class DescriptionLogReceiverTest {

    @Test
    void testTest() {
        final DescriptionLogReceiver logReceiver = new DescriptionLogReceiver("TAG")
        assertThat(logReceiver.test("TAG: some message")).isTrue()
    }

    @Test
    void testTestIfNoTag() {
        final DescriptionLogReceiver logReceiver = new DescriptionLogReceiver("TAG")
        assertThat(logReceiver.test("wrong: some message")).isFalse()
    }

    @Test
    void testApply() {
        final DescriptionLogReceiver logReceiver = new DescriptionLogReceiver("TAG")
        assertThat(logReceiver.apply("TAG: some message")).isEqualTo("some message\n")
    }

    @Test
    void testApplyIfNoTag() {
        final DescriptionLogReceiver logReceiver = new DescriptionLogReceiver("TAG")
        assertThat(logReceiver.apply("wrong: some message")).isEmpty()
    }
}
