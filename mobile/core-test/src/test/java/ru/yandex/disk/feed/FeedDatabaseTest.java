package ru.yandex.disk.feed;

import android.database.Cursor;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.provider.ContentRequest;
import ru.yandex.disk.provider.ContentRequestFactory;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemRow;
import ru.yandex.disk.provider.Settings;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.util.BetterCursorWrapper;
import ru.yandex.util.Path;

import static org.hamcrest.Matchers.*;

public class FeedDatabaseTest extends AndroidTestCase2 {

    public static final Path TEST_FILE_PATH = new Path(MockFeedResources.DISK_PATH);
    private FeedDatabase feedDatabase;
    private DiskDatabase diskDatabase;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        final SQLiteOpenHelper2 sqlite = new SQLiteOpenHelper2(getMockContext(), null, 1);

        diskDatabase = TestObjectsFactory.createDiskDatabase(sqlite);
        final Settings settings = TestObjectsFactory.createSettings(getMockContext());
        feedDatabase = TestObjectsFactory.createFeedDatabase(sqlite, settings, diskDatabase);
    }

    @Test
    public void shouldStoreBlockItem() throws Exception {
        final long blockId = insertContentBlock();

        insertFileToDiskDatabase();

        final int order = 2;
        feedDatabase.upsertBlockToFileBinder(blockId, order, 0, TEST_FILE_PATH);

        try (BetterCursorWrapper<FeedBlockItem> items = feedDatabase.queryFirstFractions()) {
            assertThat(items.getCount(), equalTo(1));
            final FeedBlockItem item = items.get(0);
            assertThat(item.getBlockId(), equalTo(blockId));
            assertThat(item.getServerOrder(), equalTo(order));
            assertThat(item.getFraction(), equalTo(0));
            assertThat(item.getFirstFractionOrder(), equalTo(order));
            assertThat(item.getName(), equalTo(MockFeedResources.NAME));
            assertThat(item.getParent(), equalTo(TEST_FILE_PATH.getParentPath()));
        }
    }

    @Test
    public void shouldNotQueryNotLoadedBlockItems() throws Exception {
        final long blockId = insertContentBlock();
        feedDatabase.upsertBlockToFileBinder(blockId, 2, 0, TEST_FILE_PATH);

        try (BetterCursorWrapper<FeedBlockItem> items =
                     feedDatabase.queryLoadedBlockItems(blockId)) {
            assertThat(items.getCount(), equalTo(0));
        }
    }

    @Test
    public void shouldQueryBlockItemsOrderedByBlockOrder() throws Exception {
        final long blockId1 = feedDatabase.insertBlock(TestObjectsFactory.createContentBlockWithOrder(1));
        final long blockId2 = feedDatabase.insertBlock(TestObjectsFactory.createContentBlockWithOrder(2));

        insertFileToDiskDatabase();

        feedDatabase.upsertBlockToFileBinder(blockId1, 1, 0, TEST_FILE_PATH);
        feedDatabase.upsertBlockToFileBinder(blockId2, 1, 0, TEST_FILE_PATH);

        try (BetterCursorWrapper<FeedBlockItem> items = feedDatabase.queryFirstFractions()) {
            assertThat(items.get(0).getBlockId(), equalTo(blockId2));
            assertThat(items.get(1).getBlockId(), equalTo(blockId1));
        }
    }

    @Test
    public void shouldQueryFirstItemsCountCorrectly() throws Exception {
        final long blockId1 = feedDatabase.insertBlock(TestObjectsFactory.createContentBlockWithOrder(1));
        // blockId2
        feedDatabase.insertBlock(TestObjectsFactory.createContentBlockWithOrder(2));
        final long blockId3 = feedDatabase.insertBlock(TestObjectsFactory.createContentBlockWithOrder(3));

        insertFileToDiskDatabase();

        feedDatabase.upsertBlockToFileBinder(blockId1, 1, 0, TEST_FILE_PATH);
        feedDatabase.upsertBlockToFileBinder(blockId1, 2, 0, TEST_FILE_PATH);
        feedDatabase.upsertBlockToFileBinder(blockId1, 3, 0, TEST_FILE_PATH);
        //block2 is empty
        feedDatabase.upsertBlockToFileBinder(blockId3, 1, 0, TEST_FILE_PATH);
        feedDatabase.upsertBlockToFileBinder(blockId3, 2, 0, TEST_FILE_PATH);

        try (Cursor counts = feedDatabase.queryFirstFractionsCounts()) {
            counts.moveToPosition(0); // block3
            assertThat(counts.getInt(0), equalTo(2));
            counts.moveToPosition(1); // block2
            assertThat(counts.getInt(0), equalTo(0));
            counts.moveToPosition(2); // block1
            assertThat(counts.getInt(0), equalTo(3));
        }
    }

    @Test
    public void shouldRenameAllChildrenAfterParentDirRenamed() {
        final long blockId = 1;
        final Path parentPath = new Path("/disk/A");

        feedDatabase.upsertBlockToFileBinder(blockId, 1, 0, parentPath);
        feedDatabase.upsertBlockToFileBinder(blockId, 2, 0, new Path(parentPath, "B"));
        feedDatabase.upsertBlockToFileBinder(blockId, 3, 0, new Path(parentPath, "C"));

        feedDatabase.renameDir(parentPath, new Path("/disk/AA"));

        assertThat(feedDatabase.blockItemExists("/disk/A"), is(false));
        assertThat(feedDatabase.blockItemExists("/disk/AA"), is(true));
        assertThat(feedDatabase.blockItemExists("/disk/AA/B"), is(true));
        assertThat(feedDatabase.blockItemExists("/disk/AA/C"), is(true));
    }

    @Test
    public void shouldMoveAllChildrenAfterParentDirMoved() {
        final long blockId = 1;
        final Path parentPath = new Path("/disk/A");

        feedDatabase.upsertBlockToFileBinder(blockId, 1, 0, parentPath);
        feedDatabase.upsertBlockToFileBinder(blockId, 2, 0, new Path(parentPath, "B"));
        feedDatabase.upsertBlockToFileBinder(blockId, 3, 0, new Path(parentPath, "C"));

        feedDatabase.moveDir(parentPath, new Path("/disk/AA"));

        assertThat(feedDatabase.blockItemExists("/disk/A"), is(false));
        assertThat(feedDatabase.blockItemExists("/disk/AA/A"), is(true));
        assertThat(feedDatabase.blockItemExists("/disk/AA/A/B"), is(true));
        assertThat(feedDatabase.blockItemExists("/disk/AA/A/C"), is(true));
    }

    @Test
    public void shouldSortFirstFractionFirstInViewer() throws Exception {
        final long blockId = insertBlockWithSevenFiles();

        reorderBlock(blockId);

        final ContentRequest cr = ContentRequestFactory.newOpenFeedPhotoRequest(blockId);
        try (BetterCursorWrapper<DiskItem> files = feedDatabase.queryDiskItems(cr)) {
            assertThat(files.get(0).getDisplayName(), equalTo("3"));
            assertThat(files.get(1).getDisplayName(), equalTo("2"));
            assertThat(files.get(2).getDisplayName(), equalTo("1"));
            assertThat(files.get(3).getDisplayName(), equalTo("0"));
            assertThat(files.get(4).getDisplayName(), equalTo("4"));
            assertThat(files.get(5).getDisplayName(), equalTo("5"));
            assertThat(files.get(6).getDisplayName(), equalTo("6"));
        }
    }

    @Test
    public void shouldFindPositionForFileForInViewer() throws Exception {
        final long blockId = insertBlockWithSevenFiles();
        reorderBlock(blockId);

        assertThat(queryPositionForViewer(blockId, "0"), equalTo(3));
        assertThat(queryPositionForViewer(blockId, "1"), equalTo(2));
        assertThat(queryPositionForViewer(blockId, "2"), equalTo(1));
        assertThat(queryPositionForViewer(blockId, "3"), equalTo(0));
        assertThat(queryPositionForViewer(blockId, "4"), equalTo(4));
        assertThat(queryPositionForViewer(blockId, "5"), equalTo(5));
        assertThat(queryPositionForViewer(blockId, "6"), equalTo(6));

    }

    @Test
    public void shouldFindPositionForInViewerFilteringBlocks() throws Exception {
        reorderBlock(insertBlockWithSevenFiles());

        final long blockId = insertBlockWithSevenFiles();
        reorderBlock(blockId);

        assertThat(queryPositionForViewer(blockId, "0"), equalTo(3));
        assertThat(queryPositionForViewer(blockId, "1"), equalTo(2));
        assertThat(queryPositionForViewer(blockId, "2"), equalTo(1));
        assertThat(queryPositionForViewer(blockId, "3"), equalTo(0));
        assertThat(queryPositionForViewer(blockId, "4"), equalTo(4));
        assertThat(queryPositionForViewer(blockId, "5"), equalTo(5));
        assertThat(queryPositionForViewer(blockId, "6"), equalTo(6));

    }

    private int queryPositionForViewer(final long blockId, final String fileName) {
        final ContentRequest cr = ContentRequestFactory
                .newOpenFeedPhotoPositionRequest(blockId, new Path("/disk", fileName));
        return feedDatabase.queryDiskItemPosition(cr);
    }

    private long insertBlockWithSevenFiles() {
        final long blockId = feedDatabase.insertBlock(TestObjectsFactory.createContentBlockWithOrder(1));

        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, insertFileAndGetPath(0));
        feedDatabase.upsertBlockToFileBinder(blockId, 1, 0, insertFileAndGetPath(1));
        feedDatabase.upsertBlockToFileBinder(blockId, 2, 0, insertFileAndGetPath(2));
        feedDatabase.upsertBlockToFileBinder(blockId, 3, 0, insertFileAndGetPath(3));
        feedDatabase.upsertBlockToFileBinder(blockId, 4, 0, insertFileAndGetPath(4));
        feedDatabase.upsertBlockToFileBinder(blockId, 6, 1, insertFileAndGetPath(6));
        feedDatabase.upsertBlockToFileBinder(blockId, 5, 1, insertFileAndGetPath(5));
        return blockId;
    }

    private void reorderBlock(final long blockId) {
        final int gridType = 1;
        feedDatabase.moveToNextFraction(blockId, 0);
        feedDatabase.moveToFirstFraction(blockId, 1, gridType, 3);
        feedDatabase.moveToFirstFraction(blockId, 2, gridType, 2);
        feedDatabase.moveToFirstFraction(blockId, 3, gridType, 1);
        feedDatabase.moveToNextFraction(blockId, 4);
    }

    private Path insertFileAndGetPath(final int i) {
        final Path path = new Path("/disk/", String.valueOf(i));
        final DiskItemRow diskItem =
                DiskDatabase.convertToDiskItemRow(MockFeedResources.createSingleResource());
        diskItem.setPath(path);
        diskItem.setDisplayName(path.getName());
        diskDatabase.updateOrInsert(diskItem);
        return path;
    }

    private long insertContentBlock() {
        return feedDatabase.insertBlock(TestObjectsFactory.createContentBlock());
    }

    private void insertFileToDiskDatabase() {
        final DiskItemRow diskItem =
                DiskDatabase.convertToDiskItemRow(MockFeedResources.createSingleResource());
        diskDatabase.updateOrInsert(diskItem);
    }

}
