package com.yandex.frankenstein.results.reporters.attachments

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class ScreenshotAttachmentProviderTest {

    final File screenshotDir = File.createTempDir()
    final String[] branches = ["branch1", "branch2"]
    final String testClass = "SomeTestClass"
    final String testName = "SomeTestName"

    @Test
    void testGetAttachments() {
        final List<File> screenshotFiles = []
        branches.each {
            final File screenshotBranchDir = new File(screenshotDir, it)
            screenshotBranchDir.mkdir()
            final File screenshotFile = new File(screenshotBranchDir,
                    ScreenshotAttachmentProvider.getScreenshotFileName(testClass, testName))
            screenshotFile.createNewFile()
            screenshotFiles.add(screenshotFile)
        }
        final ScreenshotAttachmentProvider provider = new ScreenshotAttachmentProvider(screenshotDir, branches)
        final List<Attachment> allureAttachmentList = provider.getAttachments(testClass, testName)

        for (int i = 0; i < branches.size(); ++i) {
            assertThat(allureAttachmentList[i].toMap()).isEqualTo(
                    new Attachment(branches[i] + ' screenshot image', screenshotFiles[i].absolutePath).toMap())
        }
    }

    @Test
    void testGetScreenshotFileName() {
        final String fileName = ScreenshotAttachmentProvider.getScreenshotFileName(testClass, testName)
        final String expected = "${testClass}_${testName.replaceAll(' ', '')}.png".toString()
        assertThat(fileName).isEqualTo(expected)
    }

    @Test
    void testGetScreenshotFileNameIfHasSpaces() {
        final String fileName = ScreenshotAttachmentProvider.getScreenshotFileName(testClass, "Some Test Name")
        final String expected = "${testClass}_SomeTestName.png".toString()
        assertThat(fileName).isEqualTo(expected)
    }

    @Test
    void testGetAttachmentsWithoutFiles() {
        final ScreenshotAttachmentProvider provider = new ScreenshotAttachmentProvider(screenshotDir, branches)
        final List<Attachment> allureAttachmentList = provider.getAttachments(testClass, testName)
        assertThat(allureAttachmentList).isEmpty()
    }

    @Test
    void testGetAttachmentsWithoutBranches() {
        final ScreenshotAttachmentProvider provider = new ScreenshotAttachmentProvider(screenshotDir)
        final List<Attachment> allureAttachmentList = provider.getAttachments(testClass, testName)
        assertThat(allureAttachmentList).isEmpty()
    }
}
