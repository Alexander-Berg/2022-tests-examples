package ru.yandex.disk.feed;

import android.content.Context;
import org.junit.Test;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static org.mockito.Mockito.*;

public class PrepareFeedItemsCommandTest extends AndroidTestCase2 {

    private FeedDatabase feedDatabase;
    private PrepareFeedItemsCommand command;
    private ImageGridConfigProvider mockImageGridConfigProvider;
    private DiskDatabase diskDatabase;
    private ContentBlock block;
    private ContentBlockGridPreparator blockGridPreparator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final Context context = getMockContext();
        final SQLiteOpenHelper2 sqlite = TestObjectsFactory.createSqlite(context);

        diskDatabase = TestObjectsFactory.createDiskDatabase(sqlite);
        feedDatabase = TestObjectsFactory.createFeedDatabase(sqlite, TestObjectsFactory.createSettings(context), diskDatabase);

        mockImageGridConfigProvider = mock(ImageGridConfigProvider.class);
        blockGridPreparator = mock(ContentBlockGridPreparator.class);

        command = new PrepareFeedItemsCommand(blockGridPreparator);

        block = TestObjectsFactory.createContentBlock();

    }

    private GridConfig createGridConfig(int gridType, int[] positions) {
        return new GridConfig(gridType, positions);
    }

    @Test
    public void shouldNotProcessBlockInChangedStatus() throws Exception {
        final long blockId = feedDatabase.insertBlock(block);
        final Path path = Path.asPath("/disk/a");
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(path).build());
        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, path);

        when(mockImageGridConfigProvider.getGridConfigByRatio(any()))
                .thenReturn(createGridConfig(0, new int[1]));

        feedDatabase.updateBlockStatus(blockId, FeedBlock.Status.CHANGED);

        command.execute(new PrepareFeedItemsCommandRequest(blockId));

        verifyNoMoreInteractions(mockImageGridConfigProvider);
    }

    @Test
    public void shouldIgnoreItemsWithUnexpectedServerOrder() throws Exception {
        final long blockId = feedDatabase.insertBlock(block);
        final Path path = Path.asPath("/disk/a");
        diskDatabase.updateOrInsert(new DiskItemBuilder().setPath(path).build());

        feedDatabase.upsertBlockToFileBinder(blockId, 0, 0, path);
        feedDatabase.upsertBlockToFileBinder(blockId, 3, 0, path);

        when(mockImageGridConfigProvider.getGridConfigByRatio(any()))
                .thenReturn(createGridConfig(0, new int[2]));

        feedDatabase.updateBlockStatus(blockId, FeedBlock.Status.ITEMS_LOADED);

        command.execute(new PrepareFeedItemsCommandRequest(blockId));
    }
}
