package ru.yandex.bitbucket.plugin.teamcity.entity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class TeamcityBuildInfoTest {
    private final String preparedJsonString = String.format("{\"id\":1,\"state\":\"%s\"}", TeamcityBuildState.queued);
    private final TeamcityBuildInfo preparedTeamcityBuildInfo = new TeamcityBuildInfo();
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<TeamcityBuildInfo> adapter = moshi.adapter(TeamcityBuildInfo.class);

    @Before
    public void init() {
        preparedTeamcityBuildInfo.setId(1L);
        preparedTeamcityBuildInfo.setState(TeamcityBuildState.queued);
    }

    @Test
    public void shouldConvertObjectToJson() {
        String jsonString = adapter.toJson(preparedTeamcityBuildInfo);
        assertEquals(preparedJsonString, jsonString);
    }

    @Test
    public void shouldConvertJsonToObject() throws IOException {
        TeamcityBuildInfo teamcityBuildInfo = adapter.fromJson(preparedJsonString);
        assertEquals(preparedTeamcityBuildInfo, teamcityBuildInfo);
    }
}