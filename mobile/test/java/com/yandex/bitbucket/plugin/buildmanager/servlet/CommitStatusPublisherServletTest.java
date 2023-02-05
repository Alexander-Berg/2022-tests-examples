package com.yandex.bitbucket.plugin.buildmanager.servlet;


import com.atlassian.bitbucket.build.BuildState;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.CommitStatusPublisherData;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.utils.api.TeamcityApiHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildState;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class CommitStatusPublisherServletTest {
    private static final long PR_ID = 1L;
    private static final int REPO_ID = 2;
    private static final String BRANCH = String.format("%d/merge", PR_ID);
    private static final String REVISION = "revision";
    private static final Long BUILD_ID = 30114671L;
    private static final String BUILD_TYPE_ID = "MobileNew_Monorepo_Infra_BitbucketPlugins_BitbucketPluginsBuildBbTestOk";
    private static final String STATUS_TEXT = "status_text";
    private static final String URL = String.format("https://teamcity.yandex-team.ru/viewLog.html?buildId=%d&buildTypeId=%s", BUILD_ID, BUILD_TYPE_ID);
    private final CommitStatusPublisherData commitStatusPublisherData = new CommitStatusPublisherData();
    private final TeamcityBuild teamcityBuild = new TeamcityBuild();

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private TeamcityApiHelper teamcityApiHelper;

    @Mock
    private Repository repository;

    private final CacheManager cacheManager = new MemoryCacheManager();

    private Cache<BuildResultCacheKey, TeamcityBuildResult> buildResultCache = ConstantUtils.getBuildResultCache(cacheManager);

    private CommitStatusPublisherServlet servlet;

    private BuildResultCacheKey buildResultCacheKey;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        commitStatusPublisherData.setDescription("description");
        commitStatusPublisherData.setState(BuildState.SUCCESSFUL);
        commitStatusPublisherData.setKey("key");
        commitStatusPublisherData.setName("name");
        commitStatusPublisherData.setUrl(URL);

        teamcityBuild.setId(BUILD_ID);
        teamcityBuild.setBuildTypeId(BUILD_TYPE_ID);
        teamcityBuild.setBranchName(BRANCH);
        teamcityBuild.setStatusText(STATUS_TEXT);

        buildResultCacheKey = new BuildResultCacheKey(REPO_ID, PR_ID, BUILD_TYPE_ID);

        when(teamcityApiHelper.getTeamcityBuildById(BUILD_ID, null, REVISION)).thenReturn(teamcityBuild);
        when(repository.getId()).thenReturn(REPO_ID);
        when(repositoryService.getBySlug("mobile", "monorepo")).thenReturn(repository);
        servlet = new CommitStatusPublisherServlet(cacheManager, repositoryService, teamcityApiHelper);
    }

    @Test
    public void cspFinishedBuild() {
        teamcityBuild.setStatus(TeamcityBuildStatus.SUCCESS);
        teamcityBuild.setState(TeamcityBuildState.finished);
        checkBuild(TeamcityBuildStatus.SUCCESS, ConstantUtils.SUCCESSFUL_BUILD_STATUS_MESSAGE);
    }

    @Test
    public void cspQueuedBuild() {
        teamcityBuild.setStatus(null);
        teamcityBuild.setState(TeamcityBuildState.queued);
        String url = String.format("https://teamcity.yandex-team.ru/viewLog.html?itemId=%s", BUILD_ID);
        commitStatusPublisherData.setUrl(url);
        checkBuild(TeamcityBuildStatus.RUNNING, ConstantUtils.RUNNING_BUILD_STATUS_MESSAGE);
    }

    @Test
    public void cspRunningBuild() {
        teamcityBuild.setStatus(TeamcityBuildStatus.SUCCESS);
        teamcityBuild.setState(TeamcityBuildState.running);
        checkBuild(TeamcityBuildStatus.RUNNING, ConstantUtils.RUNNING_BUILD_STATUS_MESSAGE);
    }

    @Test
    public void cspFailedBuild() {
        teamcityBuild.setStatus(TeamcityBuildStatus.FAILURE);
        teamcityBuild.setState(TeamcityBuildState.finished);
        checkBuild(TeamcityBuildStatus.FAILURE, ConstantUtils.makeFailedBuildStatusMessage(STATUS_TEXT));
    }

    @Test
    public void cspErrorBuild() {
        teamcityBuild.setStatus(TeamcityBuildStatus.ERROR);
        teamcityBuild.setState(TeamcityBuildState.unknown);
        checkBuild(TeamcityBuildStatus.ERROR, ConstantUtils.makeFailedBuildStatusMessage(STATUS_TEXT));
    }

    private void checkBuild(TeamcityBuildStatus status, String message) {
        Response response = servlet.setBuildStatus(REVISION, commitStatusPublisherData);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());

        TeamcityBuildResult teamcityBuildResult = buildResultCache.get(buildResultCacheKey);
        assertEquals(status, teamcityBuildResult.getStatus());
        assertEquals(BUILD_ID, teamcityBuildResult.getBuildId());
        assertEquals(REVISION, teamcityBuildResult.getMergeHash());
        assertEquals(message, teamcityBuildResult.getStatusMessage());
    }
}
