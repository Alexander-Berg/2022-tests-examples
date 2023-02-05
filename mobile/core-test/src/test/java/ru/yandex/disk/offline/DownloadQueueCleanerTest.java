package ru.yandex.disk.offline;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.download.DownloadQueueItem;
import ru.yandex.disk.fetchfilelist.DbFileItem;
import ru.yandex.disk.fetchfilelist.SyncListenerTest;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static org.hamcrest.Matchers.equalTo;


public class DownloadQueueCleanerTest extends SyncListenerTest {

    private DownloadQueue downloadQueue;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        downloadQueue = TestObjectsFactory.createDownloadQueue(context);
        final DownloadQueueCleaner cleaner = new DownloadQueueCleaner(downloadQueue);
        addSycListener(cleaner);
    }

    //TODO another test for functionality of DownloadQueueCleaner
    //are placed in OfflineFilesSyncOperationTest.testDownloadQueueStateAfterSync() 


    @Test
    public void testShouldDeleteOnlyGivenItem() throws Exception {
        downloadQueue.add(DownloadQueueItem.Type.SYNC, new Path("/disk/a"), null, 0, 0);
        downloadQueue.add(DownloadQueueItem.Type.SYNC, new Path("/disk/b"), null, 0, 0);

        addToDb(new DbFileItem("/disk/a", false, DiskItem.OfflineMark.MARKED, null, null));

        emulateSync(new DiskItem[] {});

        assertTrue("downloadQueue.isEmpty()", !downloadQueue.isEmpty());

        DownloadQueueItem dqi = downloadQueue.poll();
        assertThat("/disk/b", equalTo(dqi.getServerPath().getPath()));
    }

}
