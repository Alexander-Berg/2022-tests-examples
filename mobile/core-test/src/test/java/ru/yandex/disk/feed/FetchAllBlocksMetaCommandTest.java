package ru.yandex.disk.feed;

import android.content.Context;
import com.yandex.disk.rest.json.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.event.DiskEvents;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.remote.FeedApi;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.offline.operations.ImmutableOperation;
import ru.yandex.disk.offline.operations.PendingOperations;
import ru.yandex.disk.offline.operations.PendingOperationsObserver;
import ru.yandex.disk.offline.operations.delete.DeleteResourcePayload;
import ru.yandex.disk.offline.operations.registry.PendingOperationsRegistry;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.ResourcesApi;
import ru.yandex.disk.remote.exceptions.NotFoundException;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.BetterCursorWrapper;
import ru.yandex.util.Path;
import rx.Single;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.nullable;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.disk.util.Preconditions.checkNotNull;

@RunWith(RobolectricTestRunner.class)
public class FetchAllBlocksMetaCommandTest extends AndroidTestCase2 {

    private FeedDatabase feedDatabase;
    private RemoteRepo remoteRepo;
    private UserSettings userSettings;

    private FetchAllBlocksMetaCommand command;
    private FetchAllBlocksMetaCommandRequest request;
    private DiskDatabase diskDatabase;
    private EventLogger eventLogger;

    private ResourcesApi.UsersResources resource;
    private Resource item;
    private ImmutableContentBlock defaultBlock;
    private PendingOperationsRegistry pendingOperationsRegistry;
    private PendingOperationsObserver pendingOperationsObserver;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final Context context = getMockContext();
        checkNotNull(context);
        final SQLiteOpenHelper2 sqlite = TestObjectsFactory.createSqlite(context);
        diskDatabase = TestObjectsFactory.createDiskDatabase(sqlite);
        feedDatabase = TestObjectsFactory.createFeedDatabase(sqlite, TestObjectsFactory.createSettings(context),
                diskDatabase);
        remoteRepo = mock(RemoteRepo.class);
        userSettings = mock(UserSettings.class);

        final ArgumentCaptor<PendingOperationsObserver> captor = ArgumentCaptor.forClass(PendingOperationsObserver.class);
        final PendingOperations pendingOperations = mock(PendingOperations.class);
        pendingOperationsRegistry = new PendingOperationsRegistry(pendingOperations);
        verify(pendingOperations).addObserver(captor.capture());
        pendingOperationsObserver = captor.getValue();

        eventLogger = new EventLogger();

        final CredentialsManager cm = new CredentialsManagerWithUser("user");

        command = new FetchAllBlocksMetaCommand(remoteRepo, feedDatabase, diskDatabase,
                mock(CommandStarter.class), cm,
                eventLogger, userSettings, pendingOperationsRegistry,
                mock(ContentBlockGridPreparator.class));
        request = new FetchAllBlocksMetaCommandRequest();

        item = MockFeedResources.createResource();
        resource = MockFeedResources.createSingleResource();

        defaultBlock = new ImmutableContentBlock(
            "test",
            null,
            3,
            1,
            5,
            1,
            2,
            3,
            0,
            "/disk",
            null,
            null,
            "123456",
            123,
            "content_block",
            FeedBlock.DataSource.FEED,
            "index",
            "image"
        );
    }

    @Test
    public void shouldPutItemsFromRemoteRepoToDatabase() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        insertBlock();

        command.execute(request);

        assertThat(feedDatabase.queryFirstFractions().getCount(), equalTo(1));
    }

    @Test
    public void shouldUpdateDiskDatabase() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        insertBlock();

        command.execute(request);

        final DiskItem file = diskDatabase.queryFileItem(
                new Path(DiskDatabase.ROOT_PATH, item.getPath().getPath()));
        assertThat(file.getSize(), equalTo(MockFeedResources.SIZE));
        assertThat(file.getDisplayName(), equalTo(MockFeedResources.NAME));
        assertThat(file.getPath(), equalTo(MockFeedResources.DISK_PATH));
        assertThat(file.getMimeType(), equalTo(MockFeedResources.MIME));
        assertThat(file.getMediaType(), equalTo(MockFeedResources.MEDIA));
        assertThat(file.getETag(), equalTo(MockFeedResources.MD5));
    }

    @Test
    public void shouldSendErrorEventOnException() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.error(new Exception()));

        final long id = insertBlock();

        command.execute(request);
        checkBlockStatus(id, FeedBlock.Status.ERROR);

        assertThat(eventLogger.getLast(), instanceOf(DiskEvents.FetchFeedBlockFailed.class));
    }

    @Test
    public void shouldSendEventOnSuccess() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        insertBlock();

        command.execute(request);
        assertThat(eventLogger.getLast(), instanceOf(DiskEvents.FeedUpdated.class));
    }

    @Test
    public void shouldSetStatusItemsLoadedForMultiFilesBlock() throws Exception {
        final Resource resource1 = MockFeedResources.createResource();
        final Resource resource2 = MockFeedResources.createResource();
        final ResourcesApi.UsersResources resources =
                MockFeedResources.createResources(resource1, resource2);
        whenGetFeedBlockItems().thenReturn(Single.just(resources));

        final long id = insertBlock();

        command.execute(request);
        checkBlockStatus(id, FeedBlock.Status.ITEMS_LOADED);
    }

    @Test
    public void shouldSaveBlockInfo() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        final long id = feedDatabase.insertBlock(defaultBlock.setModifierUid("2"));

        command.execute(request);

        try (BetterCursorWrapper<FeedBlock> blocks = feedDatabase.queryFeedBlock(id)) {
            final FeedBlock block = blocks.get(0);
            assertThat(block.getModifierLogin(), equalTo("testuser2"));
            if (block instanceof ContentBlock) {
                final ContentBlock contentBlock = (ContentBlock) block;
                assertThat(contentBlock.getFilesCount(), equalTo(1));
            }
        }
    }

    @Test
    public void shouldIgnoreFilesInProgress() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        final String path = "/disk/lenta-data/snapshot.json";

        final long id = feedDatabase.insertBlock(defaultBlock.setModifierUid("2").setPath(path));

        final ImmutableOperation op = ImmutableOperation.builder()
                .setId(0)
                .setMaxAttempts(6)
                .setPayload(new DeleteResourcePayload(path))
                .setType(DeleteResourcePayload.TYPE)
                .build();

        pendingOperationsObserver.onOperationAdded(op);

        command.execute(request);

        try (BetterCursorWrapper<FeedBlock> blocks = feedDatabase.queryFeedBlock(id)) {
            assertThat(blocks.isEmpty(), equalTo(true));
        }
    }

    @Test
    public void shouldRequestWithModifierLogin() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        feedDatabase.insertBlock(defaultBlock.setModifierUid("uid"));

        command.execute(request);

        verify(remoteRepo).getFeedBlockItems(anyString(), nullable(String.class), anyLong(),
                anyLong(), anyInt(), anyInt(), anyString(), eq("uid"));
    }

    @Test
    public void shouldRequestMaxItemsForFirstFraction() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        feedDatabase.insertBlock(defaultBlock.setModifierUid("uid"));

        command.execute(request);

        verify(remoteRepo).getFeedBlockItems(anyString(), nullable(String.class), anyLong(), anyLong(),
                anyInt(), eq(feedDatabase.getCurrentFirstFractionLimit()), anyString(), nullable(String.class));
    }

    @Test
    public void shouldRemoveEmptyBlocks() throws Exception {
        when(resource.getTotalCount()).thenReturn(0);
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        feedDatabase.insertBlock(defaultBlock);

        command.execute(request);

        try (BetterCursorWrapper<FeedBlock> blocks = feedDatabase.queryAllFeedBlocksForFeed()) {
            assertThat(blocks.getCount(), equalTo(0));
        }
    }

    @Test
    public void shouldRemoveBlockIfDirHasBeenDeleted() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.error(new NotFoundException("test")));

        feedDatabase.insertBlock(defaultBlock);

        command.execute(request);

        try (BetterCursorWrapper<FeedBlock> blocks = feedDatabase.queryAllFeedBlocksForFeed()) {
            assertThat(blocks.getCount(), equalTo(0));
        }
    }

    private void checkBlockStatus(final long id, final int status) {
        try (BetterCursorWrapper<FeedBlock> cursor = feedDatabase.queryFeedBlock(id)) {
            assertThat(cursor.getCount(), equalTo(1));
            assertThat(cursor.get(0).getStatus(), equalTo(status));
        }
    }

    private long insertBlock() {
        return feedDatabase.insertBlock(defaultBlock);
    }

    private OngoingStubbing<Single<ResourcesApi.UsersResources>> whenGetFeedBlockItems() {
        return when(remoteRepo.getFeedBlockItems(anyString(), nullable(String.class), anyLong(),
                anyLong(), anyInt(), anyInt(), anyString(), nullable(String.class)));
    }

}
