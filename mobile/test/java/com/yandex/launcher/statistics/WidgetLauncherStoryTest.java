package com.yandex.launcher.statistics;

import android.content.ComponentName;
import android.content.Context;

import com.yandex.launcher.BaseRobolectricTest;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class WidgetLauncherStoryTest extends BaseRobolectricTest {

    private static final String RUN_WIDGET_JSON = "{\"packageName\":\"com.mock1.yandex.test\",\"className\":\".SomeClass\",\"time\":1000,\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:0\",\"number\":\"0\"}}},\"settings\":{\"theme\":\"black\"},\"action\":\"run\",\"size\":\"4:2\"}";
    private static final String MOVE_WIDGET_JSON = "{\"packageName\":\"com.mock1.yandex.test\",\"className\":\".SomeClass\",\"time\":1000,\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:0\",\"number\":\"0\"}}},\"settings\":{\"theme\":\"black\"},\"action\":\"move\",\"size\":\"4:2\"}";
    private static final String DELETE_WIDGET_JSON = "{\"packageName\":\"com.mock1.yandex.test\",\"className\":\".SomeClass\",\"time\":1000,\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:0\",\"number\":\"0\"}}},\"settings\":{\"theme\":\"black\"},\"action\":\"delete\",\"size\":\"4:2\"}";
    private static final String RESIZE_WIDGET_JSON = "{\"packageName\":\"com.mock1.yandex.test\",\"className\":\".SomeClass\",\"time\":1000,\"place\":{\"homescreens\":{\"screen\":{\"position\":\"3:0\",\"number\":\"0\"}}},\"settings\":{\"theme\":\"black\"},\"action\":{\"resize\":{\"4:2\":{\"to\":\"1:1\"}}},\"size\":\"4:2\"}";

    public WidgetLauncherStoryTest() throws NoSuchFieldException, IllegalAccessException {
    }

    private WidgetLaunchStory story;
    private ItemAnalyticsInfo itemAnalyticsInfo;
    private StoryManager storyManager = Mockito.mock(StoryManager.class);

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Context context = Mockito.mock(Context.class);
        Mockito.when(context.getString(Mockito.anyInt())).thenReturn("%1$d:%2$d");
        story = new WidgetLaunchStory(context);
        story.setManager(storyManager);
        Mockito.doNothing().when(storyManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());

        itemAnalyticsInfo = new ItemAnalyticsInfo();
        itemAnalyticsInfo.setComponentName(new ComponentName("com.mock1.yandex.test", ".SomeClass"));
        itemAnalyticsInfo.setMainContainerName(WidgetLaunchStory.MAIN_CONTAINER_HOMESCREENS);
        itemAnalyticsInfo.setContainerPartType(WidgetLaunchStory.PART_SCREEN);
        itemAnalyticsInfo.setContainerPart(WidgetLaunchStory.NUMBER_KEY, "0");
        itemAnalyticsInfo.setPositionX(3);
        itemAnalyticsInfo.setPositionY(0);
        itemAnalyticsInfo.setSpan(4,2);
        itemAnalyticsInfo.setNewSpan(1,1);
    }

    @Test
    public void reportActionWithCollectedInfoTest() throws JSONException {
        WidgetLaunchStory spyStory = Mockito.spy(story);
        Mockito.doReturn("").when(spyStory).buildJson(Mockito.any(ItemAnalyticsInfo.class), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        spyStory.reportActionWithCollectedInfo("SomeAction", 1000);
        Mockito.verify(spyStory).buildJson(Mockito.any(ItemAnalyticsInfo.class), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString());
        Mockito.verify(storyManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());
    }


    @Test
    public void buildJsonForRunEventTest() throws JSONException {
        String json = story.buildJson(itemAnalyticsInfo, WidgetLaunchStory.EVENT_RUN, 1000, "black");
        Assert.assertEquals(RUN_WIDGET_JSON, json);
    }

    @Test
    public void buildJsonForMoveEventTest() throws JSONException {
        String json = story.buildJson(itemAnalyticsInfo, WidgetLaunchStory.EVENT_MOVE, 1000, "black");
        Assert.assertEquals(MOVE_WIDGET_JSON, json);
    }

    @Test
    public void buildJsonForDeleteEventTest() throws JSONException {
        String json = story.buildJson(itemAnalyticsInfo, WidgetLaunchStory.EVENT_DELETE, 1000, "black");
        Assert.assertEquals(DELETE_WIDGET_JSON, json);
    }

    @Test
    public void buildJsonForResizeEventTest() throws JSONException {
        String json = story.buildJson(itemAnalyticsInfo, WidgetLaunchStory.EVENT_RESIZE, 1000, "black");
        Assert.assertEquals(RESIZE_WIDGET_JSON, json);
    }

}
