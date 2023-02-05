package ru.yandex.disk.offline;

import com.google.common.io.Closer;
import org.junit.Test;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.Storage;
import ru.yandex.disk.download.DownloadCommandRequest;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.download.DownloadQueueItem;
import ru.yandex.disk.fetchfilelist.DbFileItem;
import ru.yandex.disk.fetchfilelist.SyncListenerTest;
import ru.yandex.disk.provider.DiskFileCursor;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.provider.FileTree;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.sync.RemoteFileItem;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.provider.DiskDatabase.DirectorySyncStatus.*;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.util.Path.asPath;

public class DownloadEnqueuerTest extends SyncListenerTest {

    private DownloadQueue downloadQueue;
    private Storage storage;
    private Closer closer;
    private CommandLogger commandLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        downloadQueue = TestObjectsFactory.createDownloadQueue(context);
        storage = mock(Storage.class);
        commandLogger = new CommandLogger();
        OfflineDownloadEnqueuer enqueuer = new OfflineDownloadEnqueuer(downloadQueue, diskDatabase, storage,
                commandLogger, "nomatters");
        addSycListener(enqueuer);
        closer = Closer.create();
    }

    @Test
    public void testShouldQueueSyncDownloadItem() throws Exception {
        RemoteFileItem item = new RemoteFileItem("/disk/A/a", false, null, 5984726734L);

        emulateSync(item);

        DownloadQueueItem polled = downloadQueue.poll();

        assertNotNull(polled);
        assertTrue(polled.getId() > 0);

        assertEquals(polled.getType(), DownloadQueueItem.Type.SYNC);
        assertNull(polled.getDestinationDirectory());

        assertEquals(item.getPath(), polled.getServerPath().getPath());
        assertEquals(item.getSize(), polled.getSize());
    }

    @Test
    public void testChangedFileShouldBeQueuedToDownload() throws Exception {
        emulateSync(new RemoteFileItem("/disk/A/a", false, null, 0));
        assertTrue("DownloadQueue.isEmpty()", !downloadQueue.isEmpty());
    }

    @Test
    public void testNotChangedFileShouldNotBeQueuedToDownload() throws Exception {
        when(storage.fileExists(anyString())).thenReturn(true);
        addToDb(new DbFileItem("/disk/A/a", false, DiskItem.OfflineMark.NOT_MARKED, "ETAG", "ETAG"));

        emulateSync(new RemoteFileItem("/disk/A/a", false, "ETAG", 0));

        assertTrue("must be DownloadQueue.isEmpty()", downloadQueue.isEmpty());
    }

    @Test
    public void testShouldStartDownloadCommandAfterSync() throws Exception {
        RemoteFileItem fileA = new RemoteFileItem("/disk/A/a", false, null, 0);

        emulateSync(fileA);

        assertThat(commandLogger.get(0), instanceOf(DownloadCommandRequest.class));
    }

    @Test
    public void testShouldStartDownloadCommandOnceAfterSync() throws Exception {
        RemoteFileItem fileA = new RemoteFileItem("/disk/A/a", false, null, 0);
        RemoteFileItem fileB = new RemoteFileItem("/disk/A/b", false, null, 0);

        emulateSync(fileA, fileB);

        assertThat(commandLogger.getCount(), equalTo(1));
    }

    @Test
    public void testShouldNotStartDownloadCommandIfNoActualNewTaskInDownloadQueue() throws Exception {
        downloadQueue.add(DownloadQueueItem.Type.SYNC, new Path("/disk/A/a"), null, 0, 0);
        RemoteFileItem fileA = new RemoteFileItem("/disk/A/a", false, null, 0);
        RemoteFileItem[] files = { fileA };

        emulateSync(files);

        assertThat("expect not service starts", commandLogger.getCount(), equalTo(0));
    }

    @Test
    public void testShouldUpdateDirectorySyncStateAfterQueue() throws Exception {
        FileTree.create()
                .content(directory("A").setSyncStatus(SYNC_FINISHED).content(file("a")))
                .insertToDiskDatabase(diskDatabase);
        RemoteFileItem dirA = new RemoteFileItem("/disk/A", true, null, 0);
        RemoteFileItem fileAa = new RemoteFileItem("/disk/A/a", false, "ETAG", 0);

        emulateSync(dirA, fileAa);

        DiskItem dirAfterSync = queryByPath("/disk/A");

        assertThat(dirAfterSync.getETagLocal(), equalTo(SYNCING));
    }

    @Test
    public void testShouldUpdateDirectorySyncStateAfterQueueOnce() throws Exception {
        FileTree.create()
                .content(directory("A").setSyncStatus(SYNC_FINISHED)
                        .content(file("a")), file("b"))
                .insertToDiskDatabase(diskDatabase);
        RemoteFileItem dirA = new RemoteFileItem("/disk/A", true, null, 0);
        RemoteFileItem fileA = new RemoteFileItem("/disk/A/a", false, "ETAG", 0);
        RemoteFileItem fileB = new RemoteFileItem("/disk/A/b", false, "ETAG", 0);
        emulateSync(dirA, fileA, fileB);

        Path dirPath = new Path("/disk/A");
        verify(diskDatabase).updateSyncStatus(dirPath);
        verify(diskDatabase).notifyChange(dirPath);
    }

    @Test
    public void testShouldUpdateEachChangedDirectory() throws Exception {
        FileTree.create()
                .content(directory("A").setSyncStatus(SYNC_FINISHED).content(file("a")),
                         directory("B").setSyncStatus(SYNC_FINISHED).content(file("a"))
                ).insertToDiskDatabase(diskDatabase);
        RemoteFileItem dirA = new RemoteFileItem("/disk/A", true, null, 0);
        RemoteFileItem dirB = new RemoteFileItem("/disk/B", true, null, 0);
        RemoteFileItem fileAa = new RemoteFileItem("/disk/A/a", false, "ETAG", 0);
        RemoteFileItem fileBa = new RemoteFileItem("/disk/B/b", false, "ETAG", 0);

        emulateSync(dirA, fileAa, dirB, fileBa);

        assertThat(queryByPath("/disk/A").getETagLocal(), equalTo(SYNCING));

        assertThat(queryByPath("/disk/B").getETagLocal(), equalTo(SYNCING));
    }

    @Test
    public void testShouldNotUpdateDirectoryIfNoActualNewTaskInDownloadQueue() throws Exception {
        FileTree.create()
                .content(directory("A").setSyncStatus(SYNCING).content(file("a"))
                ).insertToDiskDatabase(diskDatabase);

        downloadQueue.add(DownloadQueueItem.Type.SYNC, new Path("/disk/A/a"), null, 0, 0);

        RemoteFileItem dirA = new RemoteFileItem("/disk/A", true, null, 0);
        RemoteFileItem fileA = new RemoteFileItem("/disk/A/a", false, "ETAG", 0);
        emulateSync(dirA, fileA);

        Path dirPath = new Path("/disk/A");
        verify(diskDatabase, never()).updateSyncStatus(dirPath);
        verify(diskDatabase, never()).notifyChange(dirPath);
    }

    @Test
    public void testShouldNotStartDownloadCommandIfNotDirectoriesToSync() throws Exception {
        DiskItem dirA = new DiskItemBuilder().setPath("/disk/A").setIsDir(true).build();
        DiskItem dirB = new DiskItemBuilder().setPath("/disk/A/B").setIsDir(true).build();

        emulateSync(dirA, dirB);

        assertThat("expect not service starts", commandLogger.getCount(), equalTo(0));
    }

    private DiskItem queryByPath(String path) {
        DiskFileCursor selection = diskDatabase.queryFileByPath(asPath(path));
        closer.register(selection);
        return selection.get(0);
    }

    @Override
    protected void tearDown() throws Exception {
        closer.close();
        super.tearDown();
    }

}
