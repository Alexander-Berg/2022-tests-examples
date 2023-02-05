package com.yandex.bitbucket.plugin.utils.api;

import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestState;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStateCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStatus;
import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.exception.UnauthorizedException;
import ru.yandex.bitbucket.plugin.teamcity.TeamcityApi;
import ru.yandex.bitbucket.plugin.teamcity.entity.RunTeamcityBuildInfo;
import ru.yandex.bitbucket.plugin.teamcity.entity.RunTeamcityBuildInfo.BuildType;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildInfo;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildState;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class TeamcityApiHelperTest {
    private final String UNAUTHORIZED_EXCEPTION_MESSAGE = "UnauthorizedException";
    private final String BUILD_TYPE_ID = "buildTypeId";
    private final String BRANCH_NAME = "buildTypeId";
    private final String MERGE_HASH = "merge_hash";
    private final BuildType buildType = new BuildType(BUILD_TYPE_ID);
    private final RunTeamcityBuildInfo runTeamcityBuildInfo = new RunTeamcityBuildInfo(BRANCH_NAME, buildType);
    private final long PR_ID = 2L;
    private final int REPO_ID = 1;
    private final long BUILD_ID = 5L;
    private final String ID_LOCATOR = String.format("id:%d", BUILD_ID);
    private final String TYPE_AND_BRANCH_LOCATOR = String.format("buildType:%s,branch:%s,running:any", BUILD_TYPE_ID, BRANCH_NAME);
    private final BuildResultCacheKey buildResultCacheKey = new BuildResultCacheKey(REPO_ID, PR_ID, BUILD_TYPE_ID);
    private PullRequestStateCacheKey pullRequestStateCacheKey;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PullRequest pullRequest;
    @Mock
    private TeamcityApiFactory teamcityApiFactory;
    @Mock
    private TeamcityApi teamcityApi;

    private TeamcityApiHelper teamcityApiHelper;

    private CacheManager cacheManager = new MemoryCacheManager();
    private Cache<BuildResultCacheKey, TeamcityBuildResult> buildResultCache = ConstantUtils.getBuildResultCache(cacheManager);
    private Cache<PullRequestStateCacheKey, PullRequestState> pullRequestErrorCache = ConstantUtils.getPullRequestErrorCache(cacheManager);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(teamcityApiFactory.getTeamcityApi()).thenReturn(teamcityApi);
        when(pullRequest.getToRef().getRepository().getId()).thenReturn(REPO_ID);
        when(pullRequest.getId()).thenReturn(PR_ID);
        pullRequestStateCacheKey = new PullRequestStateCacheKey(RepoPrCompositeKey.from(pullRequest), MERGE_HASH);
        teamcityApiHelper = new TeamcityApiHelper(cacheManager, teamcityApiFactory);
    }

    @Test
    public void startBuild() throws IOException {
        TeamcityBuildInfo teamcityBuildInfo = new TeamcityBuildInfo();
        teamcityBuildInfo.setId(3L);
        teamcityBuildInfo.setState(TeamcityBuildState.queued);
        when(teamcityApi.startTeamcityBuild(runTeamcityBuildInfo)).thenReturn(teamcityBuildInfo);
        teamcityApiHelper.startTeamcityBuild(BRANCH_NAME, BUILD_TYPE_ID, pullRequest, MERGE_HASH);
        assertEquals(new TeamcityBuildResult(TeamcityBuildStatus.RUNNING, teamcityBuildInfo.getId(), MERGE_HASH,
                ConstantUtils.RUNNING_BUILD_STATUS_MESSAGE), buildResultCache.get(buildResultCacheKey));
    }

    @Test
    public void startBuildNotQueuedBuild() throws IOException {
        TeamcityBuildInfo teamcityBuildInfo = new TeamcityBuildInfo();
        teamcityBuildInfo.setId(3L);
        teamcityBuildInfo.setState(TeamcityBuildState.unknown);
        when(teamcityApi.startTeamcityBuild(runTeamcityBuildInfo)).thenReturn(teamcityBuildInfo);
        teamcityApiHelper.startTeamcityBuild(BRANCH_NAME, BUILD_TYPE_ID, pullRequest, MERGE_HASH);
        assertEquals(new TeamcityBuildResult(TeamcityBuildStatus.ERROR, null, MERGE_HASH,
                ConstantUtils.ERROR_STATUS_MESSAGE), buildResultCache.get(buildResultCacheKey));
    }

    @Test
    public void startBuildNullResponse() throws IOException {
        when(teamcityApi.startTeamcityBuild(runTeamcityBuildInfo)).thenReturn(null);
        teamcityApiHelper.startTeamcityBuild(BRANCH_NAME, BUILD_TYPE_ID, pullRequest, MERGE_HASH);
        assertEquals(new TeamcityBuildResult(TeamcityBuildStatus.ERROR, null, MERGE_HASH,
                ConstantUtils.ERROR_STATUS_MESSAGE), buildResultCache.get(buildResultCacheKey));
    }

    @Test
    public void startBuildIOException() throws IOException {
        when(teamcityApi.startTeamcityBuild(runTeamcityBuildInfo)).thenThrow(IOException.class);
        teamcityApiHelper.startTeamcityBuild(BRANCH_NAME, BUILD_TYPE_ID, pullRequest, MERGE_HASH);
        assertEquals(new TeamcityBuildResult(TeamcityBuildStatus.ERROR, null, MERGE_HASH,
                ConstantUtils.ERROR_STATUS_MESSAGE), buildResultCache.get(buildResultCacheKey));
    }

    @Test
    public void startBuildUnauthorizedException() throws IOException {
        when(teamcityApi.startTeamcityBuild(runTeamcityBuildInfo))
                .thenThrow(new UnauthorizedException(UNAUTHORIZED_EXCEPTION_MESSAGE));
        teamcityApiHelper.startTeamcityBuild(BRANCH_NAME, BUILD_TYPE_ID, pullRequest, MERGE_HASH);
        checkUnauthorizedException();
    }

    @Test
    public void getBuildById() throws IOException {
        TeamcityBuild teamcityBuildExpected = new TeamcityBuild();
        when(teamcityApi.getTeamcityBuildByLocator(ID_LOCATOR)).thenReturn(teamcityBuildExpected);
        TeamcityBuild teamcityBuild = teamcityApiHelper.getTeamcityBuildById(BUILD_ID, pullRequest, MERGE_HASH);
        assertEquals(teamcityBuildExpected, teamcityBuild);
    }

    @Test
    public void getBuildByIdUnauthorizedException() throws IOException {
        when(teamcityApi.getTeamcityBuildByLocator(ID_LOCATOR))
                .thenThrow(new UnauthorizedException(UNAUTHORIZED_EXCEPTION_MESSAGE));
        TeamcityBuild teamcityBuild = teamcityApiHelper.getTeamcityBuildById(BUILD_ID, pullRequest, MERGE_HASH);
        checkUnauthorizedException();
        assertNull(teamcityBuild);
    }

    @Test
    public void getBuildByIdIOException() throws IOException {
        when(teamcityApi.getTeamcityBuildByLocator(ID_LOCATOR)).thenThrow(IOException.class);
        TeamcityBuild teamcityBuild = teamcityApiHelper.getTeamcityBuildById(BUILD_ID, pullRequest, MERGE_HASH);
        assertNull(pullRequestErrorCache.get(pullRequestStateCacheKey));
        assertNull(teamcityBuild);
    }

    @Test
    public void getBuildInfoByBuildTypeAndBranch() throws IOException {
        TeamcityBuild teamcityBuildExpected = new TeamcityBuild();
        when(teamcityApi.getTeamcityBuildByLocator(TYPE_AND_BRANCH_LOCATOR)).thenReturn(teamcityBuildExpected);
        TeamcityBuild teamcityBuild = teamcityApiHelper.getLastTeamcityBuildByBranchAndBuildType(BRANCH_NAME, BUILD_TYPE_ID,
                pullRequest, MERGE_HASH);
        assertEquals(teamcityBuildExpected, teamcityBuild);
    }

    @Test
    public void getBuildByBuildTypeAndBranchUnauthorizedException() throws IOException {
        when(teamcityApi.getTeamcityBuildByLocator(TYPE_AND_BRANCH_LOCATOR))
                .thenThrow(new UnauthorizedException(UNAUTHORIZED_EXCEPTION_MESSAGE));
        TeamcityBuild teamcityBuild = teamcityApiHelper.getLastTeamcityBuildByBranchAndBuildType(BRANCH_NAME, BUILD_TYPE_ID,
                pullRequest, MERGE_HASH);
        assertNull(teamcityBuild);
        checkUnauthorizedException();
    }

    @Test
    public void getBuildByBuildTypeAndBranchIOException() throws IOException {
        when(teamcityApi.getTeamcityBuildByLocator(TYPE_AND_BRANCH_LOCATOR)).thenThrow(IOException.class);
        TeamcityBuild teamcityBuild = teamcityApiHelper.getLastTeamcityBuildByBranchAndBuildType(BRANCH_NAME, BUILD_TYPE_ID,
                pullRequest, MERGE_HASH);
        assertNull(pullRequestErrorCache.get(pullRequestStateCacheKey));
        assertNull(teamcityBuild);
    }

    private void checkUnauthorizedException() {
        assertNull(buildResultCache.get(buildResultCacheKey));
        assertEquals(new PullRequestState(PullRequestStatus.ERROR, UNAUTHORIZED_EXCEPTION_MESSAGE),
                pullRequestErrorCache.get(pullRequestStateCacheKey));
    }
}