package com.yandex.bitbucket.plugin.buildmanager.listener;

import com.atlassian.bitbucket.comment.CommentService;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.commit.CommitRequest;
import com.atlassian.bitbucket.commit.CommitService;
import com.atlassian.bitbucket.event.pull.PullRequestDeclinedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestDeletedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestMergedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestOpenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestReopenedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Ref;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.event.internal.AnnotatedMethodsListenerHandler;
import com.atlassian.event.internal.AsynchronousAbleEventDispatcher;
import com.atlassian.event.internal.DirectEventExecutorFactory;
import com.atlassian.event.internal.EventPublisherImpl;
import com.atlassian.event.internal.EventThreadPoolConfigurationImpl;
import com.atlassian.event.spi.EventDispatcher;
import com.atlassian.event.spi.EventExecutorFactory;
import com.atlassian.plugin.scope.ScopeManager;
import com.yandex.bitbucket.plugin.TestAppender;
import com.yandex.bitbucket.plugin.buildmanager.entity.BuildManagerPluginSettings;
import com.yandex.bitbucket.plugin.buildmanager.event.SettingsChangedEvent;
import com.yandex.bitbucket.plugin.utils.ConstantUtils;
import com.yandex.bitbucket.plugin.utils.api.SolomonAgentApiHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import ru.yandex.bitbucket.plugin.solomon.entity.SensorLabel;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PullRequestEventListenerTest {
    private static final int TIMEOUT = 100;
    private static final String FROM_HASH = "fromHash";
    private static final String TO_HASH = "toHash";
    private static final String MERGE_COMMIT = "merge_commit";

    @Mock
    private ScopeManager scopeManager;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private BuildManagerPluginSettings settings;

    @Mock
    private PullRequest pullRequest;

    @Mock
    private PullRequestRef pullRequestRefFrom;

    @Mock
    private PullRequestRef pullRequestRefTo;

    @Mock
    private Repository repository;

    @Mock
    private CommentService commentService;

    @Mock
    private CommitService commitService;

    @Mock
    private SolomonAgentApiHelper solomonAgentApiHelper;

    @Mock
    private Commit mergeCommit;

    private CacheManager cacheManager = new MemoryCacheManager();

    private Cache<String, String> mergeHashCache = ConstantUtils.getMergeHashCache(cacheManager);

    private PullRequestEventListener listener;

    private EventExecutorFactory eventExecutorFactory =
            new DirectEventExecutorFactory(new EventThreadPoolConfigurationImpl());

    private EventDispatcher eventDispatcher = new AsynchronousAbleEventDispatcher(eventExecutorFactory);

    private EventPublisher eventPublisher;

    @Before
    public final void setUp() {
        MockitoAnnotations.initMocks(this);
        when(repository.getId()).thenReturn(1);
        when(pullRequestRefTo.getRepository()).thenReturn(repository);
        when(pullRequest.getFromRef()).thenReturn(pullRequestRefFrom);
        when(pullRequest.getToRef()).thenReturn(pullRequestRefTo);
        when(commitService.getCommit(any(CommitRequest.class))).thenReturn(mergeCommit);
        when(mergeCommit.getId()).thenReturn(MERGE_COMMIT);
        listener = spy(new PullRequestEventListener(applicationEventPublisher, settings, cacheManager, commentService,
                commitService, solomonAgentApiHelper));
        eventPublisher = new EventPublisherImpl(eventDispatcher,
                () -> Collections.singletonList(new AnnotatedMethodsListenerHandler()), scopeManager);
        eventPublisher.register(listener);
        setEnabledWithSendingEvent(true);
    }

    @Test
    public void singleDeclinedEvent() {
        eventPublisher.publish(new PullRequestDeclinedEvent(new Object(), pullRequest));
        verify(listener, timeout(TIMEOUT)).onDeclinedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT).times(0)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void singleDeletedEvent() {
        eventPublisher.publish(new PullRequestDeletedEvent(new Object(), pullRequest));
        verify(listener, timeout(TIMEOUT)).onDeletedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT).times(0)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void singleMergedEvent() {
        eventPublisher.publish(new PullRequestMergedEvent(new Object(), pullRequest));
        verify(listener, timeout(TIMEOUT)).onMergedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT).times(0)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void singleOpenedEvent() {
        eventPublisher.publish(new PullRequestOpenedEvent(new Object(), pullRequest));
        verify(listener, timeout(TIMEOUT)).onOpenedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void singleReopenedEvent() {
        eventPublisher.publish(new PullRequestReopenedEvent(new Object(), pullRequest, FROM_HASH, TO_HASH));
        verify(listener, timeout(TIMEOUT)).onReopenedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void singleRescopedEvent() {
        eventPublisher.publish(new PullRequestRescopedEvent(new Object(), pullRequest, FROM_HASH, TO_HASH));
        verify(listener, timeout(TIMEOUT)).onRescopedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void singleUpdatedEvent() {
        eventPublisher.publish(new PullRequestUpdatedEvent(new Object(), pullRequest, "title", "description",
                mock(Ref.class)));
        verify(listener, timeout(TIMEOUT)).onUpdatedEvent(any());
        verify(solomonAgentApiHelper, timeout(TIMEOUT).times(0)).pushData(SensorLabel.MERGE_COMMIT_UPDATE, 1, pullRequest.getId());
    }

    @Test
    public void listensForSettingsChangedEvent() {
        assertTrue(listener instanceof ApplicationListener);
        try {
            listener.getClass().getMethod("onApplicationEvent", SettingsChangedEvent.class);
        } catch (NoSuchMethodException e) {
            fail(e.toString());
        }
    }

    // This test checks only PullRequestOpenedEvent, PullRequestReopenedEvent, PullRequestRescopedEvent
    @Test
    public void doesntListenIfDisabledForRepository() {
        final int METHODS = 3;
        int innerCalls = 0;
        setEnabledWithSendingEvent(false);
        callMethodsAndVerifyEnabled(never());
        setEnabledWithSendingEvent(true);
        callMethodsAndVerifyEnabled(times(METHODS + innerCalls));
        callMethodsAndVerifyEnabled(times(2 * METHODS + innerCalls));
        setEnabledWithSendingEvent(false);
        callMethodsAndVerifyEnabled(times(2 * METHODS + innerCalls));
    }

    private void setEnabledWithSendingEvent(boolean enabled) {
        if (enabled != settings.isEnabled(repository)) {
            listener.onApplicationEvent(new SettingsChangedEvent(new Object(), repository));
        }
        when(settings.isEnabled(repository)).thenReturn(enabled);
    }

    private void callMethodsAndVerifyEnabled(VerificationMode verificationMode) {
        listener.onOpenedEvent(new PullRequestOpenedEvent(new Object(), pullRequest));
        listener.onReopenedEvent(new PullRequestReopenedEvent(new Object(), pullRequest, FROM_HASH, TO_HASH));
        listener.onRescopedEvent(new PullRequestRescopedEvent(new Object(), pullRequest, FROM_HASH, TO_HASH));
        verify(applicationEventPublisher, verificationMode).publishEvent(any());
    }

    @Test
    public void testCache() {
        int calls = 1; // setEnabledWithSendingEvent was already called once in setup
        setEnabledWithSendingEvent(false);
        calls++;
        forceCallIsEnabledForPullRequest();
        verify(settings, times(++calls)).isEnabled(repository);
        for (int i = 0; i < 10; i++) {
            forceCallIsEnabledForPullRequest();
            verify(settings, times(calls)).isEnabled(repository);
        }
        setEnabledWithSendingEvent(true);
        calls++;
        forceCallIsEnabledForPullRequest();
        verify(settings, times(++calls)).isEnabled(repository);
        for (int i = 0; i < 10; i++) {
            forceCallIsEnabledForPullRequest();
            verify(settings, times(calls)).isEnabled(repository);
        }
    }

    private void forceCallIsEnabledForPullRequest() {
        listener.onOpenedEvent(new PullRequestOpenedEvent(new Object(), pullRequest));
    }

    @Test
    public void shouldSetMergeHashInCache() {
        setEnabledWithSendingEvent(true);
        TestAppender.clearEvents();
        String msgPart = "Current and previous merge hashes are equal";

        mockRefs("from_1", "to_1");
        listener.onOpenedEvent(new PullRequestOpenedEvent(new Object(), pullRequest));
        assertEquals(ConstantUtils.makePrFromToValue("from_1", "to_1"), mergeHashCache.get(MERGE_COMMIT));
        assertThat(TestAppender.getLastElement().getMessage(), containsString("Pull request opened"));

        TestAppender.clearEvents();
        mockRefs("from_2", "to_2");
        listener.onReopenedEvent(new PullRequestReopenedEvent(new Object(), pullRequest, FROM_HASH, TO_HASH));
        assertEquals(ConstantUtils.makePrFromToValue("from_2", "to_2"), mergeHashCache.get(MERGE_COMMIT));
        assertThat(TestAppender.getLastElement().getMessage(), containsString(msgPart));

        TestAppender.clearEvents();
        mockRefs("from_3", "to_3");
        listener.onRescopedEvent(new PullRequestRescopedEvent(new Object(), pullRequest, FROM_HASH, TO_HASH));
        assertEquals(ConstantUtils.makePrFromToValue("from_3", "to_3"), mergeHashCache.get(MERGE_COMMIT));
        assertThat(TestAppender.getLastElement().getMessage(), containsString(msgPart));
    }

    private void mockRefs(String from, String to) {
        when(pullRequest.getFromRef().getLatestCommit()).thenReturn(from);
        when(pullRequest.getToRef().getLatestCommit()).thenReturn(to);
    }
}
