package com.yandex.launcher.statistics;

import android.content.Context;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.di.ApplicationModule;
import com.yandex.launcher.di.Injector;
import com.yandex.launcher.time.TimeProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import dagger.Module;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class WeatherWidgetStoryTest extends BaseRobolectricTest {

    private static final String EVENT = "weather";
    private static final String ADVANCED_SCREEN_PARAM = "advanced_screen";
    private static final String NO_DATA_PARAM = "no_data";

    private AbstractStory story;

    private final TimeProvider timeProvider = mock(TimeProvider.class);

    public WeatherWidgetStoryTest() throws NoSuchFieldException, IllegalAccessException {
        Injector.setModule(new TestModule(getAppContext()));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        story = new WeatherWidgetStory();
        story.setManager(mock(StoryManager.class));
    }

    @Test
    public void shouldSendAdvancedScreenNoLocationPermissionEvent() {
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_DETAILS_NO_LOCATION_PERMISSION, 0, null));
        verify(story.storyManager).sendRemoteEvent(EVENT, ADVANCED_SCREEN_PARAM, "no_location_permission");
    }

    @Test
    public void shouldSendAdvancedScreenEnableLocationPermissionTappedEvent() {
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_DETAILS_ENABLE_LOCATION_PERMISSION_TAPPED, 0, null));
        verify(story.storyManager).sendRemoteEvent(EVENT, ADVANCED_SCREEN_PARAM, "enable_location_permission_tapped");
    }

    @Test
    public void shouldSendNoDataNoWeatherEvent() {
        when(timeProvider.currentTimeMillis()).thenReturn(100L);
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_SET_NO_DATA, 0, false));
        when(timeProvider.currentTimeMillis()).thenReturn(TimeUnit.MINUTES.toMillis(40));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_CHECK_NO_DATA, 0, null));
        verify(story.storyManager).sendRemoteEvent(EVENT, NO_DATA_PARAM, "no_weather");
    }

    @Test
    public void shouldSendNoDataNoLocationPermissionEvent() {
        when(timeProvider.currentTimeMillis()).thenReturn(100L);
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_SET_NO_DATA, 0, true));
        when(timeProvider.currentTimeMillis()).thenReturn(TimeUnit.MINUTES.toMillis(40));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_CHECK_NO_DATA, 0, null));
        verify(story.storyManager).sendRemoteEvent(EVENT, "no_data", "no_location_permission");
    }

    @Test
    public void shouldNotSendNoDataEventIfIntervalNotPassed() {
        when(timeProvider.currentTimeMillis()).thenReturn(100L);
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_SET_NO_DATA, 0, false));
        when(timeProvider.currentTimeMillis()).thenReturn(TimeUnit.MINUTES.toMillis(20));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WEATHER_CHECK_NO_DATA, 0, null));
        verifyNoInteractions(story.storyManager);
    }


    /**
     * ********** Inner Classes *********
     */

    @Module
    class TestModule extends ApplicationModule {

        public TestModule(Context context) {
            super(context);
        }

        @Override
        public TimeProvider provideTimeProvider() {
            return timeProvider;
        }
    }
}
