package com.yandex.launcher.statistics;

import android.content.ComponentName;
import android.content.Context;

import com.yandex.launcher.BaseRobolectricTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class AppActionStoryTest extends BaseRobolectricTest {

    private static final String RUN_COLOR_JSON = "{\"packageName\":\"com.mock1.yandex.test\",\"place\":{\"all_apps\":{\"color\":{\"position\":\"3:0\",\"name\":\"black\"}}},\"settings\":{\"theme\":\"\"},\"action\":\"run\"}";
    private static final String DELETE_FOLDER_JSON = "{\"packageName\":\"com.mock2.yandex.test\",\"place\":{\"homescreens\":{\"simple_folder\":{\"position\":\"3:0\",\"number\":\"2\"}}},\"settings\":{\"theme\":\"\"},\"action\":\"delete\"}";
    private static final String MOVE_SHTORKA_JSON = "{\"packageName\":\"com.mock1.yandex.test\",\"place\":{\"shtorka\":{\"recently\":{\"position\":\"3:0\"}}},\"settings\":{\"theme\":\"\"},\"action\":\"move\"}";

    private static final String MOCK_PACKAGE_1 = "com.mock1.yandex.test";
    private static final String MOCK_PACKAGE_2 = "com.mock2.yandex.test";

    public AppActionStoryTest() throws NoSuchFieldException, IllegalAccessException {
    }

    @Test
    public void processAppLaunchEventTest(){
        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getString(Mockito.anyInt(),Mockito.anyString(),Mockito.anyString())).thenReturn("3:0");

        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());

        AppActionStory story = new AppActionStory(context);
        story.setManager(storyManager);

        ItemAnalyticsInfo runColorInfo = buildRunColorInfo();
        StoryEvent storyEvent = StoryEvent.create(StoryEvent.Events.EVENT_APP_START, 0, runColorInfo);

        story.onEvent(storyEvent);
        story.reportAction("run", story.getAccumulatedInfo(), 0, 0);
        Mockito.verify(storyManager, Mockito.atLeastOnce()).sendRemoteJson(Mockito.anyString(), Mockito.anyString());
    }

//    @Test
    public void buildJsonTest() throws JSONException {
        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getString(Mockito.anyInt(),Mockito.anyString(),Mockito.anyString())).thenReturn("3:0");

        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());

        AppActionStory story = new AppActionStory(context);
        story.setManager(storyManager);

        ItemAnalyticsInfo runColorInfo = buildRunColorInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_ALL_APPS_OPEN, Statistics.POSITION_ALL_APPS, "black"));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_COLOR_SELECTED, Statistics.POSITION_ALL_APPS, "black"));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_APP_START, Statistics.POSITION_ALL_APPS, runColorInfo));
        String runJson = story.buildJson(story.getAccumulatedInfo(), 0, 0);
        // clear name
        JSONObject jsonObject = new JSONObject(runJson);

        jsonObject.remove(AppActionStory.TIME_KEY);
        assertThat(jsonObject.toString(), is(RUN_COLOR_JSON));

        ItemAnalyticsInfo deleteFolderInfo = buildDeleteFolderInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SCREEN_SELECTED, Statistics.POSITION_WORKSPACE_0, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_FOLDER_SIMPLE, deleteFolderInfo));
        String deleteJson = story.buildJson(story.getDragInfo(), 0, 0);

        jsonObject = new JSONObject(deleteJson);
        jsonObject.remove(AppActionStory.TIME_KEY);
        assertThat(jsonObject.toString(), is(DELETE_FOLDER_JSON));

        ItemAnalyticsInfo moveShtorkaInfo = buildMoveShtorkaInfo();

        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_SEARCH_OPEN, Statistics.POSITION_WORKSPACE_0, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_SHTORKA_RECENT, moveShtorkaInfo));
        String moveJson = story.buildJson(story.getDragInfo(), 0, 0);

        jsonObject = new JSONObject(moveJson);
        jsonObject.remove(AppActionStory.TIME_KEY);
        assertThat(jsonObject.toString(), is(MOVE_SHTORKA_JSON));

    }

    private ItemAnalyticsInfo buildRunColorInfo(){
        ItemAnalyticsInfo itemAnalyticsInfo = new ItemAnalyticsInfo();
        itemAnalyticsInfo.setComponentName(new ComponentName(MOCK_PACKAGE_1, ""));
        itemAnalyticsInfo.setMainContainerName(ItemAnalyticsInfo.MAIN_CONTAINER_ALLAPPS);
        itemAnalyticsInfo.setContainerPartType(ItemAnalyticsInfo.PART_COLOR);
        itemAnalyticsInfo.setContainerPart(ItemAnalyticsInfo.NAME_KEY,"black");
        itemAnalyticsInfo.setPositionX(3);
        itemAnalyticsInfo.setPositionY(0);
        return itemAnalyticsInfo;
    }

    private ItemAnalyticsInfo buildDeleteFolderInfo(){
        ItemAnalyticsInfo itemAnalyticsInfo = new ItemAnalyticsInfo();
        itemAnalyticsInfo.setComponentName(new ComponentName(MOCK_PACKAGE_2, ""));
        itemAnalyticsInfo.setContainerPartType(ItemAnalyticsInfo.PART_SIMPLE_FOLDER);
        itemAnalyticsInfo.setContainerPart(ItemAnalyticsInfo.NUMBER_KEY,"2");
        itemAnalyticsInfo.setPositionX(3);
        itemAnalyticsInfo.setPositionY(0);
        return itemAnalyticsInfo;
    }

    private ItemAnalyticsInfo buildMoveShtorkaInfo(){
        ItemAnalyticsInfo itemAnalyticsInfo = new ItemAnalyticsInfo();
        itemAnalyticsInfo.setComponentName(new ComponentName(MOCK_PACKAGE_1, ""));
        itemAnalyticsInfo.setContainerPartType(ItemAnalyticsInfo.PART_RECENTLY);
        itemAnalyticsInfo.setPositionX(3);
        itemAnalyticsInfo.setPositionY(0);
        return itemAnalyticsInfo;
    }

    private AppActionStory buildStory(){
        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getString(Mockito.anyInt(),Mockito.anyString(),Mockito.anyString())).thenReturn("3:0");

        StoryManager storyManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());

        AppActionStory story = new AppActionStory(context);
        story.setManager(storyManager);
        return story;
    }

    private ItemAnalyticsInfo buildInfo(){
        final ItemAnalyticsInfo mockAnalyticsInfo = new ItemAnalyticsInfo();
        mockAnalyticsInfo.setComponentName(new ComponentName(MOCK_PACKAGE_1, ""));
        mockAnalyticsInfo.setPosition(1, 1);
        return mockAnalyticsInfo;
    }

    /**
     * We shouldn't change main place while app start. Because of we'll get wrong time in json.
     * @throws JSONException
     */
    @Test
    public void clickFromShtorkaWithAnotherContainerSet() throws JSONException {
        final AppActionStory story = buildStory();
        final ItemAnalyticsInfo mockAnalyticsInfo = buildInfo();
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WORKSPACE_OPEN, 0, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_APP_START, Statistics.POSITION_SHTORKA_RECENT, mockAnalyticsInfo));
        String json = story.buildJson(story.getAccumulatedInfo(), 0, 0);
        Assert.assertTrue(!json.contains(ItemAnalyticsInfo.MAIN_CONTAINER_SHTORKA));
    }

    /**
     * We shouldn't change main place while drag start. Because of we'll get wrong time in json.
     * @throws JSONException
     */
    @Test
    public void dragFromShtorkaWithAnotherContainerSet() throws JSONException {
        final AppActionStory story = buildStory();
        final ItemAnalyticsInfo mockAnalyticsInfo = buildInfo();
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_WORKSPACE_OPEN, 0, null));
        story.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_DRAG_PRE_START, Statistics.POSITION_SHTORKA_RECENT, mockAnalyticsInfo));
        String json = story.buildJson(story.getAccumulatedInfo(), 0, 0);
        Assert.assertTrue(!json.contains(ItemAnalyticsInfo.MAIN_CONTAINER_SHTORKA));
    }
}
