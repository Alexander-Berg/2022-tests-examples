package com.yandex.bitbucket.plugin.buildmanager.listener;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.content.ContentService;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildList;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildResultCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.PullRequestStateCacheKey;
import com.yandex.bitbucket.plugin.buildmanager.entity.RepoPrCompositeKey;
import com.yandex.bitbucket.plugin.buildmanager.event.MergeCommitChangedEvent;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.utils.api.SolomonAgentApiHelper;
import com.yandex.bitbucket.plugin.utils.api.TeamcityApiHelper;
import com.yandex.bitbucket.plugin.utils.git.GitCommandHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.bitbucket.plugin.PluginProperties;
import ru.yandex.bitbucket.plugin.configprocessor.ConfigProcessor;
import ru.yandex.bitbucket.plugin.configprocessor.entity.ConfigTCBuild;
import ru.yandex.bitbucket.plugin.configprocessor.entity.Path;
import ru.yandex.bitbucket.plugin.solomon.entity.SensorLabel;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild.Revision;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuild.Revisions;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildResult;
import ru.yandex.bitbucket.plugin.teamcity.entity.TeamcityBuildStatus;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MergeCommitChangedEventListenerTest {
    private static final String TARGET_BRANCH_NAME = "master";
    private static final int REPO_ID = 1;
    private static final long PR_ID_1 = 2L;
    private static final long PR_ID_2 = 3L;
    private static final String MERGE_BRANCH_1 = ConstantUtils.makeShortMergeBranch(PR_ID_1);
    private static final String MERGE_BRANCH_2 = ConstantUtils.makeShortMergeBranch(PR_ID_2);
    private static final long BUILD_ID = 4L;
    private static final String REVISION = "revision";
    private static final String NEW_REVISION = "newRevision";
    private static final String BUILD_TYPE_ID = "BuildTypeIdTeamcity";
    private static final String EXCLUDED_PATH = "excludedPath";

    private static final BuildResultCacheKey CACHE_KEY_2 = new BuildResultCacheKey(REPO_ID, PR_ID_2, BUILD_TYPE_ID);

    private static Set<String> changesSet = new HashSet<String>() {{
        add("path");
        add("another_path");
    }};

    private static ConfigTCBuild configTCBuild = new ConfigTCBuild();

    private TeamcityBuildResult teamcityBuildResult = new TeamcityBuildResult(TeamcityBuildStatus.SUCCESS, BUILD_ID,
            REVISION, ConstantUtils.SUCCESSFUL_BUILD_STATUS_MESSAGE);

    private static Set<ConfigTCBuild> configTcBuildSet = new HashSet<>();

    private static List<ConfigTCBuild> configTCBuildList = new ArrayList<>(configTcBuildSet);

    @Mock
    private GitCommandHelper gitCommandHelper;
    @Mock
    private ContentService contentService;
    @Mock
    private CommitService commitService;
    @Mock
    private PullRequest pullRequest;
    @Mock
    private PullRequestRef ref;
    @Mock
    private Repository repository;
    @Mock
    private Commit commit;
    @Mock
    private ConfigProcessor configProcessor;
    @Mock
    private MergeCommitChangedEvent event;
    @Mock
    private ApplicationPropertiesService applicationPropertiesService;
    @Mock
    private TeamcityApiHelper teamcityApiHelper;
    @Mock
    private SolomonAgentApiHelper solomonAgentApiHelper;

    private PullRequestStateCacheKey pullRequestStateCacheKey;

    private MergeCommitChangedEventListener spyEventListener;

    private CacheManager cacheManager = new MemoryCacheManager();

    private Cache<RepoPrCompositeKey, BuildList> buildListCache = ConstantUtils.getBuildListCache(cacheManager);

    private Cache<BuildResultCacheKey, TeamcityBuildResult> buildResultCache = ConstantUtils.getBuildResultCache(cacheManager);

    @Before
    public void init() throws FileNotFoundException {
        MockitoAnnotations.initMocks(this);

        configTCBuild.setName("name");
        configTCBuild.setId(BUILD_TYPE_ID);
        configTCBuild.setExcludedPaths(Collections.singletonList(new Path(EXCLUDED_PATH)));
        configTcBuildSet.add(configTCBuild);

        when(event.getPullRequest()).thenReturn(pullRequest);
        when(pullRequest.getToRef()).thenReturn(ref);
        when(ref.getId()).thenReturn("refs/heads/" + TARGET_BRANCH_NAME);
        when(pullRequest.getId()).thenReturn(PR_ID_1);
        when(pullRequest.getFromRef()).thenReturn(ref);
        when(pullRequest.getToRef()).thenReturn(ref);
        when(ref.getRepository()).thenReturn(repository);
        when(ref.getRepository().getId()).thenReturn(REPO_ID);
        when(configProcessor.getBuildInfoByChangingPath(changesSet, TARGET_BRANCH_NAME)).thenReturn(configTcBuildSet);
        when(commitService.getCommit(any(CommitRequest.class))).thenReturn(commit);
        when(commit.getId()).thenReturn(REVISION);
        when(applicationPropertiesService.getPluginProperty(PluginProperties.TEAMCITY_OAUTH)).thenReturn("token");

        spyEventListener = Mockito.spy(new MergeCommitChangedEventListener(contentService, commitService, cacheManager,
                applicationPropertiesService, teamcityApiHelper, gitCommandHelper, solomonAgentApiHelper));
        doReturn(changesSet).when(spyEventListener).getPRChanges(pullRequest, REVISION);
        pullRequestStateCacheKey = new PullRequestStateCacheKey(pullRequest, REVISION);
        doReturn(configTcBuildSet).when(spyEventListener).getBuildList(pullRequest, pullRequestStateCacheKey, REVISION);
    }

    @Test
    public void handle_shouldFillCacheAndRunStartMethod() {
        spyEventListener.onApplicationEvent(event);
        Cache<RepoPrCompositeKey, BuildList> buildListCache = ConstantUtils.getBuildListCache(cacheManager);
        BuildList buildList = buildListCache.get(new RepoPrCompositeKey(REPO_ID, PR_ID_1));
        assertEquals(REVISION, buildList.getRevision());
        assertEquals(new ArrayList<>(configTcBuildSet), buildList.getBuilds());
        verify(spyEventListener).startBuildIfNeeded(REVISION, configTCBuild, pullRequest);
    }

    @Test
    public void handle_shouldSendMetrics() {
        Set<ConfigTCBuild> configTCBuilds = new HashSet<>();

        ConfigTCBuild configTCBuildStarted = new ConfigTCBuild();
        configTCBuildStarted.setId("true");
        configTCBuilds.add(configTCBuildStarted);
        ConfigTCBuild configTCBuildNotStarted = new ConfigTCBuild();
        configTCBuildNotStarted.setId("false");
        configTCBuilds.add(configTCBuildNotStarted);

        doReturn(true).when(spyEventListener).startBuildIfNeeded(REVISION, configTCBuildStarted, pullRequest);
        doReturn(false).when(spyEventListener).startBuildIfNeeded(REVISION, configTCBuildNotStarted, pullRequest);
        doReturn(configTCBuilds).when(spyEventListener).getBuildList(pullRequest, pullRequestStateCacheKey, REVISION);
        spyEventListener.onApplicationEvent(event);
        verify(solomonAgentApiHelper).pushData(SensorLabel.AFFECTED_BUILD, configTCBuilds.size(), PR_ID_1);
        verify(solomonAgentApiHelper, times(1)).pushData(SensorLabel.STARTED_BUILD, 1, PR_ID_1);
    }

    @Test
    public void shouldStartBuildNoHistory() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_1), new BuildList(REVISION, configTCBuildList));
        when(pullRequest.getId()).thenReturn(PR_ID_1);

        spyEventListener.startBuildIfNeeded(REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper).startTeamcityBuild(MERGE_BRANCH_1, BUILD_TYPE_ID, pullRequest, REVISION);
        verify(spyEventListener, never()).getDiffBetweenRevisions(anyString(), anyString(), any(Repository.class));
    }

    @Test
    public void shouldStartBuildOutdatedHistory() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(NEW_REVISION,
                configTCBuildList));
        buildResultCache.put(new BuildResultCacheKey(REPO_ID, PR_ID_2, BUILD_TYPE_ID), teamcityBuildResult);
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(changesSet).when(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);

        spyEventListener.startBuildIfNeeded(NEW_REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper).startTeamcityBuild(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, NEW_REVISION);
        verify(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);
    }

    @Test
    public void shouldNotStartBuildEmptyDiff() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(NEW_REVISION,
                configTCBuildList));
        buildResultCache.put(CACHE_KEY_2, teamcityBuildResult);
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(Collections.EMPTY_SET).when(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);

        assertEquals(REVISION, buildResultCache.get(CACHE_KEY_2).getMergeHash());

        spyEventListener.startBuildIfNeeded(NEW_REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper, never()).startTeamcityBuild(anyString(), anyString(), any(PullRequest.class), anyString());
        verify(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);
        assertEquals(NEW_REVISION, buildResultCache.get(CACHE_KEY_2).getMergeHash());
    }

    @Test
    public void shouldNotStartBuildPreviousBuildIsSatisfied() {
        Set<String> changesSetExcluded = new HashSet<String>() {{
            add(EXCLUDED_PATH);
        }};
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(NEW_REVISION,
                configTCBuildList));
        buildResultCache.put(CACHE_KEY_2, teamcityBuildResult);
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(changesSetExcluded).when(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);

        assertEquals(REVISION, buildResultCache.get(CACHE_KEY_2).getMergeHash());

        spyEventListener.startBuildIfNeeded(NEW_REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper, never()).startTeamcityBuild(anyString(), anyString(), any(PullRequest.class), anyString());
        verify(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);
        assertEquals(NEW_REVISION, buildResultCache.get(CACHE_KEY_2).getMergeHash());
    }

    @Test
    public void shouldStartBuildWrongPrIdInKey() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(REVISION, configTCBuildList));
        buildResultCache.put(new BuildResultCacheKey(REPO_ID, PR_ID_1, BUILD_TYPE_ID), teamcityBuildResult);
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(changesSet).when(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);

        spyEventListener.startBuildIfNeeded(REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper).startTeamcityBuild(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, REVISION);
        verify(spyEventListener, never()).getDiffBetweenRevisions(anyString(), anyString(), any(Repository.class));
    }

    @Test
    public void shouldStartBuildWrongBuildTypeIdInKey() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_1), new BuildList(REVISION, configTCBuildList));
        buildResultCache.put(new BuildResultCacheKey(REPO_ID, PR_ID_1, "wrongBuildTypeId"), teamcityBuildResult);
        when(pullRequest.getId()).thenReturn(PR_ID_1);

        doReturn(changesSet).when(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);

        spyEventListener.startBuildIfNeeded(REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper).startTeamcityBuild(MERGE_BRANCH_1, BUILD_TYPE_ID, pullRequest, REVISION);
        verify(spyEventListener, never()).getDiffBetweenRevisions(anyString(), anyString(), any(Repository.class));
    }

    @Test
    public void shouldGetBuildFromTeamcityEqualRevision() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(REVISION, configTCBuildList));
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(teamcityBuildResult).when(spyEventListener).getLastTeamcityBuildByBranchAndBuildType(pullRequest, REVISION,
                MERGE_BRANCH_2, configTCBuild);

        spyEventListener.startBuildIfNeeded(REVISION, configTCBuild, pullRequest);
        verify(teamcityApiHelper, never()).startTeamcityBuild(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, REVISION);
        verify(spyEventListener, never()).getDiffBetweenRevisions(anyString(), anyString(), any(Repository.class));
        assertEquals(teamcityBuildResult, buildResultCache.get(CACHE_KEY_2));
    }

    @Test
    public void shouldGetBuildFromTeamcityOutdatedRevisionNoChanges() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(NEW_REVISION, configTCBuildList));
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(teamcityBuildResult).when(spyEventListener).getLastTeamcityBuildByBranchAndBuildType(pullRequest, NEW_REVISION,
                MERGE_BRANCH_2, configTCBuild);

        spyEventListener.startBuildIfNeeded(NEW_REVISION, configTCBuild, pullRequest);
        verify(spyEventListener).getDiffBetweenRevisions(anyString(), anyString(), any(Repository.class));
        verify(teamcityApiHelper, never()).startTeamcityBuild(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, NEW_REVISION);
    }

    @Test
    public void shouldGetBuildFromTeamcityAndStartAgainOutdatedRevisionWithChanges() {
        buildListCache.put(new RepoPrCompositeKey(REPO_ID, PR_ID_2), new BuildList(NEW_REVISION, configTCBuildList));
        when(pullRequest.getId()).thenReturn(PR_ID_2);

        doReturn(teamcityBuildResult).when(spyEventListener).getLastTeamcityBuildByBranchAndBuildType(pullRequest, NEW_REVISION,
                MERGE_BRANCH_2, configTCBuild);
        doReturn(changesSet).when(spyEventListener).getDiffBetweenRevisions(NEW_REVISION, REVISION, repository);

        spyEventListener.startBuildIfNeeded(NEW_REVISION, configTCBuild, pullRequest);
        verify(spyEventListener).getDiffBetweenRevisions(anyString(), anyString(), any(Repository.class));
        verify(teamcityApiHelper).startTeamcityBuild(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, NEW_REVISION);
    }

    @Test
    public void getNullBuildFromTeamcity() {
        when(teamcityApiHelper.getLastTeamcityBuildByBranchAndBuildType(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, REVISION))
                .thenReturn(null);

        TeamcityBuildResult teamcityBuildResult = spyEventListener
                .getLastTeamcityBuildByBranchAndBuildType(pullRequest, REVISION, MERGE_BRANCH_2, configTCBuild);
        assertNull(teamcityBuildResult);
    }

    @Test
    public void getBuildWithNullRevisionFromTeamcity() {
        TeamcityBuild teamcityBuild = new TeamcityBuild();
        teamcityBuild.setRevisions(new Revisions());
        when(teamcityApiHelper.getLastTeamcityBuildByBranchAndBuildType(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, REVISION))
                .thenReturn(teamcityBuild);

        TeamcityBuildResult teamcityBuildResult = spyEventListener
                .getLastTeamcityBuildByBranchAndBuildType(pullRequest, REVISION, MERGE_BRANCH_2, configTCBuild);
        assertNull(teamcityBuildResult);
    }

    @Test
    public void getBuildFromTeamcity() {
        TeamcityBuild teamcityBuild = new TeamcityBuild();
        Revision revision = new Revision(REVISION);
        teamcityBuild.setRevisions(new Revisions(1L, Collections.singletonList(revision)));
        teamcityBuild.setStatus(TeamcityBuildStatus.SUCCESS);
        teamcityBuild.setStatusText(ConstantUtils.SUCCESSFUL_BUILD_STATUS_MESSAGE);
        teamcityBuild.setId(BUILD_ID);
        when(teamcityApiHelper.getLastTeamcityBuildByBranchAndBuildType(MERGE_BRANCH_2, BUILD_TYPE_ID, pullRequest, REVISION))
                .thenReturn(teamcityBuild);

        TeamcityBuildResult teamcityBuildResultFromTC = spyEventListener
                .getLastTeamcityBuildByBranchAndBuildType(pullRequest, REVISION, MERGE_BRANCH_2, configTCBuild);
        assertEquals(teamcityBuildResult, teamcityBuildResultFromTC);
    }
}