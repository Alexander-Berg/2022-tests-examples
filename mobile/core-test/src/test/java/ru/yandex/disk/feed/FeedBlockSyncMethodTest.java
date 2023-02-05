package ru.yandex.disk.feed;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.BetterCursorWrapper;
import ru.yandex.util.Path;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.spy;

public class FeedBlockSyncMethodTest extends AndroidTestCase2 {

    private FeedDatabase feedDatabase;
    private FeedBlockSyncMethod syncer;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final SQLiteOpenHelper2 sqlite = TestObjectsFactory.createSqlite(getMockContext());
        final DiskDatabase diskDatabase = spy(TestObjectsFactory.createDiskDatabase(sqlite));

        this.feedDatabase = spy(TestObjectsFactory.createFeedDatabase(sqlite, null, diskDatabase));
        this.syncer = new FeedBlockSyncMethod(feedDatabase);
    }

    @Test
    public void shouldAddNewBlocks() {
        insertBlockToDb("44444", 25);
        insertBlockToDb("33333", 23);

        final List<FeedBlock> blockList = new ArrayList<>(3);
        blockList.add(createBlock("33333", 23));
        blockList.add(createBlock("44444", 25));
        blockList.add(createBlock("55555", 25));
        syncer.sync(blockList);

        assertThat(feedDatabase.queryAllFeedBlocksForFeed().getCount(), equalTo(3));
    }

    @Test
    public void shouldRemoveOldBlocks() {
        insertBlockToDb("44444", 25);
        insertBlockToDb("33333", 23);

        final List<FeedBlock> blockList = new ArrayList<>(3);
        blockList.add(createBlock("55555", 23));
        blockList.add(createBlock("77777", 25));
        blockList.add(createBlock("66666", 25));
        syncer.sync(blockList);

        assertThat(feedDatabase.queryAllFeedBlocksForFeed().getCount(), equalTo(3));
        assertThat(feedDatabase.queryFeedBlock("44444").isEmpty(), is(true));
    }

    @Test
    public void shouldRemoveOldFileToBlocksWhenOldBlocksWereRemoved() {
        insertBlockAndBlockToFile("44444", 25, Path.asPath("/disk/file1.txt"));
        insertBlockAndBlockToFile("33333", 23, Path.asPath("/disk/A/file2.txt"));

        assertThat(feedDatabase.queryMissedBlockItemsInReversedOrder().getCount(), equalTo(2));

        final List<FeedBlock> blockList = new ArrayList<>(3);
        blockList.add(createBlock("55555", 23));
        blockList.add(createBlock("77777", 25));
        blockList.add(createBlock("66666", 25));
        syncer.sync(blockList);

        assertThat(feedDatabase.queryMissedBlockItemsInReversedOrder().getCount(), equalTo(0));
    }

    @Test
    public void shouldUpdateBlocksStatusIfRevisionsAreNotMatch() {
        insertBlockToDb("44444", 32);
        insertBlockToDb("33333", 32);
        insertBlockToDb("55555", 30);

        final List<FeedBlock> blockList = new ArrayList<>(3);
        blockList.add(createBlock("55555", 23));
        blockList.add(createBlock("44444", 25));
        blockList.add(createBlock("33333", 25));
        syncer.sync(blockList);

        assertThat(feedDatabase.queryAllFeedBlocksForFeed().getCount(), equalTo(3));
        checkBlockStatus("44444", FeedBlock.Status.CHANGED);
        checkBlockStatus("33333", FeedBlock.Status.CHANGED);
        checkBlockStatus("55555", FeedBlock.Status.CHANGED);
    }

    @Test
    public void shouldUpdateBlockRevisionsIfRevisionsAreNotMatch() {
        insertBlockToDb("44444", 11);
        insertBlockToDb("33333", 20);
        insertBlockToDb("55555", 11);

        final List<FeedBlock> blockList = new ArrayList<>(3);
        blockList.add(createBlock("55555", 23));
        blockList.add(createBlock("44444", 25));
        blockList.add(createBlock("33333", 25));
        syncer.sync(blockList);

        assertThat(feedDatabase.queryFeedBlock("55555").get(0).getRevision(), equalTo(23L));
        assertThat(feedDatabase.queryFeedBlock("44444").get(0).getRevision(), equalTo(25L));
        assertThat(feedDatabase.queryFeedBlock("55555").get(0).getRevision(), equalTo(23L));
    }

    @Test
    public void shouldStoreAllIfNoFeedBlocksBefore() {
        final List<FeedBlock> blockList = new ArrayList<>(4);
        blockList.add(createBlock("55555", 23));
        blockList.add(createBlock("44444", 25));
        blockList.add(createBlock("33333", 25));
        blockList.add(createBlock("11111", 10));
        syncer.sync(blockList);

        assertThat(feedDatabase.queryAllFeedBlocksForFeed().getCount(), equalTo(4));
    }

    private long insertBlockToDb(final String remoteId, final long revision) {
        return feedDatabase.insertBlock(createBlock(remoteId, revision));
    }

    private void insertBlockAndBlockToFile(final String remoteId, final long revision,
                                           final Path filePath) {
        final long blockId = insertBlockToDb(remoteId, revision);
        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, filePath);
    }

    private static ContentBlock createBlock(final String remoteId, final long revision) {
        return TestObjectsFactory.createContentBlock(remoteId, revision);
    }

    private void checkBlockStatus(final String remoteId, final int status) {
        try (final BetterCursorWrapper<FeedBlock> cursor =
                     feedDatabase.queryFeedBlock(remoteId)) {
            assertThat(cursor.getCount(), equalTo(1));
            assertThat(cursor.get(0).getStatus(), equalTo(status));
        }
    }
}
