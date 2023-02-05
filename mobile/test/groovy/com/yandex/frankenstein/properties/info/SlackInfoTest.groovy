package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class SlackInfoTest {

    final String baseUrl = "/slack/base/url"
    final String token = "some_slack_token"
    final String channelId = "app_metrica_chanel_id"
    final String urlShortener = "/slack/url/shortener"

    @Test
    void testExtractInfo() {
        final SlackInfo slackInfo = new SlackInfo(token, channelId, urlShortener, baseUrl)
        assertThat(slackInfo.token).isEqualTo(token)
        assertThat(slackInfo.channelId).isEqualTo(channelId)
        assertThat(slackInfo.urlShortenerBase).isEqualTo(urlShortener)
        assertThat(slackInfo.baseUrl).isEqualTo(baseUrl)
    }

    @Test
    void testExtractInfoWithoutBaseUrl() {
        final SlackInfo slackInfo = new SlackInfo(token, channelId, urlShortener, "")
        assertThat(slackInfo.token).isEqualTo(token)
        assertThat(slackInfo.channelId).isEqualTo(channelId)
        assertThat(slackInfo.urlShortenerBase).isEqualTo(urlShortener)
        assertThat(slackInfo.baseUrl).isEqualTo("https://slack.com")
    }

    @Test
    void testExtractInfoWithoutUrlShortener() {
        final SlackInfo slackInfo = new SlackInfo(token, channelId, "", baseUrl)
        assertThat(slackInfo.token).isEqualTo(token)
        assertThat(slackInfo.channelId).isEqualTo(channelId)
        assertThat(slackInfo.urlShortenerBase).isEqualTo("https://nda.ya.ru/--?url=")
        assertThat(slackInfo.baseUrl).isEqualTo(baseUrl)
    }

    @Test
    void testToString() {
        final SlackInfo info = new SlackInfo(token, channelId, urlShortener, baseUrl)
        final String expected = """
SlackInfo {
    token='$token',
    channelId='$channelId',
    urlShortenerBase='$urlShortener',
    baseUrl='$baseUrl'
}
"""
        assertThat(info.toString()).isEqualTo(expected)
    }
}
