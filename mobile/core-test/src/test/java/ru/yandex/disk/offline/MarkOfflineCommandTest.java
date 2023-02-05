package ru.yandex.disk.offline;

import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.Storage;
import ru.yandex.disk.download.DownloadCommandRequest;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.event.EventSender;
import ru.yandex.disk.fetchfilelist.PathLock;
import ru.yandex.disk.notifications.PushRegistrator;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskFileCursor;
import ru.yandex.disk.provider.DiskItemRow;
import ru.yandex.disk.provider.FakeContentChangeNotifier;
import ru.yandex.disk.provider.FileTree;
import ru.yandex.disk.replication.SelfContentProviderClient;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.util.Path;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.mocks.Stubber.stub;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.util.Path.asPath;

public class MarkOfflineCommandTest extends AndroidTestCase2 {

    public static final String SYNCING = DiskDatabase.DirectorySyncStatus.SYNCING;
    private static final String SYNCED = DiskDatabase.DirectorySyncStatus.SYNC_FINISHED;

    private DiskDatabase diskDatabase;
    private MarkOfflineCommand command;
    private Storage storage;
    private FakeContentChangeNotifier contentChangeNotifier;
    private CommandLogger commandLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final SeclusiveContext context = new SeclusiveContext(mContext);
        storage = mock(Storage.class);
        final IndexDatabase indexDatabase = stub(IndexDatabase.class);
        final DownloadQueue downloadQueue = stub(DownloadQueue.class);
        final EventSender eventSender = stub(EventSender.class);
        contentChangeNotifier = new FakeContentChangeNotifier();
        final SQLiteOpenHelper2 dbOpenHelper = TestObjectsFactory.createSqlite(context);
        diskDatabase = TestObjectsFactory.createDiskDatabase(dbOpenHelper, contentChangeNotifier, mock(PathLock.class));
        commandLogger = new CommandLogger();
        final SelfContentProviderClient client =
                TestObjectsFactory.createSelfContentProviderClient(mContext);

        command = new MarkOfflineCommand(diskDatabase, stub(PushRegistrator.class), storage,
                indexDatabase, downloadQueue, stub(OfflineProgressNotificator.class),
                commandLogger, eventSender, client);
    }

    @Test
    public void testMarkDirectoryAsOffline() throws Exception {
        final String root = DiskDatabase.ROOT_PATH.getPath();
        final DiskItemRow row = new DiskItemRow();
        row.setPath(root, "A");
        row.setMpfsId(null);
        row.setIsDir(true);
        row.setEtag(null);
        row.setSize(0);
        row.setPublicUrl(null);
        row.setShared(false);
        row.setOfflineMark(DiskItem.OfflineMark.NOT_MARKED);
        diskDatabase.updateOrInsert(row);

        command.execute(makeMarkDirectoryAIntent());

        final DiskFileCursor all = diskDatabase.queryAll();
        final DiskItem dirA = all.get(0);

        assertEquals(root + "/A", dirA.getPath());
        assertEquals(DiskItem.OfflineMark.MARKED, dirA.getOffline());

        all.close();
    }

    @Test
    public void testDirectorySyncStatusUpdated() throws Exception {
        FileTree.create().content(directory("A").setSyncStatus(SYNCED).content(file("a")
                .setEtag("ETAG").setEtagLocal(null))).insertToDiskDatabase(diskDatabase);

        command.execute(makeMarkDirectoryAIntent());

        final DiskFileCursor all = diskDatabase.queryAll();
        final DiskItem dirA = all.get(0);

        final String root = DiskDatabase.ROOT_PATH.getPath();
        assertEquals(root + "/A", dirA.getPath());
        assertEquals(SYNCING, dirA.getETagLocal());

        assertThat(contentChangeNotifier.getChanges(), equalTo(asList(new Path(root + "/A"))));
        all.close();
    }

    @Test
    public void testUnmarkingOfflineFileShouldAdjustCache() throws IOException {
        final String path = DiskDatabase.ROOT_PATH.getPath() + "/dir";
        final DiskItemRow fileRow = new DiskItemRow()
                .setPath(path, "file.txt").setIsDir(false);
        diskDatabase.updateOrInsert(fileRow);

        final String filePath = path + "/file.txt";

        command.execute(new MarkOfflineCommandRequest(false, filePath, false, false));

        verify(storage).freeIfMoreThanHalfOccupied(eq(new Path(path + "/file.txt")));
    }

    @Test
    public void testUnmarkingOfflineDirShouldAdjustCache() throws IOException {
        final String root = DiskDatabase.ROOT_PATH.getPath();
        final DiskItemRow fileRow = new DiskItemRow()
                .setPath(root, "dir").setIsDir(true);
        diskDatabase.updateOrInsert(fileRow);

        final String dirPath = root + "/dir";

        command.execute(new MarkOfflineCommandRequest(false, dirPath, false, false));

        verify(storage).freeIfMoreThanHalfOccupied(eq(new Path(root + "/dir")));
    }

    @Test
    public void testUnmarkingOfflineDirShouldRemoveFromCacheIfFlagSet() {
        final String root = DiskDatabase.ROOT_PATH.getPath();
        final DiskItemRow fileRow = new DiskItemRow()
                .setPath(root, "dir").setIsDir(true);
        diskDatabase.updateOrInsert(fileRow);
        final Path path = new Path(root + "/dir");

        command.execute(new MarkOfflineCommandRequest(false, path.getPath(), false, true));

        verify(storage, never()).freeIfMoreThanHalfOccupied(path);
        verify(storage).deleteFileOrFolder(path);
    }

    @Test
    public void testShouldNotMarkAlreadyMarked() throws Exception {
        final DiskItemRow dir = new DiskItemRow()
                .setIsDir(true)
                .setPath(DiskDatabase.ROOT_PATH.getPath(), "A")
                .setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDatabase.updateOrInsert(dir);

        command.execute(new MarkOfflineCommandRequest(false, dir.getPath(), true, false));

        final DiskItem dirAfter = diskDatabase.queryDirectory(asPath(dir.getPath()));
        assertThat(dirAfter.getETagLocal(), is(nullValue()));
    }

    @Test
    public void startsDownloadCommandIfDownloadQueueIsModifiedByUnmark() throws Exception {
        final String root = DiskDatabase.ROOT_PATH.getPath();
        final String dirA = root + "/A";
        final MarkOfflineCommandRequest unmarkIntent =
                new MarkOfflineCommandRequest(false, dirA, false, false);

        command.execute(unmarkIntent);

        assertThat(commandLogger.getLast(), instanceOf(DownloadCommandRequest.class));
    }

    private MarkOfflineCommandRequest makeMarkDirectoryAIntent() {
        final String root = DiskDatabase.ROOT_PATH.getPath();
        final String dirA = root + "/A";
        return new MarkOfflineCommandRequest(false, dirA, true, false);
    }
}
