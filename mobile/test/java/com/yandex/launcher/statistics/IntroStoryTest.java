package com.yandex.launcher.statistics;

import com.yandex.launcher.BaseRobolectricTest;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class IntroStoryTest extends BaseRobolectricTest {

    private static final String PERMISSIONS_ONBOARDING = "permissions_onboarding";

    private final AbstractStory story = new IntroStory();

    public IntroStoryTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        story.setManager(mock(StoryManager.class));
    }

    @Test
    public void shouldSendPermissionsOnboardingEnabledEvent() {
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_INTRO_PERMISSIONS_ONBOARDING_ENABLED, 0, null));
        verify(story.storyManager).sendRemoteEvent(PERMISSIONS_ONBOARDING, "enabled", "");
    }

    @Test
    public void shouldSendPermissionsOnboardingSkippedEvent() {
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_INTRO_PERMISSIONS_ONBOARDING_SKIPPED, 0, null));
        verify(story.storyManager).sendRemoteEvent(PERMISSIONS_ONBOARDING, "skipped", "");
    }
}
