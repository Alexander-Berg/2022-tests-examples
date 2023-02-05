package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class StatfaceInfoTest {

    final String token = "/slack/base/url"
    final String reportName = "some_slack_token"
    final String reportDataUrl = "/slack/url/shortener"

    @Test
    void testExtractInfo() {
        final StatfaceInfo statfaceInfo = new StatfaceInfo(token, reportName, reportDataUrl)
        assertThat(statfaceInfo.token).isEqualTo(token)
        assertThat(statfaceInfo.reportName).isEqualTo(reportName)
        assertThat(statfaceInfo.reportDataUrl).isEqualTo(reportDataUrl)
    }

    @Test
    void testExtractInfoWithoutReportDataUrl() {
        final StatfaceInfo statfaceInfo = new StatfaceInfo(token, reportName, "")
        assertThat(statfaceInfo.token).isEqualTo(token)
        assertThat(statfaceInfo.reportName).isEqualTo(reportName)
        assertThat(statfaceInfo.reportDataUrl).isEqualTo("https://upload.stat.yandex-team.ru/_api/report/data")
    }

    @Test
    void testToString() {
        final StatfaceInfo info = new StatfaceInfo(token, reportName, reportDataUrl)
        final String expected = """
StatfaceInfo {
    token='$token',
    reportName='$reportName',
    reportDataUrl='$reportDataUrl',
}
"""
        assertThat(info.toString()).isEqualTo(expected)
    }
}
