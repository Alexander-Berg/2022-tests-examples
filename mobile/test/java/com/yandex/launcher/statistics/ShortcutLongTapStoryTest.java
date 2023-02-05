package com.yandex.launcher.statistics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.eq;

import android.content.ComponentName;

import com.yandex.launcher.BaseRobolectricTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ShortcutLongTapStoryTest extends BaseRobolectricTest {
    private static final String MOCK_PACKAGE_1 = "com.mock1.yandex.test";

    private static final String CLOSE_COLOR_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"nothing\","
            + "\"place\":{\"all_apps\":{\"color\":{\"position\":\"4:1\",\"name\":\"GRAY\"}}}}";
    private static final String SELECT_SHORTCUT_IN_COLOR_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":{\"click_dropdown_element\":{\"intent\":\"intent_string\",\"title_string\":\"this_is_title\"}},"
            + "\"place\":{\"all_apps\":{\"color\":{\"position\":\"4:1\",\"name\":\"GRAY\"}}}}";
    private static final String CLOSE_HOMESCREEN_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"nothing\","
            + "\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:2\",\"number\":\"3\"}}}}";
    private static final String INFO_HOMESCREEN_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"app_info\","
            + "\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:2\",\"number\":\"3\"}}}}";
    private static final String REMOVE_HOMESCREEN_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"app_shortcut_remove\","
            + "\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:2\",\"number\":\"3\"}}}}";
    private static final String DELETE_HOMESCREEN_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"app_delete\","
            + "\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:2\",\"number\":\"3\"}}}}";
    private static final String NOTIFICATION_CLICK_HOMESCREEN_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"notification_click\","
            + "\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:2\",\"number\":\"3\"}}}}";
    private static final String NOTIFICATION_SWIPE_HOMESCREEN_JSON = "{\"package_name\":\"" + MOCK_PACKAGE_1 + "\","
            + "\"whats_next\":\"notification_swipe\","
            + "\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:2\",\"number\":\"3\"}}}}";

    public ShortcutLongTapStoryTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void shouldSendNothingActionIfClosedInColorCategory() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo colorInfo = buildColorInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_ALL_APPS_OPEN,
                0, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_COLOR_SELECTED,
                0, "GRAY"));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_ALL_APPS, colorInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, colorInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(CLOSE_COLOR_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendNothingActionIfClosedInHomescreen() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo homescreenInfo = buildHomescreenInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, 3, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_WORKSPACE_0 + 2, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(CLOSE_HOMESCREEN_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendInfoActionIfClosedInHomescreen() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo homescreenInfo = buildHomescreenInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, 3, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_WORKSPACE_0 + 2, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLICK_APP_INFO, 0, null));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(INFO_HOMESCREEN_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendDeleteActionIfDeleteSelectedInHomescreen() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo homescreenInfo = buildHomescreenInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, 3, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_WORKSPACE_0 + 2, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLICK_APP_DELETE, 0, null));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(DELETE_HOMESCREEN_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendRemoveActionIfDeleteSelectedInHomescreen() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo homescreenInfo = buildHomescreenInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, 3, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_WORKSPACE_0 + 2, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLICK_SHORTCUT_REMOVE, 0, null));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(REMOVE_HOMESCREEN_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendNotificationClickActionIfNotificationSelectedInHomescreen() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo homescreenInfo = buildHomescreenInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, 3, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_WORKSPACE_0 + 2, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_NOTIFICATION_CLICK, 0, null));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(NOTIFICATION_CLICK_HOMESCREEN_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendNotificationSwipeActionIfNotificationSwipedInHomescreen() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo homescreenInfo = buildHomescreenInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, 3, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_WORKSPACE_0 + 2, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, homescreenInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_NOTIFICATION_SWIPE_DISMISS, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(NOTIFICATION_SWIPE_HOMESCREEN_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }

    @Test
    public void shouldSendShortcutSelectedActionIfPressedShortcutInColorCategory() throws JSONException {
        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(),
                Mockito.anyString());

        ShortcutLongTapStory story = new ShortcutLongTapStory(getAppContext());
        story.setManager(storyManager);

        ItemAnalyticsInfo colorInfo = buildColorInfo();
        ItemAnalyticsInfo shortcutAnalyticsInfo = new ItemAnalyticsInfo();

        shortcutAnalyticsInfo.setIntentString("intent_string");
        shortcutAnalyticsInfo.setTitle("this_is_title");

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_ALL_APPS_OPEN,
                0, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_COLOR_SELECTED,
                0, "GRAY"));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_ALL_APPS, colorInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_OPEN, 0, colorInfo));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_APP_START, Statistics.POSITION_SHORTCUTS_POPUP, shortcutAnalyticsInfo));
        story.onEvent(StoryEvent.create(
                StoryEvent.Events.EVENT_SHORTCUTS_POPUP_CLOSE, 0, null));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(
                eq(ShortcutLongTapStory.EVENT_APP_LONG_TAP),
                argumentCaptor.capture());

        JSONObject obj = new JSONObject(argumentCaptor.getValue());
        JSONObject expectedObj = new JSONObject(SELECT_SHORTCUT_IN_COLOR_JSON);

        assertThat(obj.toString(), is(expectedObj.toString()));
    }


    private ItemAnalyticsInfo buildColorInfo() {
        ItemAnalyticsInfo itemAnalyticsInfo = new ItemAnalyticsInfo();
        itemAnalyticsInfo.setComponentName(new ComponentName(MOCK_PACKAGE_1, ""));
        itemAnalyticsInfo.setMainContainerName(ItemAnalyticsInfo.MAIN_CONTAINER_ALLAPPS);
        itemAnalyticsInfo.setContainerPartType(ItemAnalyticsInfo.PART_COLOR);
        itemAnalyticsInfo.setContainerPart(ItemAnalyticsInfo.NAME_KEY, "GRAY");
        itemAnalyticsInfo.setPositionX(3);
        itemAnalyticsInfo.setPositionY(0);
        return itemAnalyticsInfo;
    }

    private ItemAnalyticsInfo buildHomescreenInfo() {
        ItemAnalyticsInfo itemAnalyticsInfo = new ItemAnalyticsInfo();
        itemAnalyticsInfo.setComponentName(new ComponentName(MOCK_PACKAGE_1, ""));
        itemAnalyticsInfo.setMainContainerName(ItemAnalyticsInfo.MAIN_CONTAINER_HOMESCREENS);
        itemAnalyticsInfo.setContainerPart(ItemAnalyticsInfo.NUMBER_KEY, "3");
        itemAnalyticsInfo.setPositionX(2);
        itemAnalyticsInfo.setPositionY(1);
        return itemAnalyticsInfo;
    }
}
