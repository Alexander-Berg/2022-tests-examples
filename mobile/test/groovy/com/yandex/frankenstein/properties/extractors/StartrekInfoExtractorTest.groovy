package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.StartrekInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class StartrekInfoExtractorTest {

    final String baseUrl = "/some/startrek/base/url"
    final String token = "some_slack_token"
    final String ticketIdString = " ticket1, ticket2  ,  , ,,  ticket3"

    final StartrekInfoExtractor mStartrekInfoExtractor = new StartrekInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "startrek.token": token,
                        "startrek.base.url": baseUrl,
                        "startrek.ticket.id": ticketIdString
                ]}
        ] as Project
        final StartrekInfo startrekInfo = mStartrekInfoExtractor.extractInfo(project)
        assertThat(startrekInfo.token).isEqualTo(token)
        assertThat(startrekInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(startrekInfo.ticketIdList).containsExactlyInAnyOrder("ticket1", "ticket2", "ticket3")
    }

    @Test
    void testExtractInfoWithoutTicketId() {
        final Project project = [
                getProperties: {[
                        "startrek.token": token,
                        "startrek.base.url": baseUrl
                ]}
        ] as Project
        final StartrekInfo startrekInfo = mStartrekInfoExtractor.extractInfo(project)
        assertThat(startrekInfo.token).isEqualTo(token)
        assertThat(startrekInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(startrekInfo.ticketIdList).isEmpty()
    }
}
