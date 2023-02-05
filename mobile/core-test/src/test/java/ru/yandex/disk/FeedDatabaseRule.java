package ru.yandex.disk;

import android.content.Context;
import org.junit.Ignore;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.feed.FeedBlock;
import ru.yandex.disk.feed.FeedDatabase;
import ru.yandex.disk.feed.ImmutableContentBlock;
import ru.yandex.disk.feed.ContentBlock;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static org.mockito.Mockito.mock;
import static ru.yandex.disk.util.Preconditions.checkNotNull;

@Ignore
public class FeedDatabaseRule implements TestRule {

    private FeedDatabase feedDatabase;
    private DiskDatabase diskDatabase;

    private EventLogger eventLogger;
    private RemoteRepo remoteRepo;

    private ImmutableContentBlock defaultBlock;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                initFields();

                base.evaluate();
            }
        };
    }

    private void initFields() {
        final Context context = getMockContext();
        checkNotNull(context);
        final SQLiteOpenHelper2 sqlite = TestObjectsFactory.createSqlite(context);

        diskDatabase = TestObjectsFactory.createDiskDatabase(sqlite);
        feedDatabase = TestObjectsFactory.createFeedDatabase(sqlite, TestObjectsFactory.createSettings(context),
                diskDatabase);

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
            FeedBlock.Type.CONTENT,
            FeedBlock.DataSource.FEED,
            "index",
            "image"
        );

        remoteRepo = mock(RemoteRepo.class);

        eventLogger = new EventLogger();
    }

    private Context getMockContext() {
        return RuntimeEnvironment.application;
    }

    public ContentBlock insertBlockWithContentItem(final long id) {
        final Path path = new Path("/disk/" + id);

        diskDatabase.updateOrInsert(new DiskItemBuilder()
                .setPath(path)
                .build());

        final ContentBlock block = defaultBlock
                .setId(id)
                .setRemoteId(String.valueOf(id * 20))
                .setRemoteCollectionId("index");
        final long blockId = feedDatabase.insertBlock(block);
        feedDatabase.upsertBlockToFileBinder(blockId, 0, 2, path);

        return block;
    }

    public FeedDatabase getFeedDatabase() {
        return feedDatabase;
    }

    public DiskDatabase getDiskDatabase() {
        return diskDatabase;
    }

    public EventLogger getEventLogger() {
        return eventLogger;
    }
}
