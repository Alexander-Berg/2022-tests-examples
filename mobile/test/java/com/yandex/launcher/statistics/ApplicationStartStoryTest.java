package com.yandex.launcher.statistics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.common.loaders.http2.ResponseInfo;
import com.yandex.launcher.common.loaders.http2.ResponseStatus;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.net.HttpURLConnection;


public class ApplicationStartStoryTest extends BaseRobolectricTest {

    public static final String TEST_JSON = "{\"clid\":\"111111\",\"installer\":\"some.installer.id\",\"settings\":{\"grid\":\"1:3\"}}";
    public static final String TEST_NOT_STARTED_EXPERIMENTS_JSON = "{\"experiments\":{\"received\":false,\"reason\":\"Not started\",\"time\":0}}";
    public static final String TEST_NOT_FINISHED_EXPERIMENTS_JSON = "{\"experiments\":{\"received\":false,\"reason\":\"Not finished\",\"time\":0}}";
    public static final String TEST_FINISHED_EXPERIMENTS_JSON = "{\"experiments\":{\"received\":true,\"reason\":\"Server response 200\",\"time\":1000}}";
    public static final String TEST_FINISHED_EXPERIMENTS_NOT_FOUND_JSON = "{\"experiments\":{\"received\":false,\"reason\":\"Server response 404\",\"time\":1000}}";

    public ApplicationStartStoryTest() throws NoSuchFieldException, IllegalAccessException {
    }

    private ApplicationStartStory startStory;
    private SharedPreferences mockPreferences;
    private SharedPreferences.Editor mockEditor;
    private StoryManager mockStoryManager;
    private StoryEvent storyEvent;
    private StoryEvent storyLoadEvent;

    @SuppressLint("CommitPrefEdits")
    @Override
    public void setUp() throws Exception {
        super.setUp();

        mockEditor = Mockito.mock(SharedPreferences.Editor.class);
        Mockito.doReturn(mockEditor).when(mockEditor).putBoolean(Mockito.anyString(), Mockito.anyBoolean());

        mockPreferences = Mockito.mock(SharedPreferences.class);
        Mockito.doReturn(true).when(mockPreferences).getBoolean(ApplicationStartStory.FIRST_START_KEY, true);
        Mockito.doReturn(mockEditor).when(mockPreferences).edit();

        Context context = Mockito.mock(Context.class);
        Mockito.doReturn(mockPreferences).when(context).getSharedPreferences(Mockito.anyString(), Mockito.anyInt());

        mockStoryManager = Mockito.mock(StoryManager.class);
        Mockito.doNothing().when(mockStoryManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());

        startStory = Mockito.spy(new ApplicationStartStory(context));
        startStory.setManager(mockStoryManager);

        storyEvent = Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_APPSTART_START,0, new String[]{"", ""}));
        storyLoadEvent = Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_LAUNCHER_FIRST_LOAD,0, ""));
    }

    @Test
    public void putFirstStartBooleanTest(){
        Mockito.doReturn(false).when(mockPreferences).getBoolean(ApplicationStartStory.FIRST_START_KEY, true);
        startStory.onEvent(StoryEvent.create(StoryEvent.Events.EVENT_APPSTART_START,0,null));
        Mockito.verify(mockEditor).putBoolean(ApplicationStartStory.FIRST_START_KEY, false);
    }

    @Test
    public void sendEventFirstTimeTest(){
        startStory.onEvent(storyEvent);
        Mockito.verify(mockStoryManager).sendRemoteJson(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void notSendEventSecondTimeTest(){
        Mockito.doReturn(false).when(mockPreferences).getBoolean(ApplicationStartStory.FIRST_START_KEY, true);
        startStory.onEvent(storyEvent);
        Mockito.verify(mockStoryManager, Mockito.never()).sendRemoteJson(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testStartEventTest(){
        startStory.onEvent(storyEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1)).sendRemoteJson(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void testDoubleStartEventTest(){
        startStory.onEvent(storyEvent);
        startStory.onEvent(storyEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1)).sendRemoteJson(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void buildRightJsonTest() {
        String json = startStory.buildJson("111111", "some.installer.id", 1, 3);
        Assert.assertEquals(json, TEST_JSON);
    }

    @Test
    public void buildWrongJsonTest() {
        String json = startStory.buildJson("qqq", "some.wrong.installer.id", 1, 3);
        Assert.assertNotEquals(TEST_JSON, json);
    }

    // first_app_load tests
    @Test
    public void shouldSendFirstLoadEventOnFirstAppLoad() {
        startStory.onEvent(storyLoadEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1))
                .sendRemoteJson(Mockito.eq(ApplicationStartStory.EVENT_LOAD_NAME), Mockito.anyString());
    }

    @Test
    public void shouldSendNotStartedEventOnFirstAppLoad() {
        startStory.onEvent(storyLoadEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1))
                .sendRemoteJson(ApplicationStartStory.EVENT_LOAD_NAME, TEST_NOT_STARTED_EXPERIMENTS_JSON);
    }

    @Test
    public void shouldSendNotFinishedEventOnFirstAppLoad() {
        final StoryEvent experimentsLoadStartedEvents =
                Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_EXPERIMENTS_START_LOAD,0, 1000L));

        startStory.onEvent(experimentsLoadStartedEvents);
        startStory.onEvent(storyLoadEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1))
                .sendRemoteJson(ApplicationStartStory.EVENT_LOAD_NAME, TEST_NOT_FINISHED_EXPERIMENTS_JSON);
    }

    @Test
    public void shouldSendSentExperimentsEventOnFirstAppLoad() {
        final StoryEvent experimentsLoadStartedEvents =
                Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_EXPERIMENTS_START_LOAD,0, 1000L));
        final ResponseInfo responseInfo =
                new ResponseInfo(ResponseStatus.INTERNET, 2000, -1, HttpURLConnection.HTTP_OK, null, null);
        final StoryEvent experimentsLoadFinishedEvents =
                Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_EXPERIMENTS_LOADED,1, responseInfo));

        startStory.onEvent(experimentsLoadStartedEvents);
        startStory.onEvent(experimentsLoadFinishedEvents);
        startStory.onEvent(storyLoadEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1))
                .sendRemoteJson(ApplicationStartStory.EVENT_LOAD_NAME, TEST_FINISHED_EXPERIMENTS_JSON);
    }

    @Test
    public void shouldSendSentExperimentsWithExpectedServerResponseCodeEventOnFirstAppLoad() {
        final StoryEvent experimentsLoadStartedEvents =
                Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_EXPERIMENTS_START_LOAD,0, 1000L));
        final ResponseInfo responseInfo =
                new ResponseInfo(ResponseStatus.INTERNET_FAIL, 2000, -1, HttpURLConnection.HTTP_NOT_FOUND, null, null);
        final StoryEvent experimentsLoadFinishedEvents =
                Mockito.spy(StoryEvent.create(StoryEvent.Events.EVENT_EXPERIMENTS_LOADED,1, responseInfo));

        startStory.onEvent(experimentsLoadStartedEvents);
        startStory.onEvent(experimentsLoadFinishedEvents);
        startStory.onEvent(storyLoadEvent);
        Mockito.verify(mockStoryManager, Mockito.times(1))
                .sendRemoteJson(ApplicationStartStory.EVENT_LOAD_NAME, TEST_FINISHED_EXPERIMENTS_NOT_FOUND_JSON);
    }
}
