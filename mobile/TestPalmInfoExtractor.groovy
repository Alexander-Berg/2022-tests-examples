package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.TestPalmInfo
import com.yandex.frankenstein.yav.YavSecretsHolderFacade
import groovy.transform.CompileStatic
import org.gradle.api.Project

@CompileStatic
class TestPalmInfoExtractor {
    TestPalmInfo extractInfo(final Project project) {
        final String projectId = project.properties['testpalm.project.id']
        final String versionId = project.properties['testpalm.version.id']
        final String testSuiteId = project.properties['testpalm.suite.id']
        final String testPalmApiToken = project.properties['testpalm.token'] ?: getTestPalmToken(project)
        final String testPalmBaseUrl = project.properties['testpalm.base.url']
        final String testPalmBaseUiUrl = project.properties['testpalm.base.ui.url']
        final String testPalmReportType = project.properties['testpalm.report.type']

        return new TestPalmInfo(projectId, testSuiteId, versionId, testPalmApiToken,
                testPalmBaseUrl, testPalmBaseUiUrl, testPalmReportType)
    }

    private String getTestPalmToken(final Project project) {
        return new YavSecretsHolderFacade(project).getTestPalmToken()
    }
}
