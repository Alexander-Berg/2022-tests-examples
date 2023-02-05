package ru.yandex.disk.feed;

import android.content.Context;
import org.junit.Test;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.BetterCursorWrapper;
import rx.Observable;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.disk.test.TestObjectsFactory.createCredentials;

public class FetchLocalBlockListCommandTest extends AndroidTestCase2 {

    private FetchLocalBlockListCommand command;
    private DiskDataSyncManager mockDataSyncManager;
    private FeedDatabase feedDatabase;
    private MockCollectionBuilder firstCollectionBuilder;
    private MockCollectionBuilder secondCollectionBuilder;
    private String secondCollectionName;
    private String thirdCollectionName;
    private String firstCollectionName;
    private CommandLogger commandLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Context context = getMockContext();
        final SQLiteOpenHelper2 db = TestObjectsFactory.createSqlite(context);
        feedDatabase = TestObjectsFactory.createFeedDatabase(db,
                TestObjectsFactory.createSettings(context),
                TestObjectsFactory.createDiskDatabase(db));
        mockDataSyncManager = mock(DiskDataSyncManager.class);
        commandLogger = new CommandLogger();
        command = createCommand();

        firstCollectionName = "index";
        secondCollectionName = "second-collection-name";
        thirdCollectionName = "third-collection-name";

        firstCollectionBuilder = new MockCollectionBuilder();
        firstCollectionBuilder
                .setId(firstCollectionName)
                .setNextCollectionReference(secondCollectionName);

        secondCollectionBuilder = new MockCollectionBuilder();
        secondCollectionBuilder
                .setId(secondCollectionName)
                .setNextCollectionReference(thirdCollectionName);

    }

    private FetchLocalBlockListCommand createCommand() {
        return new FetchLocalBlockListCommand(
                mockDataSyncManager,
                feedDatabase,
                commandLogger,
                new EventLogger(),
                new FeedBlockConverter());
    }

    @Test
    public void storeBlocksFromSecondCollection() throws Exception {
        firstCollectionBuilder.addEasyContentBlock("1");
        secondCollectionBuilder.addEasyContentBlock("2");

        when(mockDataSyncManager.requestLocalCollection(eq(firstCollectionName)))
                .thenReturn(firstCollectionBuilder.buildObservable());
        when(mockDataSyncManager.requestLocalCollection(eq(secondCollectionName)))
                .thenReturn(secondCollectionBuilder.buildObservable());

        command.execute(new FetchLocalBlockListCommandRequest());

        final BetterCursorWrapper<FeedBlock> blocks =
                feedDatabase.queryAllFeedBlocksForFeed();
        assertThat(blocks.getCount(), equalTo(2));
    }

    @Test
    public void storeNextCollectionIdFromThirdCollection() {
        firstCollectionBuilder.addEasyContentBlock("1");
        secondCollectionBuilder.addEasyContentBlock("2");

        when(mockDataSyncManager.requestLocalCollection(eq("index")))
                .thenReturn(firstCollectionBuilder.buildObservable());
        when(mockDataSyncManager.requestLocalCollection(eq(secondCollectionName)))
                .thenReturn(secondCollectionBuilder.buildObservable());

        command.execute(new FetchLocalBlockListCommandRequest());

        final BetterCursorWrapper<FeedBlock> blocks =
                feedDatabase.queryAllFeedBlocksForFeed();
        assertThat(blocks.getCount(), equalTo(2));

        assertThat(feedDatabase.getNextCollectionId(), equalTo(thirdCollectionName));
    }

    @Test
    public void shouldStoreNextCollectionIdFromOnlyLastCollection() {
        firstCollectionBuilder.addEasyContentBlock("1");
        secondCollectionBuilder.addEasyContentBlock("2");

        when(mockDataSyncManager.requestLocalCollection(eq("index")))
                .thenReturn(firstCollectionBuilder.buildObservable());
        when(mockDataSyncManager.requestLocalCollection(eq(secondCollectionName)))
                .thenReturn(secondCollectionBuilder.buildObservable());

        feedDatabase = spy(feedDatabase);
        command = createCommand();

        command.execute(new FetchLocalBlockListCommandRequest());

        verify(feedDatabase, never()).setNextCollectionId(secondCollectionName);
        verify(feedDatabase).setNextCollectionId(thirdCollectionName);
    }

    @Test
    public void shouldSaveModifierUid() throws Exception {
        final String uid = "157603762";

        firstCollectionBuilder
                .setNextCollectionReference(null)
                .addEasyContentBlock("1")
                .setModifierUid(uid);

        when(mockDataSyncManager.requestLocalCollection(eq(firstCollectionName)))
                .thenReturn(firstCollectionBuilder.buildObservable());

        command.execute(new FetchLocalBlockListCommandRequest());

        final BetterCursorWrapper<FeedBlock> blocks =
                feedDatabase.queryAllFeedBlocksForFeed();
        assertThat(blocks.get(0).getModifierUid(), equalTo(uid));
    }

    @Test
    public void shouldResetFeedDataSyncManagerAndStartFetchRemote() throws Exception {
        firstCollectionBuilder.addEasyContentBlock("1");
        firstCollectionBuilder.addEasyContentBlock("2");

        when(mockDataSyncManager.requestLocalCollection(eq(firstCollectionName)))
                .thenReturn(firstCollectionBuilder.buildObservable());
        when(mockDataSyncManager.requestLocalCollection(eq(secondCollectionName)))
                .thenReturn(Observable.error(new Throwable("Collection not found")));

        command.execute(new FetchLocalBlockListCommandRequest());

        final BetterCursorWrapper<FeedBlock> blocks = feedDatabase.queryAllFeedBlocksForFeed();
        assertThat(blocks.getCount(), equalTo(2));

        verify(mockDataSyncManager).resetCollection(secondCollectionName);

        assertThat(commandLogger.get(0), instanceOf(FetchRemoteBlockListCommandRequest.class));
    }

    @Test
    public void shouldNormallyProcessEmptyFeed() {
        when(mockDataSyncManager.requestLocalCollection(eq("index")))
                .thenReturn(Observable.empty());
        feedDatabase.setNextCollectionId("some");

        command.execute(new FetchLocalBlockListCommandRequest());

        assertThat(feedDatabase.getNextCollectionId(), is(nullValue()));
    }

    @Test
    public void shouldRemoveExistingDoubles() {
        feedDatabase.insertBlock(TestObjectsFactory.createContentBlock());
        feedDatabase.insertBlock(TestObjectsFactory.createContentBlock());

        firstCollectionBuilder
                .setNextCollectionReference(null)
                .addContentBlock("remote_id")
                .setFolderId("folder")
                .setRemoteCollectionId("index")
                .setMediaType("mediaType");
        when(mockDataSyncManager.requestLocalCollection(eq(firstCollectionName)))
                .thenReturn(firstCollectionBuilder.buildObservable());

        command.execute(new FetchLocalBlockListCommandRequest());

        assertThat(feedDatabase.queryAllFeedBlocksForFeed().getCount(), equalTo(1));
    }

    @Test
    public void shouldRemoveIgnoreIncomingDoubles() {
        firstCollectionBuilder.addEasyContentBlock("1");

        secondCollectionBuilder.addEasyContentBlock("1");

        when(mockDataSyncManager.requestLocalCollection(eq(firstCollectionName)))
                .thenReturn(firstCollectionBuilder.buildObservable());

        when(mockDataSyncManager.requestLocalCollection(eq(secondCollectionName)))
                .thenReturn(secondCollectionBuilder.buildObservable());

        command.execute(new FetchLocalBlockListCommandRequest());

        assertThat(feedDatabase.queryAllFeedBlocksForFeed().getCount(), equalTo(1));
    }

}
