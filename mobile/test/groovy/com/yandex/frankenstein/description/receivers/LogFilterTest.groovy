package com.yandex.frankenstein.description.receivers

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class LogFilterTest {

    @Test
    void testTest() {
        final LogFilter logFilter = new LogFilter("TAG")
        assertThat(logFilter.test("TAG: some info")).isTrue()
    }

    @Test
    void testTestIfNoTag() {
        final LogFilter logFilter = new LogFilter("TAG")
        assertThat(logFilter.test("wrong: some info")).isFalse()
    }
}
