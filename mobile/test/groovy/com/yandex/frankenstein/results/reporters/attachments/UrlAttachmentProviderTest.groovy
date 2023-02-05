package com.yandex.frankenstein.results.reporters.attachments


import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class UrlAttachmentProviderTest {

    final String urlBase = "sandbox_url_base"
    final String testClass = "SomeTestClass"
    final String testName = "SomeTestName"
    final String testNameWithWhitespaces = "SomeTestName[param = 42]"
    final String logFileName = "${testClass}.${testName}.log.zip"
    final String logFileNameWithWhitespaces = "${testClass}.SomeTestName%5Bparam%20%3D%2042%5D.log.zip"

    @Test
    void testGetAttachments() {
        final UrlAttachmentProvider provider = new UrlAttachmentProvider(urlBase)
        final List<Attachment> allureAttachmentList = provider.getAttachments(testClass, testName)
        assertThat(allureAttachmentList*.toMap()).containsExactlyInAnyOrder(
                new Attachment('device log', "$urlBase/logs/device/$logFileName").toMap(),
                new Attachment('runner log', "$urlBase/logs/runner/$logFileName").toMap()
        )
    }

    @Test
    void testGetAttachmentsWithoutFiles() {
        final UrlAttachmentProvider provider = new UrlAttachmentProvider(urlBase)
        final List<Attachment> allureAttachmentList = provider.getAttachments(testClass, testName)
        assertThat(allureAttachmentList*.toMap()).containsExactlyInAnyOrder(
                new Attachment('device log', "$urlBase/logs/device/$logFileName").toMap(),
                new Attachment('runner log', "$urlBase/logs/runner/$logFileName").toMap()
        )
    }

    @Test
    void testGetAttachmentsIfHasWhitespaces() {
        final UrlAttachmentProvider provider = new UrlAttachmentProvider(urlBase)
        final List<Attachment> allureAttachmentList = provider.getAttachments(testClass, testNameWithWhitespaces)
        assertThat(allureAttachmentList*.toMap()).containsExactlyInAnyOrder(
                new Attachment('device log', "$urlBase/logs/device/$logFileNameWithWhitespaces").toMap(),
                new Attachment('runner log', "$urlBase/logs/runner/$logFileNameWithWhitespaces").toMap()
        )
    }
}
