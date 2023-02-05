package com.yandex.bitbucket.plugin.buildmanager;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildList;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestState;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStateCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStatus;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.atlassian.bitbucket.pull.PullRequestState.DECLINED;
import static com.atlassian.bitbucket.pull.PullRequestState.MERGED;
import static com.atlassian.bitbucket.pull.PullRequestState.OPEN;
import static com.yandex.bitbucket.plugin.buildmanager.BuildListContextProvider.DATA_TRANSFER_KEY;
import static com.yandex.bitbucket.plugin.buildmanager.BuildListContextProvider.PULL_REQUEST_GET_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildListContextProviderTest {
    private static final String BUILD_TYPE_ID = "build_type_id";
    private static final String BUILD_NAME = "build_name";
    private static final String COMMIT_ID = "commit_id";
    private static final int REPO_ID = 1;
    private static final long PR_ID = 1L;
    private static final long BUILD_ID = 10L;

    @Mock
    private CacheManager mockedCacheManager;

    @Mock
    private CommitService mockedCommitService;

    @Mock
    private Commit commit;

    @Mock
    private Cache<RepoPrCompositeKey, BuildList> mockedBuildListCache;

    @Mock
    private Cache<BuildResultCacheKey, TeamcityBuildResult> mockedBuildResultCache;

    @Mock
    private Cache<PullRequestStateCacheKey, PullRequestState> mockedPullRequestStateCache;

    @Mock
    private Cache<PullRequestStateCacheKey, PullRequestState> mockedPullRequestErrorCache;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PullRequest mockedPullRequest;

    @Mock
    private Map<String, Object> mockedContext;

    private BuildListContextProvider contextProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockedPullRequest.getId()).thenReturn(PR_ID);
        when(mockedPullRequest.getState()).thenReturn(OPEN);
        when(mockedPullRequest.getToRef().getRepository().getId()).thenReturn(REPO_ID);
        when(mockedContext.get(eq(PULL_REQUEST_GET_KEY))).thenReturn(mockedPullRequest);
        when(ConstantUtils.getBuildResultCache(mockedCacheManager)).thenReturn(mockedBuildResultCache);
        when(ConstantUtils.getBuildListCache(mockedCacheManager)).thenReturn(mockedBuildListCache);
        when(ConstantUtils.getPullRequestStateCache(mockedCacheManager)).thenReturn(mockedPullRequestStateCache);
        when(ConstantUtils.getPullRequestErrorCache(mockedCacheManager)).thenReturn(mockedPullRequestErrorCache);
        when(mockedPullRequestErrorCache.get(any())).thenReturn(new PullRequestState(PullRequestStatus.ERROR, ""));
        when(mockedCommitService.getCommit(any(CommitRequest.class))).thenReturn(commit);
        when(commit.getId()).thenReturn(COMMIT_ID);
        contextProvider = new BuildListContextProvider(mockedCacheManager, mockedCommitService);
    }

    private void checkResultByCachedValue(BuildList cachedValue, String buildCountKey) {
        RepoPrCompositeKey repoPrCompositeKey = new RepoPrCompositeKey(REPO_ID, PR_ID);
        PullRequestStateCacheKey pullRequestStateCacheKey = new PullRequestStateCacheKey(repoPrCompositeKey, COMMIT_ID);
        when(mockedBuildListCache.get(repoPrCompositeKey)).thenReturn(cachedValue);

        Map<String, Object> response = contextProvider.getContextMap(mockedContext);

        InOrder inOrder = inOrder(mockedContext, mockedBuildListCache, mockedPullRequestErrorCache);
        inOrder.verify(mockedContext).get(eq(PULL_REQUEST_GET_KEY));
        inOrder.verify(mockedBuildListCache).get(eq(repoPrCompositeKey));
        inOrder.verify(mockedPullRequestErrorCache).get(eq(pullRequestStateCacheKey));
        inOrder.verifyNoMoreInteractions();
        verify(mockedPullRequest).getState();
        // We have 9 params in BuildListContextProvider result map
        assertEquals(9, response.size());
        assertTrue(response.containsKey(DATA_TRANSFER_KEY));
        if (cachedValue == null) {
            assertNull(response.get(DATA_TRANSFER_KEY));
        } else {
            List<ConfigTCBuild> cachedBuilds = cachedValue.getBuilds();
            for (ConfigTCBuild configTCBuild : cachedBuilds) {
                verify(mockedBuildResultCache).get(eq(new BuildResultCacheKey(REPO_ID, PR_ID, configTCBuild.getId())));
            }
            List<Map<String, Object>> responseList = (List<Map<String, Object>>) response.get(DATA_TRANSFER_KEY);
            for (int i = 0; i < cachedBuilds.size(); i++) {
                ConfigTCBuild cachedBuild = cachedBuilds.get(i);
                Map<String, Object> responseBuild = responseList.get(i);
                TeamcityBuildResult cachedBuildResult = mockedBuildResultCache.get(new BuildResultCacheKey(REPO_ID, PR_ID,
                        cachedBuild.getId()));

                assertEquals(5, responseBuild.size());
                assertEquals(cachedBuild.getName(), responseBuild.get("name"));
                assertEquals(cachedBuild.getId(), responseBuild.get("typeId"));
                if (cachedBuildResult != null) {
                    assertEquals(cachedBuildResult.getStatusMessage(), responseBuild.get("statusMessage"));
                }
                assertEquals(cachedBuildResult == null ? "NO INFO" : cachedBuildResult.getStatus().toString(), responseBuild.get(
                        "status"));
                assertEquals(cachedBuildResult == null || cachedBuildResult.getBuildId() == null ? "" : BUILD_ID,
                        responseBuild.get("id"));
                assertEquals(1, response.get(buildCountKey));
            }
        }
    }

    @Test
    public void cachedNull_null() {
        checkResultByCachedValue(null, null);
    }

    @Test
    public void cachedBuildListOfEmptyList_emptyList() {
        checkResultByCachedValue(new BuildList("revision", Collections.emptyList()), null);
    }

    private void checkResultByCachedBuildListOfBuildWithStatus(TeamcityBuildStatus status, String buildCountKey) {
        ConfigTCBuild build = new ConfigTCBuild();
        build.setName(BUILD_NAME);
        build.setId(BUILD_TYPE_ID);
        when(mockedBuildResultCache.get(eq(new BuildResultCacheKey(REPO_ID, PR_ID, BUILD_TYPE_ID)))).thenReturn(
                new TeamcityBuildResult(status, BUILD_ID, "merge_hash", "message"));
        checkResultByCachedValue(new BuildList("revision", Collections.singletonList(build)), buildCountKey);
    }

    @Test
    public void cachedBuildListOfBuildWithStatusError_thisBuild() {
        checkResultByCachedBuildListOfBuildWithStatus(TeamcityBuildStatus.ERROR, "errorNumberBuilds");
    }

    @Test
    public void cachedBuildListOfBuildWithStatusFailure_thisBuild() {
        checkResultByCachedBuildListOfBuildWithStatus(TeamcityBuildStatus.FAILURE, "failedNumberBuilds");
    }

    @Test
    public void cachedBuildListOfBuildWithStatusUnknown_thisBuild() {
        checkResultByCachedBuildListOfBuildWithStatus(TeamcityBuildStatus.UNKNOWN, "failedNumberBuilds");
    }

    @Test
    public void cachedBuildListOfBuildWithStatusRunning_thisBuild() {
        checkResultByCachedBuildListOfBuildWithStatus(TeamcityBuildStatus.RUNNING, "runningNumberBuilds");
    }

    @Test
    public void cachedBuildListOfBuildWithStatusSuccess_thisBuild() {
        checkResultByCachedBuildListOfBuildWithStatus(TeamcityBuildStatus.SUCCESS, "okNumberBuilds");
    }

    @Test
    public void pullRequestOpened_showsRebuildButtons() {
        when(mockedPullRequest.getState()).thenReturn(OPEN);
        Map<String, Object> response = contextProvider.getContextMap(mockedContext);
        assertEquals(true, response.get("isPullRequestOpened"));
    }

    @Test
    public void pullRequestDeclined_hidesRebuildButtons() {
        when(mockedPullRequest.getState()).thenReturn(DECLINED);
        Map<String, Object> response = contextProvider.getContextMap(mockedContext);
        assertEquals(false, response.get("isPullRequestOpened"));
    }

    @Test
    public void pullRequestMerged_hidesRebuildButtons() {
        when(mockedPullRequest.getState()).thenReturn(MERGED);
        Map<String, Object> response = contextProvider.getContextMap(mockedContext);
        assertEquals(false, response.get("isPullRequestOpened"));
    }
}
