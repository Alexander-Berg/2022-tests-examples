package com.yandex.bitbucket.plugin.buildmanager.servlet;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildList;
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
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CacheInfoServletTest extends AbstractServletTest {
    @Mock
    private CommitService commitService;

    @Mock
    private PullRequestService pullRequestService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PullRequest pullRequest;

    private CacheManager cacheManager = new MemoryCacheManager();

    private Cache<RepoPrCompositeKey, BuildList> buildListCache = ConstantUtils.getBuildListCache(cacheManager);

    private Cache<BuildResultCacheKey, TeamcityBuildResult> buildResultCache =
            ConstantUtils.getBuildResultCache(cacheManager);

    private Cache<PullRequestStateCacheKey, PullRequestState> pullRequestStateCache =
            ConstantUtils.getPullRequestStateCache(cacheManager);

    private CacheInfoServlet servlet;

    private StringWriter stringWriter;

    @Override
    protected void handleRequest() throws Exception {
        servlet.doGet(mockedReq, mockedBaseResp);
    }

    @Override
    protected void checkNoSideEffects() {
        assertTrue(stringWriter.toString().isEmpty());
    }

    @Before
    public void setUp() throws IOException {
        cacheManager.flushCaches();
        servlet = new CacheInfoServlet(cacheManager, commitService, pullRequestService, mockedRepositoryService);

        Commit commit = mock(Commit.class);
        when(commitService.getCommit(any(CommitRequest.class))).thenReturn(commit);
        when(commit.getId()).thenReturn(MERGE_HASH);

        when(pullRequest.getToRef().getRepository()).thenReturn(mockedRepository);
        when(pullRequest.getId()).thenReturn(PR_ID);
        when(pullRequestService.getById(REPO_ID, PR_ID)).thenReturn(pullRequest);

        when(mockedReq.getMethod()).thenReturn("GET");

        stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(mockedBaseResp.getWriter()).thenReturn(writer);
    }

    @Test
    public void testBadResponses() throws Exception {
        when(mockedReq.getPathInfo()).thenReturn("/");

        handleRequest();

        verify(mockedReq, atLeastOnce()).getPathInfo();
        checkNoSideEffects();
        assertEquals(HttpServletResponse.SC_NOT_FOUND, mockedBaseResp.getStatus());
    }

    @Test
    public void shouldPrintCacheInfo() throws Exception {
        String expectedResult = "Build List element: ConfigTCBuild(name=name, id=id, optional=false, " +
                "includedPaths=[], excludedPaths=[], includedTargetBranches=[], excludedTargetBranches=[])\n" +
                "Build Result List element: TeamcityBuildResult(status=SUCCESS, buildId=123, mergeHash=mergeHash, " +
                "statusMessage=Build successful)\n\n" + "Pull Request State: PullRequestState(status=OK, message=)";

        RepoPrCompositeKey repoPrCompositeKey = new RepoPrCompositeKey(REPO_ID, PR_ID);
        String configTCBuildId = "id";
        ConfigTCBuild configTCBuild = new ConfigTCBuild() {
            {
                setName("name");
                setId(configTCBuildId);
            }
        };
        BuildList buildList = new BuildList(MERGE_HASH, new ArrayList<ConfigTCBuild>() {{
            add(configTCBuild);
        }});
        buildListCache.put(repoPrCompositeKey, buildList);

        BuildResultCacheKey buildResultCacheKey = new BuildResultCacheKey(repoPrCompositeKey, configTCBuildId);
        TeamcityBuildResult teamcityBuildResult = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 123L, MERGE_HASH
                , ConstantUtils.SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKey, teamcityBuildResult);

        PullRequestStateCacheKey pullRequestStateCacheKey = new PullRequestStateCacheKey(pullRequest, MERGE_HASH);
        PullRequestState pullRequestState = new PullRequestState(PullRequestStatus.OK, "");
        pullRequestStateCache.put(pullRequestStateCacheKey, pullRequestState);

        handleRequest();

        assertEquals(expectedResult, stringWriter.toString());
        assertEquals(HttpServletResponse.SC_OK, mockedBaseResp.getStatus());
    }

    @Test
    public void shouldPrintEmptyCaches() throws Exception {
        String expectedResult = "Build List is null" + "\n" + "Pull Request State is null";

        handleRequest();

        assertEquals(expectedResult, stringWriter.toString());
        assertEquals(HttpServletResponse.SC_OK, mockedBaseResp.getStatus());
    }
}
