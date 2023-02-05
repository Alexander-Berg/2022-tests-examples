package com.yandex.bitbucket.plugin.buildmanager;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.hook.repository.PreRepositoryHookContext;
import com.atlassian.bitbucket.hook.repository.PullRequestMergeHookRequest;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildList;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildManagerPluginSettings;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestState;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStateCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStatus;
import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import com.yandex.bitbucket.plugin.buildmanager.event.CheckBuildListStatusEvent;
import com.yandex.bitbucket.plugin.buildmanager.event.MergeCommitChangedEvent;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;
import org.springframework.context.ApplicationEventPublisher;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import java.util.ArrayList;
import java.util.List;

import static com.yandex.bitbucket.plugin.buildmanager.BuildManagerMergeCheck.ACCEPTED;
import static com.yandex.bitbucket.plugin.buildmanager.BuildManagerMergeCheck.PENDING;
import static com.yandex.bitbucket.plugin.buildmanager.BuildManagerMergeCheck.PLUGIN_DISABLED;
import static com.yandex.bitbucket.plugin.buildmanager.BuildManagerMergeCheck.REJECTED;
import static com.yandex.bitbucket.plugin.utils.ConstantUtils.RUNNING_BUILD_STATUS_MESSAGE;
import static com.yandex.bitbucket.plugin.utils.ConstantUtils.SUCCESSFUL_BUILD_STATUS_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BuildManagerMergeCheckTest {
    private static final String OUTDATED_MERGE_HASH = "outdated_merge_hash";
    private static final String UP_TO_DATE_MERGE_HASH = "up_to_date_merge_hash";
    private static final long PR_ID = 1;
    private static final int REPO_ID = 1;

    @Mock
    private PreRepositoryHookContext mockedContext;

    @Mock
    private CommitService mockedCommitService;

    @Mock
    private Commit mockedMergeCommit;

    @Mock
    private PullRequestMergeHookRequest mockedRequest;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private PullRequest mockedPullRequest;

    @Mock
    private Repository mockedRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BuildManagerPluginSettings mockedSettings;

    private CacheManager cacheManager = new MemoryCacheManager();
    private Cache<RepoPrCompositeKey, BuildList> buildListCache = ConstantUtils.getBuildListCache(cacheManager);
    private Cache<BuildResultCacheKey, TeamcityBuildResult> buildResultCache = ConstantUtils.getBuildResultCache(cacheManager);
    private Cache<PullRequestStateCacheKey, PullRequestState> pullRequestStateCache = ConstantUtils.getPullRequestStateCache(cacheManager);
    private Cache<PullRequestStateCacheKey, PullRequestState> pullRequestErrorCache = ConstantUtils.getPullRequestErrorCache(cacheManager);
    private Cache<RepoPrCompositeKey, String> skipChecksOnceCache = ConstantUtils.getSkipChecksOnceCache(cacheManager);
    private Cache<Long, Boolean> buildCheckCache = ConstantUtils.getBuildCheckCache(cacheManager);

    private BuildManagerMergeCheck mergeCheck;

    private PullRequestStateCacheKey pullRequestStateCacheKey;

    private static RepoPrCompositeKey repoPrCompositeKey = new RepoPrCompositeKey(REPO_ID, PR_ID);

    private static List<String> configTCBuildIds = new ArrayList<>();

    private static List<ConfigTCBuild> configTCBuilds = new ArrayList<>();

    private static List<BuildResultCacheKey> buildResultCacheKeys = new ArrayList<>();

    private static BuildList buildList;

    @BeforeClass
    public static void testDataInit() {
        Boolean[] optional = {null, false, true};
        for (int i = 0; i < 3; i++) {
            String configTcBuildId = "id_" + i;
            configTCBuildIds.add(configTcBuildId);
            ConfigTCBuild configTCBuild = new ConfigTCBuild();
            configTCBuild.setName("name");
            configTCBuild.setId(configTCBuildIds.get(i));
            configTCBuild.setOptional(optional[i]);
            configTCBuilds.add(configTCBuild);
            BuildResultCacheKey buildResultCacheKey = new BuildResultCacheKey(repoPrCompositeKey, configTcBuildId);
            buildResultCacheKeys.add(buildResultCacheKey);
        }
        buildList = new BuildList(UP_TO_DATE_MERGE_HASH, configTCBuilds);
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mockedCommitService.getCommit(any(CommitRequest.class))).thenReturn(mockedMergeCommit);
        when(mockedPullRequest.getId()).thenReturn(PR_ID);
        when(mockedPullRequest.getToRef().getRepository()).thenReturn(mockedRepository);
        when(mockedRepository.getId()).thenReturn(REPO_ID);
        when(mockedRequest.getPullRequest()).thenReturn(mockedPullRequest);
        when(mockedSettings.isEnabled(same(mockedRepository))).thenReturn(true);
        mergeCheck = new BuildManagerMergeCheck(cacheManager, mockedCommitService, applicationEventPublisher, mockedSettings);
        pullRequestStateCacheKey = new PullRequestStateCacheKey(mockedPullRequest, UP_TO_DATE_MERGE_HASH);
    }

    @Test
    public void checksMustBeSkipped_accepted() {
        skipChecksOnceCache.put(repoPrCompositeKey, UP_TO_DATE_MERGE_HASH);
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);
        pullRequestStateCache.put(pullRequestStateCacheKey, new PullRequestState(PullRequestStatus.ERROR, ""));

        assertSame(ACCEPTED, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void checksMustNotBeSkipped_doesNotSkipChecks() {
        skipChecksOnceCache.put(repoPrCompositeKey, OUTDATED_MERGE_HASH);
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);
        pullRequestStateCache.put(pullRequestStateCacheKey, new PullRequestState(PullRequestStatus.ERROR, ""));

        assertSame(PENDING, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void pluginDisabled_showSpecialMessage() {
        when(mockedSettings.isEnabled(same(mockedRepository))).thenReturn(false);

        assertSame(PLUGIN_DISABLED, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void pendingBuildResultIsOutdated() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);


        TeamcityBuildResult teamcityBuildResult = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 123L, OUTDATED_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(0), teamcityBuildResult);

        assertSame(PENDING, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher).publishEvent(any(MergeCommitChangedEvent.class));

        assertEquals(new PullRequestState(PullRequestStatus.PENDING, ""), pullRequestStateCache.get(pullRequestStateCacheKey));
    }

    @Test
    public void rejectedErrorCacheIsNotEmpty() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);
        pullRequestErrorCache.put(pullRequestStateCacheKey, new PullRequestState(PullRequestStatus.ERROR, "ERROR"));

        assertSame(REJECTED, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void pendingBuildListCacheIsEmpty() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        assertSame(PENDING, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void pendingBuildResultIsNull() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);

        assertSame(PENDING, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void pendingRunningBuild() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);
        TeamcityBuildResult teamcityBuildResult_1 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 1L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(0), teamcityBuildResult_1);
        TeamcityBuildResult teamcityBuildResult_2 = new TeamcityBuildResult(TeamcityBuildStatus.RUNNING, 2L, UP_TO_DATE_MERGE_HASH,
                RUNNING_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(1), teamcityBuildResult_2);

        assertSame(PENDING, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
        verify(applicationEventPublisher).publishEvent(any(CheckBuildListStatusEvent.class));
        assertTrue(buildCheckCache.containsKey(teamcityBuildResult_2.getBuildId()));
    }

    @Test
    public void pendingRunningBuildCachedStatus() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);
        TeamcityBuildResult teamcityBuildResult_1 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 1L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(0), teamcityBuildResult_1);
        TeamcityBuildResult teamcityBuildResult_2 = new TeamcityBuildResult(TeamcityBuildStatus.RUNNING, 2L, UP_TO_DATE_MERGE_HASH,
                RUNNING_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(1), teamcityBuildResult_2);

        buildCheckCache.put(teamcityBuildResult_2.getBuildId(), true);


        assertSame(PENDING, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
        verify(applicationEventPublisher, never()).publishEvent(any(CheckBuildListStatusEvent.class));
    }

    @Test
    public void rejectedFailedBuild() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);
        TeamcityBuildResult teamcityBuildResult_1 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 1L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(0), teamcityBuildResult_1);
        TeamcityBuildResult teamcityBuildResult_2 = new TeamcityBuildResult(TeamcityBuildStatus.FAILURE, 2L, UP_TO_DATE_MERGE_HASH,
                ConstantUtils.makeFailedBuildStatusMessage("message"));
        buildResultCache.put(buildResultCacheKeys.get(1), teamcityBuildResult_2);

        assertSame(REJECTED, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void acceptedSuccessBuild() {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);
        TeamcityBuildResult teamcityBuildResult_1 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 1L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(0), teamcityBuildResult_1);
        TeamcityBuildResult teamcityBuildResult_2 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 2L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(1), teamcityBuildResult_2);
        TeamcityBuildResult teamcityBuildResult_3 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 3L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(2), teamcityBuildResult_3);

        assertSame(ACCEPTED, mergeCheck.preUpdate(mockedContext, mockedRequest));
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void skipOptionalErrorBuildOutdatedHash() {
        assertOptionalAccepted(TeamcityBuildStatus.ERROR, OUTDATED_MERGE_HASH);
    }

    @Test
    public void skipOptionalFailureBuildOutdatedHash() {
        assertOptionalAccepted(TeamcityBuildStatus.FAILURE, OUTDATED_MERGE_HASH);
    }

    @Test
    public void skipOptionalUnknownBuildOutdatedHash() {
        assertOptionalAccepted(TeamcityBuildStatus.UNKNOWN, OUTDATED_MERGE_HASH);
    }

    @Test
    public void assertOptionalAcceptedOutdatedHash() {
        assertOptionalAccepted(null, OUTDATED_MERGE_HASH);
    }

    @Test
    public void skipOptionalErrorBuildUpToDateHash() {
        assertOptionalAccepted(TeamcityBuildStatus.ERROR, UP_TO_DATE_MERGE_HASH);
    }

    @Test
    public void skipOptionalFailureBuildUpToDateHash() {
        assertOptionalAccepted(TeamcityBuildStatus.FAILURE, UP_TO_DATE_MERGE_HASH);
    }

    @Test
    public void skipOptionalUnknownBuildUpToDateHash() {
        assertOptionalAccepted(TeamcityBuildStatus.UNKNOWN, UP_TO_DATE_MERGE_HASH);
    }

    @Test
    public void assertOptionalAcceptedUpToDateHash() {
        assertOptionalAccepted(null, UP_TO_DATE_MERGE_HASH);
    }

    private void assertOptionalAccepted(TeamcityBuildStatus status, String mergeHash) {
        when(mockedMergeCommit.getId()).thenReturn(UP_TO_DATE_MERGE_HASH);

        buildListCache.put(repoPrCompositeKey, buildList);
        TeamcityBuildResult teamcityBuildResult_1 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 1L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(0), teamcityBuildResult_1);
        TeamcityBuildResult teamcityBuildResult_2 = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, 2L, UP_TO_DATE_MERGE_HASH,
                SUCCESSFUL_BUILD_STATUS_MESSAGE);
        buildResultCache.put(buildResultCacheKeys.get(1), teamcityBuildResult_2);
        if (status != null) {
            TeamcityBuildResult teamcityBuildResult_3 = new TeamcityBuildResult(status, 3L, mergeHash, "message");
            buildResultCache.put(buildResultCacheKeys.get(2), teamcityBuildResult_3);
        }

        assertSame(ACCEPTED, mergeCheck.preUpdate(mockedContext, mockedRequest));
        VerificationMode verificationMode;

        if (mergeHash.equals(OUTDATED_MERGE_HASH) || status == null) {
            verificationMode = times(1);
        } else {
            verificationMode = never();
        }
        verify(applicationEventPublisher, verificationMode).publishEvent(any(MergeCommitChangedEvent.class));
    }
}
