package com.yandex.frankenstein.properties.info

import groovy.transform.CompileStatic

@CompileStatic
class TestPalmInfo {
    final String projectId
    final String suiteId
    final String versionId
    final String token
    final String baseUrl
    final String baseUiUrl
    final String reportType

    TestPalmInfo(final String projectId, final String suiteId, final String versionId,
                 final String token, final String baseUrl, final String baseUiUrl, final String reportType = "") {
        this.projectId = projectId
        this.suiteId = suiteId
        this.versionId = versionId
        this.token = token
        this.baseUrl = baseUrl ?: 'https://testpalm-api.yandex-team.ru'
        this.baseUiUrl = baseUiUrl ?: 'https://testpalm2.yandex-team.ru'
        this.reportType = reportType ?: ReportType.DEFAULT
    }

    @Override
    String toString() {
        return """
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
    }
}
