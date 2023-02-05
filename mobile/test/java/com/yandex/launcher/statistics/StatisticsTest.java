package com.yandex.launcher.statistics;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.pm.PackageManager;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.common.permissions.Permissions;
import com.yandex.launcher.settings.HomescreenWidgetConfig;
import com.yandex.launcher.settings.HomescreenWidgetPart;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import androidx.annotation.NonNull;

public class StatisticsTest extends BaseRobolectricTest {

    private static final String PN_BUILTIN = "com.yandex.launcher";
    private static final String PN_EXTERNAL = "com.yandex.launcher.external";

    private Statistics statistics;

    public StatisticsTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        statistics = new TestStatistics().getSpy();
    }

    @Test
    public void onSettingsPermissionsDisplayed() {
        Statistics.onSettingsPermissions(false, false, false, false);
        verify(statistics).processEvent(
                StoryEvent.Events.EVENT_SETTINGS_PERMISSIONS_DISPLAYED,
                0,
                null);

        Statistics.onSettingsPermissions(false, true, true, true);
        verify(statistics).processEvent(
                StoryEvent.Events.EVENT_SETTINGS_PERMISSIONS_DISPLAYED,
                SettingsStory.PERMISSION_CONTACTS | SettingsStory.PERMISSION_LOCATION | SettingsStory.PERMISSION_SMS_EMAIL,
                null);
    }

    @Test
    public void onWeatherSettingsOpened() {
        HomescreenWidgetConfig config = new HomescreenWidgetConfig();

        config.setUseCelsius(true);
        config.setParts(Arrays.asList(HomescreenWidgetPart.CLOCK, HomescreenWidgetPart.WEATHER));
        Statistics.onSettingsWeatherOpened(config, false);
        WeatherWidgetChapter.InitialState state = new WeatherWidgetChapter.InitialState(
                false, SettingsStory.WeatherScale.CELSIUS, SettingsStory.WeatherType.CLOCK_WEATHER,
                SettingsStory.State.OFF, SettingsStory.State.OFF);
        verify(statistics).processEvent(StoryEvent.Events.EVENT_SETTINGS_WEATHER_WIDGET_OPENED, 0, state);

        config.setUseCelsius(false);
        config.setParts(Arrays.asList(HomescreenWidgetPart.WEATHER));
        Statistics.onSettingsWeatherOpened(config, true);
        state = new WeatherWidgetChapter.InitialState(
                true, SettingsStory.WeatherScale.FAHRENHEIT, SettingsStory.WeatherType.WEATHER,
                SettingsStory.State.OFF, SettingsStory.State.OFF);
        verify(statistics).processEvent(StoryEvent.Events.EVENT_SETTINGS_WEATHER_WIDGET_OPENED, 0, state);
    }

    @Test
    public void onWeatherDetailsNoLocationPermissions() {
        Statistics.onWeatherDetailsNoLocationPermissions();
        verify(statistics).processEvent(StoryEvent.Events.EVENT_WEATHER_DETAILS_NO_LOCATION_PERMISSION, 0, null);
    }

    @Test
    public void onWeatherDetailsEnableLocationPermissionTapped() {
        Statistics.onWeatherDetailsEnableLocationPermissionTapped();
        verify(statistics).processEvent(StoryEvent.Events.EVENT_WEATHER_DETAILS_ENABLE_LOCATION_PERMISSION_TAPPED, 0, null);
    }

    @Test
    public void setWeatherNoData() {
        Statistics.setWeatherNoData(true);
        verify(statistics).processEvent(StoryEvent.Events.EVENT_WEATHER_SET_NO_DATA, 0, true);

        Statistics.setWeatherNoData(false);
        verify(statistics).processEvent(StoryEvent.Events.EVENT_WEATHER_SET_NO_DATA, 0, false);
    }

    @Test
    public void onPermissionsOnboardingEnabled() {
        Statistics.onPermissionsOnboardingEnabled();
        verify(statistics).processEvent(StoryEvent.Events.EVENT_INTRO_PERMISSIONS_ONBOARDING_ENABLED, 0, null);
    }

    @Test
    public void onPermissionsOnboardingSkipped() {
        Statistics.onPermissionsOnboardingSkipped();
        verify(statistics).processEvent(StoryEvent.Events.EVENT_INTRO_PERMISSIONS_ONBOARDING_SKIPPED, 0, null);
    }

    @Test
    public void onPermissionsRequest() {
        final String origin = PermissionsStory.ORIGIN_INTRO;
        final String[] permissions = new String[] {Permissions.READ_EXTERNAL_STORAGE};
        final int[] granted = new int[] {PackageManager.PERMISSION_DENIED};

        Statistics.onPermissionsRequest(origin, permissions);
        final PermissionsStory.RequestData requestData = new PermissionsStory.RequestData(origin, permissions);
        verify(statistics).processEvent(StoryEvent.Events.EVENT_PERMISSION_REQUEST, 0, requestData);

        Statistics.onPermissionsResult(origin, permissions, granted);
        final PermissionsStory.ResultData resultData = new PermissionsStory.ResultData(origin, permissions, granted);
        verify(statistics).processEvent(StoryEvent.Events.EVENT_PERMISSION_RESULT, 0, resultData);
    }

    /**
     * ********** Inner Classes *********
     */

    private static class TestStatistics extends Statistics {

        private final TestStatistics spy;

        public TestStatistics() {
            spy = spy(this);
            instance = spy;
        }

        public TestStatistics getSpy() {
            return spy;
        }

        @Override
        protected void registerStory(@NonNull AbstractStory story) {
        }

        @Override
        protected void unregisterStory(@NonNull AbstractStory story) {
        }

        @Override
        protected void processEvent(@StoryEvent.Events int type, int param0, Object param1) {
        }

        @Override
        protected void processEventImmediately(int type, int param0, Object param1) {
            
        }

        @Override
        protected void processEventImmediatelySync(int type, int param0, Object param1) {

        }
    }
}
