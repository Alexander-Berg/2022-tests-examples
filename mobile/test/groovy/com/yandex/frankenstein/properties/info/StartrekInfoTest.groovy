package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class StartrekInfoTest {

    final String token = "some_statrack_token"
    final String baseUrl = "/some/base/url"
    final String baseUiUrl = "/some/base/ui/url"
    final List<String> ticketIdList = ["ticket1", "ticket2"]

    @Test
    void testExtractInfo() {
        final StartrekInfo startrekInfo = new StartrekInfo(token, baseUrl, baseUiUrl, ticketIdList)
        assertThat(startrekInfo.token).isEqualTo(token)
        assertThat(startrekInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(startrekInfo.baseUiUrl).isEqualTo(baseUiUrl)
        assertThat(startrekInfo.ticketIdList).containsExactlyElementsOf(ticketIdList)
    }

    @Test
    void testExtractInfoWithoutBaseUrl() {
        final StartrekInfo startrekInfo = new StartrekInfo(token, "", "", ticketIdList)
        assertThat(startrekInfo.token).isEqualTo(token)
        assertThat(startrekInfo.baseUrl).isEqualTo('https://st-api.yandex-team.ru/v2')
        assertThat(startrekInfo.baseUiUrl).isEqualTo('https://st.yandex-team.ru')
        assertThat(startrekInfo.ticketIdList).containsExactlyElementsOf(ticketIdList)
    }

    @Test
    void testToString() {
        final StartrekInfo startrekInfo = new StartrekInfo(token, baseUrl, baseUiUrl, ticketIdList)
        final String expected = """
StartrekInfo {
    token='$token'
    baseUrl='$baseUrl'
    baseUiUrl='$baseUiUrl'
    ticketIdList='$ticketIdList'
}
"""
        assertThat(startrekInfo.toString()).isEqualTo(expected)
    }
}
