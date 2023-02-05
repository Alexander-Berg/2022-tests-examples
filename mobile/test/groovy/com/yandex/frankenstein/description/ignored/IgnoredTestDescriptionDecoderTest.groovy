package com.yandex.frankenstein.description.ignored

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class IgnoredTestDescriptionDecoderTest {

    @Test
    void testDecode() {
        final String info = "{\"reason\":\"Suite filter\",\"case id\":723}"
        final IgnoredTestDescriptionDecoder ignoredTestDescriptionDecoder = new IgnoredTestDescriptionDecoder()
        final IgnoredTestDescription ignoredTestDescription = ignoredTestDescriptionDecoder.decode(info)

        assertThat(ignoredTestDescription.testCaseId).isEqualTo(723)
        assertThat(ignoredTestDescription.reason).isEqualTo("Suite filter")
    }
}
