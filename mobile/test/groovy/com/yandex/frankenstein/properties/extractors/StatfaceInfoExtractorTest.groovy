package com.yandex.frankenstein.properties.extractors


import com.yandex.frankenstein.properties.info.StatfaceInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class StatfaceInfoExtractorTest {

    final String token = "some_bitbucket_project_key"
    final String reportName = "app_metrica"
    final String reportDataUrl = "/some/bitbucket/base/url"

    final StatfaceInfoExtractor statfaceInfoExtractor = new StatfaceInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "statface.token": token,
                        "statface.report.name": reportName,
                        "statface.report.data.url": reportDataUrl,
                ]}
        ] as Project
        final StatfaceInfo statfaceInfo = statfaceInfoExtractor.extractInfo(project)
        assertThat(statfaceInfo.token).isEqualTo(token)
        assertThat(statfaceInfo.reportName).isEqualTo(reportName)
        assertThat(statfaceInfo.reportDataUrl).isEqualTo(reportDataUrl)
    }
}
