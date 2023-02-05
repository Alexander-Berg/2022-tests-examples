package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.TestPalmInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestPalmInfoExtractorTest {

    final String projectId = "some_project_id"
    final String versionId = "some_testpalm_version_id"
    final String suiteId = "some_testpalm_suite_id"
    final String token = "some_testpalm_token"
    final String baseUrl = "/some/testpalm/base/url"
    final String baseUiUrl = "/some/testpalm/base/ui/url"
    final String reportType = "all"

    final TestPalmInfoExtractor mTestPalmInfoExtractor = new TestPalmInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "testpalm.project.id": projectId,
                        "testpalm.version.id": versionId,
                        "testpalm.suite.id": suiteId,
                        "testpalm.token": token,
                        "testpalm.base.url": baseUrl,
                        "testpalm.base.ui.url": baseUiUrl,
                        "testpalm.report.type": reportType
                ]}
        ] as Project
        final TestPalmInfo testPalmInfo = mTestPalmInfoExtractor.extractInfo(project)
        assertThat(testPalmInfo.projectId).isEqualTo(projectId)
        assertThat(testPalmInfo.suiteId).isEqualTo(suiteId)
        assertThat(testPalmInfo.versionId).isEqualTo(versionId)
        assertThat(testPalmInfo.token).isEqualTo(token)
        assertThat(testPalmInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(testPalmInfo.baseUiUrl).isEqualTo(baseUiUrl)
        assertThat(testPalmInfo.reportType).isEqualTo(reportType)
    }
}
