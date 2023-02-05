package com.yandex.bitbucket.plugin.buildmanager.listener;


import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildList;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import com.yandex.bitbucket.plugin.buildmanager.event.CheckBuildListStatusEvent;
import com.yandex.bitbucket.plugin.buildmanager.event.MergeCommitChangedEvent;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.utils.api.TeamcityApiHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildState;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CheckBuildListStatusEventListenerTest {
    private final String MERGE_BRANCH = "1/merge";
    private final long PR_ID = 1L;
    private final long BUILD_ID = 123L;
    private final int REPO_ID = 2;
    private static final String COMMIT_ID = "1bjsdh823s";

    @Mock
    private CommitService commitService;
    @Mock(answer = RETURNS_DEEP_STUBS)
    private PullRequest pullRequest;
    @Mock
    private Commit commit;
    @Mock
    private CheckBuildListStatusEvent event;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    @Mock
    private TeamcityApiHelper teamcityApiHelper;

    private CheckBuildListStatusEventListener eventListener;

    private CacheManager cacheManager = new MemoryCacheManager();

    private Cache<RepoPrCompositeKey, BuildList> buildListCache = ConstantUtils.getBuildListCache(cacheManager);

    private Cache<BuildResultCacheKey, TeamcityBuildResult> buildResultCache =
            ConstantUtils.getBuildResultCache(cacheManager);

    private RepoPrCompositeKey repoPrCompositeKey = new RepoPrCompositeKey(REPO_ID, PR_ID);
    private String configTCBuildId = "id";
    private ConfigTCBuild configTCBuild = new ConfigTCBuild() {
        {
            setName("name");
            setId(configTCBuildId);
        }
    };
    private BuildResultCacheKey buildResultCacheKey = new BuildResultCacheKey(repoPrCompositeKey, configTCBuildId);
    private TeamcityBuildResult teamcityBuildResult = new TeamcityBuildResult(TeamcityBuildStatus.RUNNING, BUILD_ID,
            COMMIT_ID, ConstantUtils.RUNNING_BUILD_STATUS_MESSAGE);

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);

        when(event.getPullRequest()).thenReturn(pullRequest);
        when(pullRequest.getId()).thenReturn(PR_ID);
        when(commitService.getCommit(any(CommitRequest.class))).thenReturn(commit);
        when(commit.getId()).thenReturn(COMMIT_ID);
        when(pullRequest.getToRef().getRepository().getId()).thenReturn(REPO_ID);

        eventListener = new CheckBuildListStatusEventListener(commitService, cacheManager, applicationEventPublisher,
                teamcityApiHelper);
    }

    @Test
    public void buildListIsNull() {
        eventListener.onApplicationEvent(event);
        verify(applicationEventPublisher).publishEvent(any(MergeCommitChangedEvent.class));
    }

    @Test
    public void buildResultIsNull() {
        buildListCache.put(repoPrCompositeKey, new BuildList(COMMIT_ID, Collections.singletonList(configTCBuild)));
        eventListener.onApplicationEvent(event);
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
        verify(teamcityApiHelper).startTeamcityBuild(MERGE_BRANCH, configTCBuildId, pullRequest, COMMIT_ID);
    }

    @Test
    public void teamcityBuildOutdatedStatus() {
        TeamcityBuild teamcityBuild = new TeamcityBuild();
        teamcityBuild.setState(TeamcityBuildState.finished);
        teamcityBuild.setStatus(TeamcityBuildStatus.SUCCESS);
        teamcityBuild.setId(BUILD_ID);
        when(teamcityApiHelper.getTeamcityBuildById(BUILD_ID, pullRequest, COMMIT_ID)).thenReturn(teamcityBuild);

        buildListCache.put(repoPrCompositeKey, new BuildList(COMMIT_ID, Collections.singletonList(configTCBuild)));
        buildResultCache.put(buildResultCacheKey, teamcityBuildResult);

        eventListener.onApplicationEvent(event);
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
        verify(teamcityApiHelper, never()).startTeamcityBuild(MERGE_BRANCH, configTCBuildId, pullRequest, COMMIT_ID);
        assertEquals(TeamcityBuildStatus.SUCCESS, buildResultCache.get(buildResultCacheKey).getStatus());
    }

    @Test
    public void teamcityBuildUpToDateStatus() {
        TeamcityBuild teamcityBuild = new TeamcityBuild();
        teamcityBuild.setState(TeamcityBuildState.finished);
        teamcityBuild.setStatus(TeamcityBuildStatus.RUNNING);
        teamcityBuild.setId(BUILD_ID);
        when(teamcityApiHelper.getTeamcityBuildById(BUILD_ID, pullRequest, COMMIT_ID)).thenReturn(teamcityBuild);

        buildListCache.put(repoPrCompositeKey, new BuildList(COMMIT_ID, Collections.singletonList(configTCBuild)));
        buildResultCache.put(buildResultCacheKey, teamcityBuildResult);

        eventListener.onApplicationEvent(event);
        verify(applicationEventPublisher, never()).publishEvent(any(MergeCommitChangedEvent.class));
        verify(teamcityApiHelper, never()).startTeamcityBuild(MERGE_BRANCH, configTCBuildId, pullRequest, COMMIT_ID);
        assertEquals(TeamcityBuildStatus.RUNNING, buildResultCache.get(buildResultCacheKey).getStatus());
    }
}
