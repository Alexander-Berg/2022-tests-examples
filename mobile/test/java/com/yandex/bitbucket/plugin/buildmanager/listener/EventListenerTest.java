package com.yandex.bitbucket.plugin.buildmanager.listener;

import com.atlassian.bitbucket.pull.PullRequest;
import com.yandex.bitbucket.plugin.buildmanager.event.MergeCommitChangedEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Mockito.verify;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EventListenerTest {
    @Autowired
    private ApplicationEventPublisher publisher;

    @Mock
    private PullRequest pullRequest;

    @MockBean
    private MergeCommitChangedEventListener mergeCommitChangedEventListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void listenMergeCommitChangedEvent() {
        MergeCommitChangedEvent event = new MergeCommitChangedEvent(this, pullRequest);
        publisher.publishEvent(event);

        verify(mergeCommitChangedEventListener).onApplicationEvent(event);
    }
}
