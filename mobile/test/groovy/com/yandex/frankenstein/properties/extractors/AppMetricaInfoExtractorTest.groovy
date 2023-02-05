package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.AppMetricaInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class AppMetricaInfoExtractorTest {

    final String testPalmVersionId = "some_testpalm_version_id"
    final String appmetricaApiKey = "some_appmetrica_apikey"
    final String appmetricaBaseUrl = "/some/appmetrica/base/url"

    final AppMetricaInfoExtractor mAppMetricaInfoExtractor = new AppMetricaInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "testpalm.version.id": testPalmVersionId,
                        "appmetrica.apikey": appmetricaApiKey,
                        "appmetrica.base.url": appmetricaBaseUrl
                ]}
        ] as Project
        final AppMetricaInfo appMetricaInfo = mAppMetricaInfoExtractor.extractInfo(project)
        assertThat(appMetricaInfo.reportName).isEqualTo(testPalmVersionId)
        assertThat(appMetricaInfo.apiKey).isEqualTo(appmetricaApiKey)
        assertThat(appMetricaInfo.baseUrl).isEqualTo(appmetricaBaseUrl)
    }
}
