package ru.yandex.disk.fetchfilelist;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.Storage;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.download.DownloadQueueItem;
import ru.yandex.disk.offline.DownloadQueueCleaner;
import ru.yandex.disk.offline.OfflineDownloadEnqueuer;
import ru.yandex.disk.offline.OfflineDownloadEnqueuerFactory;
import ru.yandex.disk.offline.OfflineFolderSyncer;
import ru.yandex.disk.offline.OfflineProgressNotificator;
import ru.yandex.disk.offline.TransactionsSender;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.provider.FileTree;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.mocks.Stubber.stub;
import static ru.yandex.disk.provider.FileTree.*;

public class OfflineFolderSyncerTest extends DirectorySyncerTest {
    private DownloadQueue downloadQueue;
    private Storage stubStorage;
    private CommandLogger commandLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        downloadQueue = TestObjectsFactory.createDownloadQueue(context);
        stubStorage = stub(Storage.class);
        when(stubStorage.fileExists(anyString())).thenReturn(true);
    }

    @Override
    protected DirectorySyncer createDirectorySyncer() {
        commandLogger = new CommandLogger();
        final OfflineDownloadEnqueuerFactory enqueuerFactory = new OfflineDownloadEnqueuerFactory() {
            @Override
            public OfflineDownloadEnqueuer create(String analyticsEventName) {
                return new OfflineDownloadEnqueuer(downloadQueue, diskDatabase, stubStorage, commandLogger, analyticsEventName);
            }
        };
        final DownloadQueueCleaner downloadQueueCleaner = new DownloadQueueCleaner(downloadQueue);
        final TransactionsSender transactionsSender = new TransactionsSender(downloadQueue);
        return new OfflineFolderSyncer(diskDatabase, enqueuerFactory, downloadQueueCleaner,
                transactionsSender, getDirectory());
    }

    @Test
    public void testChangedFileShouldBeQueuedToDownload() throws Exception {
        FileTree.create().content(directory("A").setOffline(DiskItem.OfflineMark.MARKED).content(file("a").setEtag("ETAG"))).insertToDiskDatabase(diskDatabase);
        final DiskItem dirA = new DiskItemBuilder().setPath("/disk/A").setIsDir(true).build();
        final DiskItem fileAa = new DiskItemBuilder().setPath("/disk/A/a").setEtag("NEW").build();

        syncer = createSyncer("/disk/A");

        emulateSync(dirA, fileAa);

        assertTrue("DownloadQueue.isEmpty()", !downloadQueue.isEmpty());
    }

    @Test
    public void testNotChangedFileShouldNotBeQueuedToDownload() throws Exception {
        FileTree.create().content(directory("A").setOffline(DiskItem.OfflineMark.MARKED).content(file("a").setEtag("ETAG").setEtagLocal("ETAG"))).insertToDiskDatabase(diskDatabase);
        final DiskItem fileA = new DiskItemBuilder().setPath("/disk/A/a").setEtag("ETAG").build();

        syncer = createSyncer("/disk/A");

        emulateSync(fileA);

        assertTrue("must be DownloadQueue.isEmpty()", downloadQueue.isEmpty());
    }

    @Test
    public void testDeletedFileShouldBeRemovedFromDownloadQueue() throws Exception {
        downloadQueue.add(DownloadQueueItem.Type.SYNC, new Path("/disk/A/a"), null, 0, 0);
        FileTree.create().content(directory("A").setOffline(DiskItem.OfflineMark.MARKED).content(file("a").setEtag("ETAG").setEtagLocal("ETAG"))).insertToDiskDatabase(diskDatabase);

        syncer = createSyncer("/disk/A");

        emulateSyncEmptyList();

        assertTrue("file still in download queue", downloadQueue.isEmpty());
    }

    @Test
    public void testFileBecameDirectoryShouldBeRemovedFromDownloadQueue() throws Exception {
        downloadQueue.add(DownloadQueueItem.Type.SYNC, new Path("/disk/A/a"), null, 0, 0);
        FileTree.create().content(directory("A").setOffline(DiskItem.OfflineMark.MARKED).content(file("a").setEtag("ETAG").setEtagLocal("ETAG"))).insertToDiskDatabase(diskDatabase);

        final DiskItem dirA = new DiskItemBuilder().setPath("/disk/A").setIsDir(true).build();
        final DiskItem dirAa = new DiskItemBuilder().setPath("/disk/A/a").setIsDir(true).build();

        syncer = createSyncer("/disk/A");

        emulateSync(dirA, dirAa);

        assertTrue("file still in download queue", downloadQueue.isEmpty());
    }
}

