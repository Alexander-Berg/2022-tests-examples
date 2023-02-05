package ru.yandex.disk.download;

import org.junit.Test;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import java.util.List;

public class DownloadQueueTest extends AndroidTestCase2 {

    private DownloadQueue queue;
    private SeclusiveContext context;
    private Path srcA;
    private Path srcB;
    private Path destDirectory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        queue = TestObjectsFactory.createDownloadQueue(context);
        srcA = new Path("/disk/a");
        srcB = new Path("/disk/b");
        destDirectory = new Path("/local");
    }

    @Test
    public void testAddAndPeek() throws Exception {
        queue.add(DownloadQueueItem.Type.UI, srcA, destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);
        DownloadQueueItem dqi = queue.peek();
        assertEquals(srcA, dqi.getServerPath());
        assertEquals(destDirectory, dqi.getDestinationDirectory());
        assertEquals(DownloadQueueItem.Type.UI, dqi.getType());
    }

    @Test
    public void testRemove() throws Exception {
        queue.add(DownloadQueueItem.Type.UI, srcA, destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);
        DownloadQueueItem dqi = queue.peek();

        queue.remove(dqi);

        assertNull(queue.peek());
    }

    @Test
    public void testUpdate() throws Exception {
        Path unfinishedPath = new Path("/unfinished/path");

        queue.add(DownloadQueueItem.Type.UI, srcA, destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);
        DownloadQueueItem origin = queue.peek();
        origin.setTaskId(10);
        //origin.setState(DownloadQueueItem.State.INACTIVE);
        origin.setUnfinishedDownloadPath(unfinishedPath);

        queue.update(origin);

        queue.add(DownloadQueueItem.Type.UI, srcB, destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);

        DownloadQueueItem modified = queue.peek();
        assertEquals(origin.getId(), modified.getId());
        assertEquals(10, modified.getTaskId());
        //assertEquals(DownloadQueueItem.State.INACTIVE, modified.getQueueState());
        assertEquals(unfinishedPath, modified.getUnfinishedDownloadPath());
    }

    @Test
    public void testGetInactiveItem() throws Exception {
        queue.add(DownloadQueueItem.Type.UI, srcA, destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.UI, srcB, destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);

        DownloadQueueItem origin = queue.peek();
        origin.setState(DownloadQueueItem.State.INACTIVE);
        queue.update(origin);

        List<DownloadQueueItem> inactiveItems = queue.getInactiveItems();
        assertEquals(1, inactiveItems.size());
        assertEquals(srcA, inactiveItems.get(0).getServerPath());
    }

    @Test
    public void testFindByServerPathAndDestination() throws Exception {
        Path destDirectoryA = new Path("/local/A");
        Path destDirectoryB = new Path("/local/B");

        queue.add(DownloadQueueItem.Type.UI, srcA, destDirectoryA, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.SYNC, srcA, destDirectoryA, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.SYNC, srcA, destDirectoryB, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.SYNC, srcB, destDirectoryA, DownloadQueue.UNDEF_TASK_ID, 0);

        List<DownloadQueueItem> twins = queue.findByServerPathAndDestination(srcA, destDirectoryA);
        assertEquals(2, twins.size());
        assertEquals(DownloadQueueItem.Type.UI, twins.get(0).getType());
    }

    @Test
    public void testFindByServerPathAndDestinationIfNullDestination() throws Exception {
        Path destDirectoryA = null;
        Path destDirectoryB = new Path("/local/B");

        queue.add(DownloadQueueItem.Type.UI, srcA, destDirectoryA, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.SYNC, srcA, destDirectoryA, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.SYNC, srcA, destDirectoryB, DownloadQueue.UNDEF_TASK_ID, 0);
        queue.add(DownloadQueueItem.Type.SYNC, srcB, destDirectoryA, DownloadQueue.UNDEF_TASK_ID, 0);

        List<DownloadQueueItem> twins = queue.findByServerPathAndDestination(srcA, null);
        assertEquals(2, twins.size());
        assertEquals(DownloadQueueItem.Type.UI, twins.get(0).getType());
    }

    @Test
    public void testContainsItem() {
        queue.add(DownloadQueueItem.Type.UI, new Path("/local/A/a/b"), destDirectory, DownloadQueue.UNDEF_TASK_ID, 0);

        assertTrue(queue.containsItem("/local/A"));
    }

    @Override
    protected void tearDown() throws Exception {
        queue.getDatabase().getDbOpener().close();
        super.tearDown();
    }
}
