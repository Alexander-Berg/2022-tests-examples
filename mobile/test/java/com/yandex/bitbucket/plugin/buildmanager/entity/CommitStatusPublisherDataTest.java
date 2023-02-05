package com.yandex.bitbucket.plugin.buildmanager.entity;

import com.atlassian.bitbucket.build.BuildState;
import com.yandex.bitbucket.plugin.buildmanager.entity.CommitStatusPublisherData.BuildInfo;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CommitStatusPublisherDataTest {
    private static final Long BUILD_ID = 30114671L;
    private static final String BUILD_TYPE_ID = "MobileNew_Monorepo_Infra_BitbucketPlugins_BitbucketPluginsBuildBbTestOk";

    @Test
    public void shouldParseBuildIdAndBuildTypeId() {
        CommitStatusPublisherData commitStatusPublisherData = new CommitStatusPublisherData();
        String url = String.format("https://teamcity.yandex-team.ru/viewLog.html?buildId=%d&buildTypeId=%s", BUILD_ID, BUILD_TYPE_ID);
        commitStatusPublisherData.setUrl(url);
        BuildInfo buildInfo = commitStatusPublisherData.getBuildInfo();
        assertEquals(BUILD_ID, buildInfo.getBuildId());
        assertEquals(BUILD_TYPE_ID, buildInfo.getBuildTypeId());
    }

    @Test
    public void shouldNotParseUrl_emptyBuildId() {
        shouldNotParseUrl(String.format("https://teamcity.yandex-team.ru/viewLog.html?buildId=%s", BUILD_ID));
    }

    @Test
    public void shouldNotParseUrl_emptyBuildTypeId() {
        shouldNotParseUrl(String.format("https://teamcity.yandex-team.ru/viewLog.html?buildTypeId=%s", BUILD_TYPE_ID));
    }

    private void shouldNotParseUrl(String url) {
        CommitStatusPublisherData commitStatusPublisherData = new CommitStatusPublisherData();
        commitStatusPublisherData.setUrl(url);
        BuildInfo buildInfo = commitStatusPublisherData.getBuildInfo();
        assertNull(buildInfo);
    }

    @Test(expected = NumberFormatException.class)
    public void shouldNotParseUrl_NumberFormatException() {
        CommitStatusPublisherData commitStatusPublisherData = new CommitStatusPublisherData();
        String url = String.format("https://teamcity.yandex-team.ru/viewLog.html?buildId=%s&buildTypeId=%s", "string_not_long", BUILD_TYPE_ID);
        commitStatusPublisherData.setUrl(url);
        commitStatusPublisherData.getBuildInfo();
    }

    @Test
    public void shouldParseUrl_queuedBuild() {
        CommitStatusPublisherData commitStatusPublisherData = new CommitStatusPublisherData();
        String url = String.format("https://teamcity.yandex-team.ru/viewQueued.html?itemId=%s", BUILD_ID);
        commitStatusPublisherData.setUrl(url);
        BuildInfo buildInfo = commitStatusPublisherData.getBuildInfo();
        assertEquals(BUILD_ID, buildInfo.getBuildId());
        assertNull(buildInfo.getBuildTypeId());
    }

    @Test
    public void jsonToObject() throws IOException {
        CommitStatusPublisherData commitStatusPublisherData = new CommitStatusPublisherData();
        commitStatusPublisherData.setUrl("url");
        commitStatusPublisherData.setDescription("description");
        commitStatusPublisherData.setState(BuildState.FAILED);
        commitStatusPublisherData.setKey("key");
        commitStatusPublisherData.setName("name");

        ObjectMapper objectMapper = new ObjectMapper();

        String jsonString =
                        "{\n" +
                        "    \"state\": \"FAILED\",\n" +
                        "    \"key\": \"key\",\n" +
                        "    \"name\": \"name\",\n" +
                        "    \"url\": \"url\",\n" +
                        "    \"description\": \"description\"\n" +
                        "}";

        CommitStatusPublisherData dataFromJson = objectMapper.readValue(jsonString, CommitStatusPublisherData.class);
        assertEquals(commitStatusPublisherData, dataFromJson);
    }
}
