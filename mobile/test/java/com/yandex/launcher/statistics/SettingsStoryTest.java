package com.yandex.launcher.statistics;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.icons.IconProviderId;
import com.yandex.launcher.icons.IconProviderInfo;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static com.yandex.launcher.statistics.SettingsStory.OpenTrigger;
import static com.yandex.launcher.statistics.StoryEvent.Events;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SettingsStoryTest extends BaseRobolectricTest {

    private static final String ESCAPE = "\"%s\"";
    private static final String DUMMY_FILL = "REPLACEABLE";
    private static final String DUMMY_REPLACE = "\"REPLACEABLE\"";

    private static final String EVENT = "settings";

    private static final String NEXT_PERMISSIONS = "permissions_opened";
    private static final String NEXT_GRID = "grid_opened";
    private static final String NEXT_EFFECTS = "effects_opened";
    private static final String NEXT_SEARCH = "search_opened";
    private static final String NEXT_ICONS = "icons_opened";
    private static final String NEXT_BADGES = "badges_opened";
    private static final String NEXT_ALLAPPS = "allapps_button";
    private static final String NEXT_WIDGET = "widget_opened";
    private static final String NEXT_HOMESCREEN = "desktops_opened";
    private static final String NEXT_WEATHER = "weather_opened";

    private final AbstractStory story = new SettingsStory();

    public SettingsStoryTest() {
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        story.setManager(mock(StoryManager.class));
    }

    private void finishStory() {
        story.onEvent(StoryEvent.create(Events.EVENT_SETTINGS_CLOSE, 0, null));
    }

    private void repeatEvent(int times, @Events int event) {
        for (int i = 0; i < times; i++) {
            story.onEvent(StoryEvent.create(event, 0, null));
        }
    }

    @Test
    public void settingsOpenEvent() {
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_OPEN,
                0,
                OpenTrigger.LONG_TAP
        ));
        finishStory();

        verify(story.storyManager).sendRemoteJson(
                EVENT,
                "{\"settings_opened\":{\"method\":\"longtap\"}}"
        );

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_OPEN,
                0,
                OpenTrigger.APP
        ));
        finishStory();

        verify(story.storyManager).sendRemoteJson(
                EVENT,
                "{\"settings_opened\":{\"method\":\"app\"}}"
        );

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_OPEN,
                0,
                OpenTrigger.BACK
        ));
        finishStory();

        verify(story.storyManager).sendRemoteJson(
                EVENT,
                "{\"settings_opened\":{\"method\":\"back\"}}"
        );

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_OPEN,
                0,
                OpenTrigger.HARDWARE_BUTTON
        ));
        finishStory();

        verify(story.storyManager).sendRemoteJson(
                EVENT,
                "{\"settings_opened\":{\"method\":\"hardware_button\"}}"
        );

    }

    @Test
    public void badges() throws JSONException {
        // no badges, no whatsapp, no changes
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_BADGES_DIALOG_DISPLAYED,
                getBadgesMask(false, false),
                null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_BADGES_DIALOG_CLOSED,
                getBadgesMask(false, false),
                null
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_BADGES,
                "{\"method\":\"settings\",\"icons\":\"off\",\"sms_email\":\"off\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // no badges, changes
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_BADGES_DIALOG_DISPLAYED,
                getBadgesMask(false, false),
                null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_BADGES_DIALOG_CLOSED,
                getBadgesMask(true, true),
                null
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_BADGES,
                "{\"method\":\"settings\",\"icons\":{\"off\":\"on\"},\"sms_email\":{\"off\":\"on\"}}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void permissionsNotChanged() throws JSONException {
        // no permissions
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_DISPLAYED,
                getPermissionsMask(false, false, false),
                null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_CLOSED,
                getPermissionsMask(false, false, false),
                null
        ));
        finishStory();
        
        String verifyString = getWhatsNextJsonString(NEXT_PERMISSIONS,
                "{\"method\":\"settings\",\"location\":\"off\",\"contacts\":\"off\",\"sms_email\":\"off\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // some permissions
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_INTRO, 0, null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_DISPLAYED,
                getPermissionsMask(true, false, true),
                null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_CLOSED,
                getPermissionsMask(true, false, true),
                null
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_PERMISSIONS,
                "{\"method\":\"intro\",\"location\":\"on\",\"contacts\":\"off\",\"sms_email\":\"on\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void permissionsChanged() throws JSONException {
        // every permission granted
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_DISPLAYED,
                getPermissionsMask(false, false, false),
                null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_CLOSED,
                getPermissionsMask(true, true, true),
                null
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_PERMISSIONS,
                "{\"method\":\"settings\",\"location\":{\"off\":\"on\"},\"contacts\":{\"off\":\"on\"},\"sms_email\":{\"off\":\"on\"}}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // some permissions granted
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_DISPLAYED,
                getPermissionsMask(false, true, false),
                null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_INTRO, 0, null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_PERMISSIONS_CLOSED,
                getPermissionsMask(false, true, true),
                null
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_PERMISSIONS,
                "{\"method\":\"intro\",\"location\":\"off\",\"contacts\":\"on\",\"sms_email\":{\"off\":\"on\"}}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }
    
    private int getPermissionsMask(boolean locationOn, boolean contactsOn, boolean smsEmailOn) {
        int mask = 0;
        if (locationOn) {
            mask |= SettingsStory.PERMISSION_LOCATION;
        }
        if (contactsOn) {
            mask |= SettingsStory.PERMISSION_CONTACTS;
        }
        if (smsEmailOn) {
            mask |= SettingsStory.PERMISSION_SMS_EMAIL;
        }
        return mask;
    }

    private int getBadgesMask(boolean icons, boolean smsEmail) {
        int mask = 0;
        if (icons) {
            mask |= SettingsNotificationsChapter.BADGES_ICONS;
        }
        if (smsEmail) {
            mask |= SettingsNotificationsChapter.BADGES_SMS_EMAIL;
        }
        return mask;
    }

    @Test
    public void grid() throws JSONException {
        // grid changed
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_GRID_OPENED, 0, new int[] {5, 5}
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_GRID_CHANGED, 0, new int[] {6, 6}
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(
                NEXT_GRID, "{\"5:5\":\"6:6\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // grid not changed
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_GRID_OPENED, 0, new int[] {3, 3}
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(
                NEXT_GRID, "3:3"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // grid not changed
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_GRID_OPENED, 0, new int[] {4, 4}
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_GRID_CHANGED, 0, new int[] {4, 4}
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(
                NEXT_GRID, "4:4"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

    }

    @Test
    public void effects() throws JSONException {
        // effects no change
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_EFFECT_OPENED, 0, SettingsStory.EffectType.SMOOTH
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(
                NEXT_EFFECTS, "smooth"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_EFFECT_OPENED, 0, SettingsStory.EffectType.JELLY
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_EFFECT_CHANGED, 0, SettingsStory.EffectType.JELLY
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(
                NEXT_EFFECTS, "jelly"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        //effects changed
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_EFFECT_OPENED, 0, SettingsStory.EffectType.CAROUSEL
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_EFFECT_CHANGED, 0, SettingsStory.EffectType.ROTATION
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(
                NEXT_EFFECTS, "{\"carousel\":\"rotation\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void search() throws JSONException {
        // search no change
        final SettingsStory.SearchState state = new SettingsStory.SearchState(
                SettingsStory.SearchType.YANDEX, "top", true
        );
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_SEARCH_SHOWN, 0, state
        ));

        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_SEARCH,
                "{\"provider\":\"yandex\",\"show_search\":\"on\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);


        final SettingsStory.SearchState state2 = new SettingsStory.SearchState(
                SettingsStory.SearchType.BING, "above_dock", false
        );
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_SEARCH_SHOWN, 0, state2
        ));

        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_SEARCH,
                "{\"provider\":\"bing\",\"show_search\":\"off\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        final SettingsStory.SearchState state3 = new SettingsStory.SearchState(
                SettingsStory.SearchType.GOOGLE, "top", true
        );
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_SEARCH_SHOWN, 0, state3
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_SEARCH_CHANGED, 0, SettingsStory.SearchType.GOOGLE
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_SEARCH_POSITION_CHANGED, 0, "below_dock"
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_SEARCH_ON, 1, null
        ));

        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_SEARCH,
                "{\"provider\":\"google\",\"show_search\":\"on\",\"change_position\":{\"before\":\"top\",\"after\":\"below_dock\"}}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void icons() throws JSONException {
        final String testPackage = "com.test.package";
        final IconProviderInfo ipClassic = new IconProviderInfo(IconProviderId.createClassic(), null, null);
        final IconProviderInfo ipPillow = new IconProviderInfo(IconProviderId.createPillow(), null, null);
        final IconProviderInfo ipExternal = new IconProviderInfo(IconProviderId.createExternal(testPackage), null, null);

        // icons no change
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_SHOWN, 0, ipClassic
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_ICONS,
                "{\"classic\":\"\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);


        // icons changed

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_SHOWN, 0, ipClassic
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_CHANGED, 0, ipPillow
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_ICONS,
                "{\"classic\":\"square\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_SHOWN, 0, ipPillow
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_CHANGED, 0, ipExternal
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_ICONS,
                "{\"square\":\""+testPackage+"\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_SHOWN, 0, ipPillow
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_MORE, 0, null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ICON_CHANGED, 0, ipExternal
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_ICONS,
                "{\"square\":\""+testPackage+"\",\"more_clicked\":\"\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

    }

    @Test
    public void allAppsSettings() throws JSONException {
        // change button image
        String verifyString = getWhatsNextJsonString(NEXT_ALLAPPS, "{\"item\":\"shape\",\"value\":\"preset\"}");
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_ALLAPPS_BTN_PRESET, 0, "preset"
        ));
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // hide button
        story.onEvent(StoryEvent.create(Events.EVENT_SETTINGS_ALLAPPS_BTN_SHOWN, 0, null));
        verifyString = getWhatsNextJsonString(NEXT_ALLAPPS,
                "{\"item\":\"show_button\",\"value\":\"false\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // show button
        story.onEvent(StoryEvent.create(Events.EVENT_SETTINGS_ALLAPPS_BTN_SHOWN, 1, null));
        verifyString = getWhatsNextJsonString(NEXT_ALLAPPS,
                "{\"item\":\"show_button\",\"value\":\"true\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void widgets() throws JSONException {

        // widget set
        final String packageName = "com.widget.test";
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WIDGET_OPENED, Statistics.PLACE_WORKSPACE, null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WIDGET_SET, 0, packageName
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_WIDGET,
                "{\"method\":\"settings\",\"widget_set\":\""+packageName+"\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void homescreenConfigurator() throws JSONException {
        // homescreens no change
        HomeScreensConfiguratorChapter.InitState state =
                new HomeScreensConfiguratorChapter.InitState(1, true, false);
        story.onEvent(StoryEvent.create(
                Events.EVENT_HOME_SCREENS_CONFIG_OPEN, 0, state
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_HOMESCREEN, "{\"default\":\"1\",\"zen\":\"on\",\"round_screen\":\"off\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // homescreens full changes
        state = new HomeScreensConfiguratorChapter.InitState(5, false, true);
        story.onEvent(StoryEvent.create(
                Events.EVENT_HOME_SCREENS_CONFIG_OPEN, 0, state
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_HOME_SCREENS_CONFIG_ZEN_TOGGLE, 1, null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_HOME_SCREENS_CONFIG_SET_DEFAULT, -1, null
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_HOME_SCREENS_CONFIG_INFINITE_SCROLL_TOGGLE, 0, null
        ));
        repeatEvent(3, Events.EVENT_HOME_SCREENS_CONFIG_ADD);
        repeatEvent(2, Events.EVENT_HOME_SCREENS_CONFIG_REMOVE);
        story.onEvent(StoryEvent.create(
                Events.EVENT_HOME_SCREENS_CONFIG_RESTORE, 0, null
        ));
        repeatEvent(4, Events.EVENT_HOME_SCREENS_CONFIG_MOVE);
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_HOMESCREEN,
                "{\"default\":{\"5\":\"-1\"},\"zen\":{\"off\":\"on\"},\"round_screen\":{\"on\":\"off\"},\"add_desktop\":\"3\",\"dragdrop\":\"4\",\"delete\":\"2\",\"restore\":\"1\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void weather() throws JSONException {
        // weather no change
        WeatherWidgetChapter.InitialState state = new WeatherWidgetChapter.InitialState(
                false, SettingsStory.WeatherScale.CELSIUS, SettingsStory.WeatherType.CLOCK_WEATHER,
                SettingsStory.State.OFF, SettingsStory.State.OFF);

        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WEATHER_WIDGET_OPENED, 0, state
        ));
        finishStory();

        String verifyString = getWhatsNextJsonString(NEXT_WEATHER,
                "{\"method\":\"settings\",\"type\":\"clock_weather\",\"scale\":\"celsius\",\"alarm\":\"off\",\"date\":\"off\"}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);

        // full change
        state = new WeatherWidgetChapter.InitialState(
                true, SettingsStory.WeatherScale.FAHRENHEIT, SettingsStory.WeatherType.CLOCK,
                SettingsStory.State.OFF, SettingsStory.State.ON);
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WEATHER_WIDGET_OPENED, 0, state
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WEATHER_TYPE, 0, SettingsStory.WeatherType.WEATHER
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WEATHER_SCALE, 0, SettingsStory.WeatherScale.CELSIUS
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WEATHER_ALARM, 0, SettingsStory.State.ON
        ));
        story.onEvent(StoryEvent.create(
                Events.EVENT_SETTINGS_WEATHER_DATE, 0, SettingsStory.State.OFF
        ));
        finishStory();

        verifyString = getWhatsNextJsonString(NEXT_WEATHER,
                "{\"method\":\"widget\",\"type\":{\"clock\":\"weather\"},\"scale\":{\"fahrenheit\":\"celsius\"},\"alarm\":{\"off\":\"on\"},\"date\":{\"on\":\"off\"}}"
        );
        verify(story.storyManager).sendRemoteJson(EVENT, verifyString);
    }

    @Test
    public void selfVerify() throws JSONException {

        Assert.assertEquals(
                "{\"test\":\"test\"}",
                getWhatsNextJsonString("test", "test")
        );

        Assert.assertEquals(
                "{\"permissions\":{\"location\":\"off\"}}",
                getWhatsNextJsonString("permissions", "{\"location\":\"off\"}")
        );

    }

    private String getWhatsNextJsonString(String name, String value) throws JSONException {
        final JSONObject whatsNextJson = new JSONObject();
        whatsNextJson.put(name, DUMMY_FILL);
        return whatsNextJson.toString().replace(DUMMY_REPLACE, escapeJsonString(value));
    }

    private String escapeJsonString(String json) {
        if (json.isEmpty() || !json.substring(0,1).equals("{")) {
            return String.format(ESCAPE, json);
        }
        return json;
    }

}
