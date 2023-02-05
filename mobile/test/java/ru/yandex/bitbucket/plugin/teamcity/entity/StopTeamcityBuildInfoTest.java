package ru.yandex.bitbucket.plugin.teamcity.entity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class StopTeamcityBuildInfoTest {
    private final String preparedJsonString = "{\"comment\":\"testComment\",\"readdIntoQueue\":false}";
    private final StopTeamcityBuildInfo preparedStopTeamcityBuildInfo = new StopTeamcityBuildInfo("testComment", false);
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<StopTeamcityBuildInfo> adapter = moshi.adapter(StopTeamcityBuildInfo.class);

    @Test
    public void shouldConvertObjectToJson() {
        String jsonString = adapter.toJson(preparedStopTeamcityBuildInfo);
        assertEquals(preparedJsonString, jsonString);
    }

    @Test
    public void shouldConvertJsonToObject() throws IOException {
        StopTeamcityBuildInfo stopTeamcityBuildInfo = adapter.fromJson(preparedJsonString);
        assertEquals(preparedStopTeamcityBuildInfo, stopTeamcityBuildInfo);
    }
}