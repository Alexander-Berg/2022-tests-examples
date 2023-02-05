package com.yandex.frankenstein.results.reporters.attachments

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class AttachmentTest {

    final String name = 'allure_name'
    final String source = 'allure_source'

    @Test
    void testToMap() {
        final Attachment allureAttachment = new Attachment(name, source)
        final Map<String, String> allureAttachmentMap = [
                name: name,
                source: source
        ]
        assertThat(allureAttachment.toMap()).isEqualTo(allureAttachmentMap)
    }
}
