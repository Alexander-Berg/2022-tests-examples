package com.yandex.frankenstein.properties.extractors

import com.yandex.frankenstein.properties.info.BitbucketInfo
import org.gradle.api.Project
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class BitbucketInfoExtractorTest {

    final String projectKey = "some_bitbucket_project_key"
    final String repoName = "app_metrica"
    final String pullRequestId = "some_id"
    final String baseUrl = "/some/bitbucket/base/url"
    final String token = "app_metrica_bitbucket_token"

    final BitbucketInfoExtractor mBitbucketInfoExtractor = new BitbucketInfoExtractor()

    @Test
    void testExtractInfo() {
        final Project project = [
                getProperties: {[
                        "bitbucket.project.key": projectKey,
                        "bitbucket.repo.name": repoName,
                        "bitbucket.pullrequest.id": pullRequestId,
                        "bitbucket.base.url": baseUrl,
                        "bitbucket.token": token,
                ]}
        ] as Project
        final BitbucketInfo bitbucketInfo = mBitbucketInfoExtractor.extractInfo(project)
        assertThat(bitbucketInfo.projectKey).isEqualTo(projectKey)
        assertThat(bitbucketInfo.repoName).isEqualTo(repoName)
        assertThat(bitbucketInfo.pullRequestId).isEqualTo(pullRequestId)
        assertThat(bitbucketInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(bitbucketInfo.token).isEqualTo(token)
    }

    @Test
    void testParsePullRequestId() {
        final String id = "123"
        final Project project = [
                getProperties: {[
                        "bitbucket.pullrequest.id": "$id/merge",
                        "bitbucket.token": token,
                ]}
        ] as Project
        final BitbucketInfo bitbucketInfo = mBitbucketInfoExtractor.extractInfo(project)
        assertThat(bitbucketInfo.pullRequestId).isEqualTo(id)
    }
}
