package ru.yandex.disk.feed;

import android.content.Context;
import com.yandex.disk.rest.json.Resource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.OngoingStubbing;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.offline.operations.PendingOperations;
import ru.yandex.disk.offline.operations.registry.PendingOperationsRegistry;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.ResourcesApi;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import rx.Single;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.util.Preconditions.checkNotNull;

public class FetchContentBlockFirstMetaCommandTest extends AndroidTestCase2 {

    private DiskDatabase diskDatabase;
    private FeedDatabase feedDatabase;
    private RemoteRepo remoteRepo;
    private EventLogger eventLogger;
    private FetchContentBlockFirstMetaCommand command;
    private UserSettings userSettings;
    private Resource item;
    private ResourcesApi.UsersResources resource;
    private ImmutableContentBlock defaultBlock;
    private ContentBlockGridPreparator mockGridPreparator;
    private PendingOperationsRegistry pendingOperationsRegistry;

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

        eventLogger = new EventLogger();
        userSettings = mock(UserSettings.class);
        pendingOperationsRegistry = new PendingOperationsRegistry(mock(PendingOperations.class));

        mockGridPreparator = mock(ContentBlockGridPreparator.class);
        command = new FetchContentBlockFirstMetaCommand(remoteRepo, feedDatabase, diskDatabase,
                new CommandLogger(), eventLogger, mockGridPreparator, userSettings,
                pendingOperationsRegistry);

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

        final long blockId = insertBlock();

        command.execute(new FetchContentBlockFirstMetaCommandRequest(blockId));

        assertThat(feedDatabase.queryFirstFractions().getCount(), equalTo(1));
    }

    @Test
    public void shouldRequestPreparation() throws Exception {
        whenGetFeedBlockItems().thenReturn(Single.just(resource));

        final long blockId = insertBlock();

        command.execute(new FetchContentBlockFirstMetaCommandRequest(blockId));

        verify(mockGridPreparator).prepare(blockId, true);
    }

    private long insertBlock() {
        return feedDatabase.insertBlock(defaultBlock);
    }

    private OngoingStubbing<Single<ResourcesApi.UsersResources>> whenGetFeedBlockItems() {
        return when(remoteRepo.getFeedBlockItems(anyString(), nullable(String.class), anyLong(),
                anyLong(), anyInt(), anyInt(), anyString(), nullable(String.class)));
    }

}
