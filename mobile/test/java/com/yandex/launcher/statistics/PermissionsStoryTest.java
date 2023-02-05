package com.yandex.launcher.statistics;

import android.content.pm.PackageManager;
import com.yandex.launcher.BaseRobolectricTest;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class PermissionsStoryTest extends BaseRobolectricTest {

    private static final String ROOT = "permissions";

    private final AbstractStory story = new PermissionsStory();

    public PermissionsStoryTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        story.setManager(mock(StoryManager.class));
    }

    @Test
    public void testMultiplePermissions() {
        final String[] permissions = new String[] {"permission1", "permission2", "permission3", "permission4"};
        final String origin = "origin";
        final PermissionsStory.RequestData request = new PermissionsStory.RequestData(origin, permissions);
        final int[] granted = new int[] {PackageManager.PERMISSION_DENIED, PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_GRANTED, PackageManager.PERMISSION_DENIED};
        final PermissionsStory.ResultData result = new PermissionsStory.ResultData(origin, permissions, granted);
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_REQUEST, 0, request
        ));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_RESULT, 0, result
        ));
        verify(story.storyManager).sendRemoteJson(ROOT, "{\"origin\":{\"permission1\":\"denied\",\"permission2\":\"granted\",\"permission3\":\"granted\",\"permission4\":\"denied\"}}");
    }

    @Test
    public void testSinglePermission() {
        final String[] permissions = new String[] {"permission"};
        final String origin = "intro";
        final PermissionsStory.RequestData request = new PermissionsStory.RequestData(origin, permissions);
        final int[] granted = new int[] {PackageManager.PERMISSION_GRANTED};
        final PermissionsStory.ResultData result = new PermissionsStory.ResultData(origin, permissions, granted);
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_REQUEST, 0, request
        ));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_RESULT, 0, result
        ));
        verify(story.storyManager).sendRemoteJson(ROOT, "{\"intro\":{\"permission\":\"granted\"}}");
    }

    @Test
    public void testDifferentOrigins() {
        final String[] permissions = new String[] {"permission"};
        final PermissionsStory.RequestData request = new PermissionsStory.RequestData("origin1", permissions);
        final int[] granted = new int[] {PackageManager.PERMISSION_GRANTED};
        final PermissionsStory.ResultData result = new PermissionsStory.ResultData("origin2", permissions, granted);
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_REQUEST, 0, request
        ));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_RESULT, 0, result
        ));

        verifyNoInteractions(story.storyManager);
    }

    @Test
    public void testEmptyPermissions() {
        final String[] permissions = new String[] {};
        final PermissionsStory.RequestData request = new PermissionsStory.RequestData("origin", permissions);
        final int[] granted = new int[] {PackageManager.PERMISSION_GRANTED};
        final PermissionsStory.ResultData result = new PermissionsStory.ResultData("origin", permissions, granted);
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_REQUEST, 0, request
        ));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_PERMISSION_RESULT, 0, result
        ));

        verifyNoInteractions(story.storyManager);
    }

}
