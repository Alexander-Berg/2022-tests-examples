package ru.yandex.disk.feed;

import android.content.Context;
import org.junit.Test;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.provider.Settings;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.BetterCursorWrapper;
import ru.yandex.util.Path;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.util.Path.asPath;

public class InvalidateBlocksCommandTest extends AndroidTestCase2 {

    private FeedDatabase feedDatabase;
    private DiskDatabase diskDatabase;
    private InvalidateBlocksCommand command;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Context context = getMockContext();
        final SQLiteOpenHelper2 sqlite = TestObjectsFactory.createSqlite(context);
        diskDatabase = TestObjectsFactory.createDiskDatabase(sqlite);
        final Settings settings = TestObjectsFactory.createSettings(context);
        feedDatabase = TestObjectsFactory.createFeedDatabase(sqlite, settings, diskDatabase);
        command = new InvalidateBlocksCommand(feedDatabase, new EventLogger(), new CommandLogger());
    }

    @Test
    public void shouldDeleteEmptyBlock() throws Exception {
        final long blockId = feedDatabase.insertBlock(
            TestObjectsFactory.createContentBlockWithStatus(FeedBlock.Status.READY));
        final Path filePath = asPath("/disk/file");
        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, filePath);
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(filePath).build());
        assertThat(feedDatabase.queryFeedBlock(blockId).getCount(), equalTo(1));

        diskDatabase.delete(filePath);
        command.execute(new InvalidateBlocksCommandRequest());
        assertThat(feedDatabase.queryFeedBlock(blockId).getCount(), equalTo(0));
    }

    @Test
    public void shouldDeleteInvalidBindings() throws Exception {
        final long blockId = feedDatabase.insertBlock(
            TestObjectsFactory.createContentBlockWithStatus(FeedBlock.Status.READY));
        final Path invalidBindingFilePath = asPath("/disk/invalidBindingFilePath");
        final Path filePath = asPath("/disk/file");

        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, filePath);
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(filePath).build());

        feedDatabase.upsertBlockToFileBinder(blockId, 1, 0, invalidBindingFilePath);
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(invalidBindingFilePath).build());

        assertThat(feedDatabase.queryMissedBlockItemsInReversedOrder(blockId).getCount(), equalTo(0));

        diskDatabase.delete(invalidBindingFilePath);
        assertThat(feedDatabase.queryMissedBlockItemsInReversedOrder(blockId).getCount(), equalTo(1));

        command.execute(new InvalidateBlocksCommandRequest());
        assertThat(feedDatabase.queryMissedBlockItemsInReversedOrder(blockId).getCount(), equalTo(0));
    }

    @Test
    public void shouldDeleteFileInFirstFractionAndLeftBlockReady() throws Exception {
        final long blockId = feedDatabase.insertBlock(TestObjectsFactory
            .createContentBlockWithStatusAndFilesCount(FeedBlock.Status.READY, 2));

        final Path filePath = asPath("/disk/file");
        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, filePath);
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(filePath).build());

        final Path invalidBindingFilePath = asPath("/disk/invalidBindingFilePath");
        feedDatabase.upsertBlockToFileBinder(blockId, 1, 0, invalidBindingFilePath);

        command.execute(new InvalidateBlocksCommandRequest());

        final BetterCursorWrapper<FeedBlockItem> items = feedDatabase.queryFirstFractions();
        assertThat(items.getCount(), equalTo(1));
        assertThat(feedDatabase.queryFeedBlock(blockId).single().getStatus(),
                equalTo(FeedBlock.Status.READY));

    }

    @Test
    public void shouldMarkAsChangedIfRemoveFromFirstFractionButHasSecond() throws Exception {
        final long blockId = feedDatabase.insertBlock(TestObjectsFactory
            .createContentBlockWithStatusAndFilesCount(FeedBlock.Status.READY, 3));

        final Path filePath = asPath("/disk/file");
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(filePath).build());

        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, filePath);
        final Path invalidBindingFilePath = asPath("/disk/invalidBindingFilePath");
        feedDatabase.upsertBlockToFileBinder(blockId, 1, 0, invalidBindingFilePath);

        feedDatabase.upsertBlockToFileBinder(blockId, 2, 1, filePath);

        assertThat(feedDatabase.queryFirstFractions().getCount(), equalTo(1));

        command.execute(new InvalidateBlocksCommandRequest());

        final BetterCursorWrapper<FeedBlockItem> items = feedDatabase.queryFirstFractions();
        assertThat(items.getCount(), equalTo(1));
        assertThat(feedDatabase.queryFeedBlock(blockId).single().getStatus(),
                equalTo(FeedBlock.Status.CHANGED));

    }
}
