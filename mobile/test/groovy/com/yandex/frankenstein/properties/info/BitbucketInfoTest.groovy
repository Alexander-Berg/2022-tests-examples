package com.yandex.frankenstein.properties.info

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class BitbucketInfoTest {

    final String projectKey = "some_project_key"
    final String repoName = "appmetrica_repo_name"
    final String pullRequestId = "some pull request id"
    final String baseUrl = "/some/bitbucket/base/url"
    final String token = "appmetrica_token"

    @Test
    void testExtractInfo() {
        final BitbucketInfo bitbucketInfo =
                new BitbucketInfo(projectKey, repoName, pullRequestId, baseUrl, token)
        assertThat(bitbucketInfo.projectKey).isEqualTo(projectKey)
        assertThat(bitbucketInfo.repoName).isEqualTo(repoName)
        assertThat(bitbucketInfo.pullRequestId).isEqualTo(pullRequestId)
        assertThat(bitbucketInfo.baseUrl).isEqualTo(baseUrl)
        assertThat(bitbucketInfo.token).isEqualTo(token)
    }

    @Test
    void testToString() {
        final BitbucketInfo info = new BitbucketInfo(projectKey, repoName, pullRequestId, baseUrl, token)
        final String expected = """
BitbucketInfo {
    projectKey='$projectKey',
    repoName='$repoName',
    pullRequestId='$pullRequestId',
    baseUrl='$baseUrl',
    token='$token',
}
"""
        assertThat(info.toString()).isEqualTo(expected)
    }
}
