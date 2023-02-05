package ru.yandex.bitbucket.plugin.teamcity.entity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RunTeamcityBuildInfoTest {
    private final String preparedJsonString = "{\"branchName\":\"branchName\",\"buildType\":{\"id\":\"buildTypeId\"}}";
    private final RunTeamcityBuildInfo.BuildType buildType = new RunTeamcityBuildInfo.BuildType("buildTypeId");
    private final RunTeamcityBuildInfo preparedRunTeamcityBuildInfo = new RunTeamcityBuildInfo("branchName", buildType);
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<RunTeamcityBuildInfo> adapter = moshi.adapter(RunTeamcityBuildInfo.class);

    @Test
    public void shouldConvertObjectToJson() {
        String jsonString = adapter.toJson(preparedRunTeamcityBuildInfo);
        assertEquals(preparedJsonString, jsonString);
    }

    @Test
    public void shouldConvertJsonToObject() throws IOException {
        RunTeamcityBuildInfo runTeamcityBuildInfo = adapter.fromJson(preparedJsonString);
        assertEquals(preparedRunTeamcityBuildInfo, runTeamcityBuildInfo);
    }
}