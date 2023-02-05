package ru.yandex.bitbucket.plugin.teamcity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.mock.Calls;
import ru.yandex.bitbucket.plugin.teamcity.entity.RunTeamcityBuildInfo;
import ru.yandex.bitbucket.plugin.teamcity.entity.StopTeamcityBuildInfo;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildInfo;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildState;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuilds;

import static org.mockito.Mockito.when;

public class TeamcityApiTest {
    @Mock
    private TeamcityBuildService buildService;

    private TeamcityApi teamcityApi;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        teamcityApi = new TeamcityApi(buildService);
    }

    @Test
    public void shouldGetTeamcityBuildByLocator() throws Exception {
        TeamcityBuilds teamcityBuilds = new TeamcityBuilds();
        TeamcityBuild build = new TeamcityBuild();
        teamcityBuilds.setCount(1L);
        teamcityBuilds.setHref("href");
        build.setId(2L);
        build.setBuildTypeId("buildTypeId");
        build.setStatus(TeamcityBuildStatus.SUCCESS);
        build.setState(TeamcityBuildState.finished);
        build.setBranchName("branchName");
        Call<TeamcityBuild> call = Calls.response(build);

        String locator = "locator";
        when(buildService.getTeamcityBuildListByLocator(locator)).thenReturn(call);

        TeamcityBuild teamcityBuild = teamcityApi.getTeamcityBuildByLocator(locator);
        Assert.assertEquals(build, teamcityBuild);
    }

    @Test
    public void shouldRunTeamcityBuild() throws Exception {
        TeamcityBuildInfo expectedBuildInfo = new TeamcityBuildInfo();
        expectedBuildInfo.setId(1L);
        expectedBuildInfo.setState(TeamcityBuildState.queued);
        Call<TeamcityBuildInfo> call = Calls.response(expectedBuildInfo);

        RunTeamcityBuildInfo.BuildType buildType = new RunTeamcityBuildInfo.BuildType("buildTypeId");
        RunTeamcityBuildInfo runTeamcityBuildInfo = new RunTeamcityBuildInfo("branchName", buildType);
        when(buildService.startTeamcityBuild(runTeamcityBuildInfo)).thenReturn(call);

        TeamcityBuildInfo teamcityBuildInfo = teamcityApi.startTeamcityBuild(runTeamcityBuildInfo);
        Assert.assertEquals(expectedBuildInfo, teamcityBuildInfo);
    }

    @Test
    public void shouldStopTeamcityBuild() throws Exception {
        long id = 2l;
        TeamcityBuildInfo expectedBuildInfo = new TeamcityBuildInfo();
        expectedBuildInfo.setId(1L);
        expectedBuildInfo.setState(TeamcityBuildState.finished);
        Call<TeamcityBuildInfo> call = Calls.response(expectedBuildInfo);

        StopTeamcityBuildInfo stopTeamcityBuildInfo = new StopTeamcityBuildInfo("test comment", false);
        when(buildService.stopTeamcityBuild(id, stopTeamcityBuildInfo)).thenReturn(call);

        TeamcityBuildInfo teamcityBuildInfo = teamcityApi.stopTeamcityBuildInfo(id, stopTeamcityBuildInfo);
        Assert.assertEquals(expectedBuildInfo, teamcityBuildInfo);
    }
}