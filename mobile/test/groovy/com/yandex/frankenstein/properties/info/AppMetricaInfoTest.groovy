package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class AppMetricaInfoTest {

    final String testpalmVersionId = "some_id"
    final String appmetricaApikey = "some_appmetrica_apikey"
    final String appmetricaBaseUrl = "/some/appmetrica/base/url"

    @Test
    void testExtractInfo() {
        final AppMetricaInfo appMetricaInfo =
                new AppMetricaInfo(appmetricaApikey, testpalmVersionId, appmetricaBaseUrl)
        assertThat(appMetricaInfo.reportName).isEqualTo(testpalmVersionId)
        assertThat(appMetricaInfo.apiKey).isEqualTo(appmetricaApikey)
        assertThat(appMetricaInfo.baseUrl).isEqualTo(appmetricaBaseUrl)
    }

    @Test
    void testExtractInfoWithoutBaseURL() {
        final AppMetricaInfo appMetricaInfo =
                new AppMetricaInfo(appmetricaApikey, testpalmVersionId, "")
        assertThat(appMetricaInfo.reportName).isEqualTo(testpalmVersionId)
        assertThat(appMetricaInfo.apiKey).isEqualTo(appmetricaApikey)
        assertThat(appMetricaInfo.baseUrl).isEqualTo('https://report.appmetrica.yandex.net')
    }

    @Test
    void testToString() {
        final AppMetricaInfo info = new AppMetricaInfo(appmetricaApikey, testpalmVersionId, appmetricaBaseUrl)
        final String expected = """
AppMetricaInfo {
    apiKey='$appmetricaApikey',
    reportName='$testpalmVersionId',
    baseUrl='$appmetricaBaseUrl'
}
"""
        assertThat(info.toString()).isEqualTo(expected)
    }
}
