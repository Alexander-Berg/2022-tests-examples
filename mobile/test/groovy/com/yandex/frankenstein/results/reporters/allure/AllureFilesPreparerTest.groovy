package com.yandex.frankenstein.results.reporters.allure

import com.yandex.frankenstein.description.TestCaseDescription
import com.yandex.frankenstein.description.TestRunDescription
import com.yandex.frankenstein.description.ignored.IgnoredTestDescription
import com.yandex.frankenstein.properties.info.BuildInfo
import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.results.reporters.allure.AllureFilesPreparer
import com.yandex.frankenstein.results.reporters.attachments.Attachment
import com.yandex.frankenstein.results.reporters.attachments.AttachmentProvider
import groovy.json.JsonBuilder
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class AllureFilesPreparerTest {

    final BuildInfo mBuildInfo = new BuildInfo("test.com", "1", "testBuildName", "reportsUrl", "schedulerId", "taskId")
    final TestPalmInfo mTestPalmInfo = new TestPalmInfo("appmetrica_project_id", "", "", "token", "appmetrica_base_url", "testpalm_ui_url", "")
    final AttachmentProvider firstAttachmentProvider = new AttachmentProvider() {
        @Override
        List<Attachment> getAttachments(final String testClass, final String testName) {
            return [
                    new Attachment(testClass + "_attachment_1", testName + "_attachment_1"),
                    new Attachment(testClass + "_attachment_2", testName + "_attachment_2"),
            ]
        }
    }
    final AttachmentProvider secondAttachmentProvider = new AttachmentProvider() {
        @Override
        List<Attachment> getAttachments(final String testClass, final String testName) {
            return [
                    new Attachment(testClass + "_attachment_3", testName + "_attachment_3"),
                    new Attachment(testClass + "_attachment_4", testName + "_attachment_4"),
            ]
        }
    }

    final AllureFilesPreparer allureFilesPreparer =
            new AllureFilesPreparer(mBuildInfo, mTestPalmInfo, firstAttachmentProvider, secondAttachmentProvider)

    @Test
    void testAddAttachmentsAndLinks() {
        final File tempDir = File.createTempDir()
        final File result = new File(tempDir, "1-result.json")
        result.write(new JsonBuilder([:]).toPrettyString())

        final AllureFilesPreparer allureFilesPreparer =
                new AllureFilesPreparer(mBuildInfo, mTestPalmInfo, firstAttachmentProvider, secondAttachmentProvider) {
                    protected void addInfo(final Map testResult,
                                           final TestRunDescription testRunDescription) {
                        testResult.info = "some_info"
                    }
                }
        allureFilesPreparer.addAttachmentsAndLinks(new TestRunDescription(), tempDir)
        assertThat(result.text).isEqualTo("""{
    "info": "some_info"
}""")
    }

    @Test
    void testAddAttachmentsAndLinksIfWrongName() {
        final File tempDir = File.createTempDir()
        final File result = new File(tempDir, "name.json")
        result.write(new JsonBuilder([:]).toPrettyString())

        final AllureFilesPreparer allureFilesPreparer =
                new AllureFilesPreparer(mBuildInfo, mTestPalmInfo, firstAttachmentProvider, secondAttachmentProvider) {
                    protected void addInfo(final Map testResult,
                                           final TestRunDescription testRunDescription) {
                        testResult.info = "some_info"
                    }
                }
        allureFilesPreparer.addAttachmentsAndLinks(new TestRunDescription(), tempDir)
        assertThat(result.text).isEqualTo("""{
    
}""")
    }

    @Test
    void testAddAttachmentsAndLinksIfWrongFormat() {
        final File tempDir = File.createTempDir()
        final File result = new File(tempDir, "1-result.txt")
        result.write(new JsonBuilder([:]).toPrettyString())

        final AllureFilesPreparer allureFilesPreparer =
                new AllureFilesPreparer(mBuildInfo, mTestPalmInfo, firstAttachmentProvider, secondAttachmentProvider) {
                    protected void addInfo(final Map testResult,
                                           final TestRunDescription testRunDescription) {
                        testResult.info = "some_info"
                    }
                }
        allureFilesPreparer.addAttachmentsAndLinks(new TestRunDescription(), tempDir)
        assertThat(result.text).isEqualTo("""{
    
}""")
    }

    @Test
    void testAddInfo() {
        final AllureFilesPreparer allureFilesPreparer =
                new AllureFilesPreparer(mBuildInfo, mTestPalmInfo, firstAttachmentProvider, secondAttachmentProvider) {
                    protected void setAttachments(final Map<String, ?> testResult,
                                                  final String testClass,
                                                  final String testName) {
                        testResult.attachments = "allureAttachments"
                    }

                    protected void setLinks(final Map<String, ?> testResult,
                                            final TestCaseDescription testCaseDescription) {
                        testResult.links = "links"
                    }

                    protected void setDescription(final Map<String, ?> testResult) {
                        testResult.description = "description"
                    }

                    protected void setMessage(final Map<String, ?> testResult,
                                              final IgnoredTestDescription ignoredTestDescription) {
                        testResult.message = "message"
                    }

                    protected void addLabels(final Map testResult,
                                             final TestCaseDescription testCaseDescription) {
                        testResult.labels = "labels"
                    }

                    protected void setSuite(final Map<String, ?> testResult) {
                        testResult.suite = "suite"
                    }

                    protected void setHistoryId(final Map<String, ?> testResult) {
                        testResult.historyId = "historyId"
                    }
                }
        final TestRunDescription testRunDescription = new TestRunDescription()
        final Map<String, ?> realTestResult = [
                "labels": [
                        [
                                "name": "testClass",
                                "value": "some_test_class",
                        ],
                        [
                                "name": "testMethod",
                                "value": "some_test_method",
                        ]
                ]
        ]
        allureFilesPreparer.addInfo(realTestResult, testRunDescription)
        assertThat(realTestResult).isEqualTo([
                "attachments": "allureAttachments",
                "description": "description",
                "labels": "labels",
                "links": "links",
                "message": "message",
                "suite": "suite",
                "historyId": "historyId",
        ])
    }

    @Test
    void testGenerateEnvironmentProperties() {
        final File tempDir = File.createTempDir()
        allureFilesPreparer.generateEnvironmentProperties(tempDir)
        final File settingsFile = new File(tempDir, "environment.properties")
        assertThat(settingsFile).exists()
        assertThat(settingsFile.text).contains(
                "Build\\ name=testBuildName",
                "Build\\ url=test.com",
                "Reports\\ URL=reportsUrl",
                "Os\\ api\\ level=1")
    }

    @Test
    void testGenerateCategoriesFile() {
        final File tempDir = File.createTempDir()
        allureFilesPreparer.generateCategoriesFile(tempDir)
        final File categoriesFile = new File(tempDir, "categories.json")
        assertThat(categoriesFile).hasContent("""[
    {
        "name": "Assumption failed",
        "traceRegex": ".*AssumptionViolatedException.*"
    },
    {
        "name": "Has bugs",
        "messageRegex": ".*has_bugs.*"
    }
]""")
    }

    @Test
    void testSetAttachments() {
        final Map<String, ?> testResult = [:]
        allureFilesPreparer.setAttachments(testResult, "test_class", "test_name")
        assertThat(testResult).isEqualTo([
                "attachments": [
                        [
                                "name": "test_class_attachment_1",
                                "source": "test_name_attachment_1"
                        ],
                        [
                                "name": "test_class_attachment_2",
                                "source": "test_name_attachment_2"
                        ],
                        [
                                "name": "test_class_attachment_3",
                                "source": "test_name_attachment_3"
                        ],
                        [
                                "name": "test_class_attachment_4",
                                "source": "test_name_attachment_4"
                        ],
                ]])
    }

    @Test
    void testSetLinks() {
        final Map<String, ?> testResult = [:]
        final TestCaseDescription testCaseDescription = new TestCaseDescription(42, null, null, null, [
                ["id": "1", "url": "issue/url/1"],
                ["id": "2", "url": "issue/url/2"]
        ])
        allureFilesPreparer.setLinks(testResult, testCaseDescription)
        assertThat(testResult).isEqualTo([
                "links": [
                        ["name": "Build", "type": "tms", "url": mBuildInfo.buildUrl],
                        ["name": "Reports", "type": "tms", "url": mBuildInfo.reportsUrl],
                        ["name": "Testcase #42", "type": "tms", "url": "testpalm_ui_url/testcase/appmetrica_project_id-42"],
                        ["name": "1", "type": "issue", "url": "issue/url/1"],
                        ["name": "2", "type": "issue", "url": "issue/url/2"],
                ]
        ])
    }

    @Test
    void testSetDescription() {
        final Map<String, ?> testResult = [:]
        allureFilesPreparer.setDescription(testResult)
        assertThat(testResult).isEqualTo([
                "description": """\
Os API level: 1
"""
        ])
    }

    @Test
    void testTestMessageIfIgnored() {
        final Map<String, ?> testResult = [
                "statusDetails": [
                        "message": "wrong_reason"
                ]
        ]
        final IgnoredTestDescription ignoredTestDescription = new IgnoredTestDescription(
                testCaseId: 42,
                reason: "test_reason"
        )
        allureFilesPreparer.setMessage(testResult, ignoredTestDescription)
        assertThat(testResult).isEqualTo([
                "statusDetails": [
                        "message": "test_reason"
                ]
        ])
    }

    @Test
    void testTestMessageIfIgnoredAndNoStatusDetails() {
        final Map<String, ?> testResult = [:]
        final IgnoredTestDescription ignoredTestDescription = new IgnoredTestDescription(
                testCaseId: 42,
                reason: "test_reason"
        )
        allureFilesPreparer.setMessage(testResult, ignoredTestDescription)
        assertThat(testResult).isEqualTo([:])
    }

    @Test
    void testTestMessageIfNotIgnored() {
        final Map<String, ?> testResult = [:]
        allureFilesPreparer.setMessage(testResult, null)
        assertThat(testResult).isEqualTo([:])
    }

    @Test
    void testAddLabels() {
        final Map<String, ?> testResult = [
                "labels": []
        ]
        final TestCaseDescription testCaseDescription = new TestCaseDescription(42, null, ["priority_1"],
                ["functionality_1", "functionality_2"], null)
        allureFilesPreparer.addLabels(testResult, testCaseDescription)
        assertThat(testResult).isEqualTo([
                "labels": [
                        ["name": "severity", "value": "priority_1"],
                        ["name": "feature", "value": "functionality_1"],
                        ["name": "feature", "value": "functionality_2"],
                ]
        ])
    }

    @Test
    void testAddLabelsWithoutPriorityAndFunctionality() {
        final Map<String, ?> testResult = [
                "labels": []
        ]
        final TestCaseDescription testCaseDescription = new TestCaseDescription(42, null, null, null, null)
        allureFilesPreparer.addLabels(testResult, testCaseDescription)
        assertThat(testResult).isEqualTo([
                "labels": [
                        ["name": "severity", "value": "No priority found"],
                ]
        ])
    }

    @Test
    void testAddLabelsWithoutLabels() {
        final Map<String, ?> testResult = [:]
        final TestCaseDescription testCaseDescription = new TestCaseDescription(42, null, ["priority_1"],
                ["functionality_1", "functionality_2"], null)
        allureFilesPreparer.addLabels(testResult, testCaseDescription)
        assertThat(testResult).isEqualTo([
                "labels": [
                        ["name": "severity", "value": "priority_1"],
                        ["name": "feature", "value": "functionality_1"],
                        ["name": "feature", "value": "functionality_2"],
                ]
        ])
    }
}
