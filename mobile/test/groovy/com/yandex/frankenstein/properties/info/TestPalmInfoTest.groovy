package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class TestPalmInfoTest {

    final String projectId = "app_metrica"
    final String versionId = "test_version"
    final String suiteId = "some_suite_id"
    final String token = "some_token"
    final String baseUrl = "/base/url"
    final String baseUiUrl = "/base/ui/url"
    final String reportType = "all"


    @Test
    void testExtractInfo() {
        final TestPalmInfo testPalmInfo = new TestPalmInfo(
                projectId, suiteId, versionId, token, baseUrl, baseUiUrl, reportType)
        assertThat(testPalmInfo.projectId).isEqualTo(projectId)
        assertThat(testPalmInfo.suiteId).isEqualTo(suiteId)
        assertThat(testPalmInfo.versionId).isEqualTo(versionId)
        assertThat(testPalmInfo.token).isEqualTo(token)
        assertThat(testPalmInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(testPalmInfo.baseUiUrl).isEqualTo(baseUiUrl)
        assertThat(testPalmInfo.reportType).isEqualTo(reportType)
    }

    @Test
    void testExtractInfoDefaults() {
        final TestPalmInfo testPalmInfo = new TestPalmInfo(projectId, suiteId, versionId, token, "", "", "")
        assertThat(testPalmInfo.projectId).isEqualTo(projectId)
        assertThat(testPalmInfo.suiteId).isEqualTo(suiteId)
        assertThat(testPalmInfo.versionId).isEqualTo(versionId)
        assertThat(testPalmInfo.token).isEqualTo(token)
        assertThat(testPalmInfo.baseUrl).isEqualTo("https://testpalm-api.yandex-team.ru")
        assertThat(testPalmInfo.baseUiUrl).isEqualTo("https://testpalm2.yandex-team.ru")
        assertThat(testPalmInfo.reportType).isEqualTo(ReportType.DEFAULT)
    }

    @Test
    void testToString() {
        final TestPalmInfo info = new TestPalmInfo(projectId, suiteId, versionId, token, baseUrl, baseUiUrl, reportType)
        final String expected = """
TestPalmInfo {
    projectId='$projectId',
    suiteId='$suiteId',
    versionId='$versionId',
    token='$token'
    baseUrl='$baseUrl'
    baseUiUrl='$baseUiUrl'
    reportType='$reportType'
}
"""
        assertThat(info.toString()).isEqualTo(expected)
    }
}
