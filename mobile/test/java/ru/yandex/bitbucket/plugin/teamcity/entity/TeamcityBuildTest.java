package ru.yandex.bitbucket.plugin.teamcity.entity;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TeamcityBuildTest {
    private final String preparedJsonString = String.format("{\"branchName\":\"branchName\",\"buildTypeId\":\"buildTypeId\"," +
            "\"id\":1,\"revisions\":{\"count\":2,\"revision\":[{\"version\":\"revision\"}]}," +
            "\"state\":\"%s\",\"status\":\"%s\"}", TeamcityBuildState.finished, TeamcityBuildStatus.ERROR);
    private final TeamcityBuild preparedTeamcityBuild = new TeamcityBuild();
    private final Moshi moshi = new Moshi.Builder().build();
    private final JsonAdapter<TeamcityBuild> adapter = moshi.adapter(TeamcityBuild.class);

    @Before
    public void init() {
        preparedTeamcityBuild.setId(1L);
        preparedTeamcityBuild.setBuildTypeId("buildTypeId");
        preparedTeamcityBuild.setStatus(TeamcityBuildStatus.ERROR);
        preparedTeamcityBuild.setState(TeamcityBuildState.finished);
        preparedTeamcityBuild.setBranchName("branchName");
        TeamcityBuild.Revisions revisions = new TeamcityBuild.Revisions();
        revisions.setCount(2L);
        revisions.setRevision(Collections.singletonList(new TeamcityBuild.Revision("revision")));
        preparedTeamcityBuild.setRevisions(revisions);
    }

    @Test
    public void shouldConvertObjectToJson() {
        String jsonString = adapter.toJson(preparedTeamcityBuild);
        assertEquals(preparedJsonString, jsonString);
    }

    @Test
    public void shouldConvertJsonToObject() throws IOException {
        TeamcityBuild teamcityBuild = adapter.fromJson(preparedJsonString);
        assertEquals(preparedTeamcityBuild, teamcityBuild);
    }

}