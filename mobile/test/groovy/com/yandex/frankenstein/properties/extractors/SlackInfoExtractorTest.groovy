package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.SlackInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class SlackInfoExtractorTest {

    final String baseUrl = "/some/slack/base/url"
    final String token = "some_slack_token"
    final String channelId = "some_slack_channel_id"
    final String urlShortener = "/some/slack/url/shortener"

    final SlackInfoExtractor mSlackInfoExtractor = new SlackInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "slack.base.url": baseUrl,
                        "slack.token": token,
                        "slack.channel.id": channelId,
                        "slack.url.shortener": urlShortener
                ]}
        ] as Project
        final SlackInfo slackInfo = mSlackInfoExtractor.extractInfo(project)
        assertThat(slackInfo.token).isEqualTo(token)
        assertThat(slackInfo.channelId).isEqualTo(channelId)
        assertThat(slackInfo.urlShortenerBase).isEqualTo(urlShortener)
        assertThat(slackInfo.baseUrl).isEqualTo(baseUrl)
    }
}
