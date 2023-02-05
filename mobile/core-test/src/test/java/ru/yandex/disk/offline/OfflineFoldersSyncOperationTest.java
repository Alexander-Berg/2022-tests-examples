package ru.yandex.disk.offline;

import android.content.ContentResolver;
import com.yandex.disk.client.CustomHeader;
import com.yandex.disk.client.exceptions.WebdavClientInitException;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.Storage;
import ru.yandex.disk.client.IndexItem;
import ru.yandex.disk.client.IndexNotExistsException;
import ru.yandex.disk.client.IndexParsingHandler;
import ru.yandex.disk.commonactions.SingleWebdavClientPool;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.event.EventSender;
import ru.yandex.disk.fetchfilelist.PathLockSpy;
import ru.yandex.disk.fetchfilelist.RegularFileStorageCleaner;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.notifications.PushRegistrator;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskFileCursor;
import ru.yandex.disk.provider.DiskItemRow;
import ru.yandex.disk.provider.FakeContentChangeNotifier;
import ru.yandex.disk.provider.FileTree;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.RestApiClient;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.replication.SelfContentProviderClient;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.settings.AutoUploadSettings;
import ru.yandex.disk.sql.SQLiteOpenHelper2;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.Reflector;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;
import ru.yandex.disk.util.Exceptions;
import ru.yandex.util.Path;

import javax.annotation.NonnullByDefault;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.nullable;
import static ru.yandex.disk.mocks.Stubber.stub;
import static ru.yandex.disk.provider.DiskDatabaseMethodTest.asPathsList;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.disk.test.MoreMatchers.anyPath;
import static ru.yandex.disk.util.SimpleProviderKt.asProvider;
import static ru.yandex.util.Path.asPath;

public class OfflineFoldersSyncOperationTest extends AndroidTestCase2 {
    private static final DiskItem.OfflineMark MARKED_OFFLINE = DiskItem.OfflineMark.MARKED;

    private SeclusiveContext context;
    private IndexDatabase indexDatabase;
    private DiskDatabase diskDatabase;
    private DownloadQueue downloadQueue;
    private PathLockSpy pathLockSpy;
    private Storage storage;
    private WebdavClient webdav;
    private OfflineProgressNotificator offlineProgressNotificator;

    private OfflineFoldersSyncOperation syncOperation;

    private DiskFileCursor selection;

    private CredentialsManagerWithUser credentialsManager;

    private SQLiteOpenHelper2 diskDatabaseOpener;

    private FakeContentChangeNotifier contentChangeNotifier;
    private CommandLogger commandStarter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        pathLockSpy = new PathLockSpy();
        indexDatabase = new IndexDatabase(new IndexDatabaseOpenHelper(context, "index.test.db", 1));
        diskDatabaseOpener = TestObjectsFactory.createSqlite(context);
        contentChangeNotifier = new FakeContentChangeNotifier();
        diskDatabaseOpener = TestObjectsFactory.createSqlite(context);
        diskDatabase = TestObjectsFactory.createDiskDatabase(diskDatabaseOpener, contentChangeNotifier,
                pathLockSpy);
        downloadQueue = stub(DownloadQueue.class);
        storage = mock(Storage.class);
        when(storage.getStoragePath()).thenReturn("/storage/");
        when(storage.cachedFile(anyString())).thenReturn(new File("cached_file"));
        webdav = stub(WebdavClient.class);
        credentialsManager = new CredentialsManagerWithUser("user");
        offlineProgressNotificator = stub(OfflineProgressNotificator.class);

        syncOperation = createOperation();
    }

    private OfflineFoldersSyncOperation createOperation() {
        commandStarter = new CommandLogger();

        final RegularFileStorageCleaner regularFileStorageCleaner = new RegularFileStorageCleaner(storage);
        final OfflineDownloadEnqueuerFactory downloadEnqueuerFactory = new OfflineDownloadEnqueuerFactory() {
            @Override
            public OfflineDownloadEnqueuer create(String analyticsEventName) {
                return new OfflineDownloadEnqueuer(downloadQueue, diskDatabase, storage, commandStarter, analyticsEventName);
            }
        };
        final DownloadQueueCleaner downloadQueueCleaner = new DownloadQueueCleaner(downloadQueue);
        final TransactionsSender transactionsSender = new TransactionsSender(downloadQueue);
        final OfflineFolderSyncerFactory offlineFolderSyncerFactory = new OfflineFolderSyncerFactory() {
            @Override
            public OfflineFolderSyncer create(DiskItem directory) {
                return new OfflineFolderSyncer(diskDatabase, downloadEnqueuerFactory, downloadQueueCleaner, transactionsSender, directory);
            }
        };
        final RemoteRepo remoteRepo = new RemoteRepo(mock(Credentials.class), new SingleWebdavClientPool(webdav), mock(RestApiClient.class),
            mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false), mock(AppStartSessionProvider.class));
        return new OfflineFoldersSyncOperation(context, remoteRepo,
            downloadQueue, indexDatabase, diskDatabase, storage, offlineFolderSyncerFactory,
            offlineProgressNotificator, regularFileStorageCleaner, commandStarter, credentialsManager);
    }

    @Override
    protected void tearDown() throws Exception {
        if (selection != null) {
            selection.close();
        }
        diskDatabaseOpener.close();
        context.shutdown();
        super.tearDown();
        Reflector.scrub(this);
    }

    @Test
    public void testVisibleItemsShouldBeMoved() throws Exception {
        final DiskItemRow offlineDir = new DiskItemRow()
                .setIsDir(true)
                .setOfflineMark(MARKED_OFFLINE)
                .setPath("/disk", "dir");

        diskDatabase.updateOrInsert(offlineDir);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexItem(
                new IndexItem.Builder()
                        .setOp(IndexItem.Operation.dir_created_or_changed)
                        .setFullPath("/dir")
                        .setResourceId("dir.mpfsid")
                        .build());

        fakeTransportClient.addIndexItem(
                new IndexItem.Builder()
                        .setOp(IndexItem.Operation.dir_created_or_changed)
                        .setFullPath("/dir/file.txt")
                        .setResourceId("file.mpfsid")
                        .build());

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        selection = diskDatabase.queryFileByPath(new Path("/disk/dir", "file.txt"));
        selection.moveToFirst();
        assertEquals(1, selection.getCount());
        assertEquals("file.mpfsid", selection.getMpfsFileId());
        selection.close();

        selection = diskDatabase.queryFileByPath(new Path("/disk", "dir"));
        selection.moveToFirst();
        assertEquals(1, selection.getCount());
        assertEquals("dir.mpfsid", selection.getMpfsFileId());
        selection.close();
    }

    @Test
    public void testInvisibleItemShouldNotBeMoved() throws Exception {
        final DiskItemRow offlineDir = new DiskItemRow()
                .setIsDir(true)
                .setOfflineMark(MARKED_OFFLINE)
                .setPath("/disk", "dir");
        diskDatabase.updateOrInsert(offlineDir);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/dir");
        fakeTransportClient.addIndexItem(
                new IndexItem.Builder()
                        .setOp(IndexItem.Operation.file_created_or_changed)
                        .setHidden(true)//TODO this value to use in StoringToDbIndexHandler
                        .setFullPath("/dir/file.txt").build()
        );

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        selection = diskDatabase.queryFileByPath(new Path("/disk/dir", "file.txt"));
        assertEquals(0, selection.getCount());
    }

    @Test
    public void testIndexEtagShouldBeUpdated() throws Exception {
        final DiskItemRow dirRow1 = new DiskItemRow()
                .setPath("/disk", "dir1")
                .setIsDir(true).setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDatabase.updateOrInsert(dirRow1);

        final DiskItemRow dirRow2 = new DiskItemRow()
                .setPath("/disk", "dir2")
                .setIsDir(true).setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDatabase.updateOrInsert(dirRow2);

        indexDatabase.patchDirCreatedOrChanged(null, "/disk/dir1", false, false, false, false, false, false);
        indexDatabase.patchDirCreatedOrChanged(null, "/disk/dir2", false, false, false, false, false, false);

        webdav = new FakeTransportClient() {
            @Override
            public void getIndex(final String path, final String etag,
                                 final List<CustomHeader> headerList,
                                 final IndexParsingHandler handler, final String cacheDir) {
                handler.setNextEtag(path, path.contains("dir1") ? "ETAG1" : "ETAG2");
            }
        };
        syncOperation = createOperation();

        syncOperation.execute();

        final IndexDatabase.Cursor dir1Cursor = indexDatabase.queryItemByPath("/disk/dir1");
        dir1Cursor.moveToFirst();
        try {
            assertEquals("ETAG1", dir1Cursor.getIndexEtag());
        } finally {
            dir1Cursor.close();
        }

        final IndexDatabase.Cursor dir2Cursor = indexDatabase.queryItemByPath("/disk/dir2");
        dir2Cursor.moveToFirst();
        try {
            assertEquals("ETAG2", dir2Cursor.getIndexEtag());
        } finally {
            dir2Cursor.close();
        }
    }

    @Test
    public void testGettingFirstIndexShouldBeWithoutEtag() throws Exception {
        final DiskItemRow dirRow = new DiskItemRow()
                .setPath("/disk", "dir")
                .setIsDir(true).setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDatabase.updateOrInsert(dirRow);

        webdav = mock(WebdavClient.class);
        syncOperation = createOperation();
        syncOperation.execute();

        verify(webdav).getIndex(eq("/disk/dir"), isNull(), anyList(), any(StoringToDbIndexHandler.class), anyString());
    }

    @Test
    public void testGettingIncrementalIndexShouldBeWithEtag() throws Exception {
        final DiskItemRow dirRow = new DiskItemRow()
                .setPath("/disk", "dir")
                .setIsDir(true).setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDatabase.updateOrInsert(dirRow);

        indexDatabase.patchDirCreatedOrChanged(null, "/disk/dir", false, false, false, false, false, false);
        indexDatabase.updateIndexEtag("/disk/dir", "INDEX_ETAG");

        webdav = mock(WebdavClient.class);
        syncOperation = createOperation();
        syncOperation.execute();

        verify(webdav).getIndex(eq("/disk/dir"), eq("INDEX_ETAG"), anyList(), any(StoringToDbIndexHandler.class), anyString());
    }

    @Test
    public void testUpdatingDirectoryShouldBeLockedWhileCopy() throws Exception {
        FileTree.create().content(
                directory("A").setOffline(MARKED_OFFLINE)
        ).insertToDiskDatabase(diskDatabase);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/A");

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        assertThat(pathLockSpy.getRecursiveLockingLog(), equalTo(asList(new Path("/disk/A"))));
    }

    @Test
    public void testFileShouldBeDeletedIfIndexDoesNotContainIt() throws Exception {
        FileTree.create().content(
                directory("A").setOffline(MARKED_OFFLINE).content(
                        file("a").setEtag("ETAG")
                )
        ).insertToDiskDatabase(diskDatabase);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/A");

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        final DiskItem dirA = diskDatabase.queryDirectory(asPath("/disk/A"));
        assertNotNull(dirA);

        selection = diskDatabase.queryFileByPath(new Path("/disk/A/a"));

        assertEquals(0, selection.getCount());
    }

    @Test
    public void testShouldSetDisplayNameToLowcase() throws Exception {
        FileTree.create().content(
                directory("A").setOffline(MARKED_OFFLINE).content(
                        file("a").setEtag("ETAG")
                )
        ).insertToDiskDatabase(diskDatabase);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/A");
        fakeTransportClient.addIndexFileCreatedEvent("/A/aBc");

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        selection = diskDatabase.queryFileByPath(new Path("/disk/A/aBc"));
        selection.moveToFirst();

        assertEquals("abc", selection.getDisplayToLowerCase());
    }

    @Test
    public void testShouldDeleteCachedFileFromStorageIfDeletedOnServer() throws Exception {
        FileTree.create().content(directory("A").setOffline(MARKED_OFFLINE).content(file("a"))).insertToDiskDatabase(diskDatabase);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/A");

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        verify(storage).deleteFileOrFolder("/disk/A/a");
    }

    @Test
    public void testShouldStartUnmarkCommandIfDirectoryWasUnmarkedDuringSync() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE),
                        directory("B").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);
        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/A");
        fakeTransportClient.addIndexDirCreatedEvent("/B");
        webdav = fakeTransportClient;

        indexDatabase = spy(indexDatabase);
        final MarkOfflineCommandRequest unmarkDirA =
                new MarkOfflineCommandRequest(false, "/disk/A", false, false);
        doAnswer(new Answer<IndexDatabase.Cursor>() {
            @Override
            public IndexDatabase.Cursor answer(final InvocationOnMock invocation) throws Throwable {
                final IndexDatabase.Cursor result = (IndexDatabase.Cursor) invocation.callRealMethod();
                result.getCount();

                final ContentResolver cr = context.getContentResolver();
                final SelfContentProviderClient client =
                        TestObjectsFactory.createSelfContentProviderClient(context);
                MarkOfflineCommand command = new MarkOfflineCommand(diskDatabase,
                        stub(PushRegistrator.class), storage, indexDatabase, downloadQueue,
                        stub(OfflineProgressNotificator.class), new CommandLogger(),
                        stub(EventSender.class), client);
                command.execute(unmarkDirA);
                context.clearStartServiceRequests();

                return result;
            }
        }).when(indexDatabase).queryRecursively("/disk/A");

        syncOperation = createOperation();
        syncOperation.execute();

        assertThat(commandStarter.getCount(), equalTo(1));
        assertThat(commandStarter.get(0), instanceOf(MarkOfflineCommandRequest.class));
        final MarkOfflineCommandRequest request = (MarkOfflineCommandRequest) commandStarter.get(0);
        assertThat(request.getFiles(), equalTo(asList("/disk/A")));
        assertThat(request.getMarkValue(), equalTo(false));
    }

    @Test
    public void testShouldNotDownloadIndexIfDirectoryWasDeletedBeforeSync() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);
        diskDatabase = spy(diskDatabase);
        webdav = mock(WebdavClient.class);
        doAnswer(new Answer<List<String>>() {
            @Override
            public List<String> answer(final InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("unchecked") final
                List<String> result = (List<String>) invocation.callRealMethod();

                diskDatabase.deleteByPath(new Path("/disk/A"));

                return result;
            }
        }).when(diskDatabase).getOfflineDirs();

        syncOperation = createOperation();

        syncOperation.execute();

        verify(webdav, never()).getIndex(anyString(), nullable(String.class), anyList(), anyIndexParsingHandler(), anyString());
    }

    @Test
    public void testShouldNotStartCopingIfDirectoryWasDeletedBefore() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);
        diskDatabase = spy(diskDatabase);
        webdav = mock(WebdavClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                diskDatabase.deleteByPath(new Path("/disk/A"));
                return null;
            }
        }).when(webdav).getIndex(anyString(), nullable(String.class), anyList(), anyIndexParsingHandler(), anyString());

        syncOperation = createOperation();

        syncOperation.execute();

        verify(diskDatabase, never()).queryFilesRecursively(anyPath());
    }

    @Test
    public void testNewFileShouldBeQueuedToDownload() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);

        webdav = mock(WebdavClient.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final IndexParsingHandler handler = (IndexParsingHandler) args[3];
                handler.handleItem(new IndexItem.Builder()
                        .setOp(IndexItem.Operation.file_created_or_changed)
                        .setFullPath("/disk/A/a").build());
                handler.setNextEtag("/disk/A", "ETAG");
                return null;
            }
        }).when(webdav).getIndex(anyString(), nullable(String.class), anyList(), anyIndexParsingHandler(), anyString());

        syncOperation = createOperation();

        syncOperation.execute();

        assertFalse("DownloadQueue.isEmpty()", downloadQueue.isEmpty());
    }

    @Test
    public void testDirecotryShouldBeDeleteIfGettingFullIndexThrow404() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);

        webdav = mock(WebdavClient.class);
        doThrow(new IndexNotExistsException("test")).when(webdav)
                .getIndex(anyString(), isNull(), anyCustomHeader(), anyIndexParsingHandler(), anyString());

        syncOperation = createOperation();

        syncOperation.execute();

        assertThat("offline folder should be deleted", diskDatabase.queryDirectory(asPath("/disk/A")), nullValue());
    }

    @Test
    public void testDirecotryShouldBeUpdatedIfGettingInrementalIndexThrow404() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);

        final DiskItemRow dirA = new DiskItemRow()
                .setPath("/disk", "A")
                .setIsDir(true).setOfflineMark(DiskItem.OfflineMark.MARKED);
        diskDatabase.updateOrInsert(dirA);

        indexDatabase.patchDirCreatedOrChanged(null, "/disk/A", false, false, false, false, false, false);
        indexDatabase.patchDirCreatedOrChanged(null, "/disk/A/B", false, false, false, false, false, false);
        final String indexEtag = "INDEX_ETAG";
        indexDatabase.updateIndexEtag("/disk/A", indexEtag);

        webdav = mock(WebdavClient.class);
        doThrow(new IndexNotExistsException("test")).when(webdav)
                .getIndex(anyString(), eq(indexEtag), anyCustomHeader(), anyIndexParsingHandler(), anyString());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                final Object[] args = invocation.getArguments();
                final IndexParsingHandler handler = (IndexParsingHandler) args[3];
                handler.handleItem(new IndexItem.Builder()
                        .setOp(IndexItem.Operation.dir_created_or_changed)
                        .setFullPath("/A").build());

                handler.handleItem(new IndexItem.Builder()
                        .setOp(IndexItem.Operation.file_created_or_changed)
                        .setFullPath("/A/a").build());
                handler.setNextEtag("/disk/A", "ETAG");
                return null;
            }
        }).when(webdav).getIndex(nullable(String.class), isNull(), anyCustomHeader(), anyIndexParsingHandler(), nullable(String.class));

        syncOperation = createOperation();

        syncOperation.execute();

        final DiskFileCursor all = diskDatabase.queryAll();
        assertThat(asPathsList(all), equalTo(asList("/disk/A", "/disk/A/a")));
        all.close();
    }

    @Test
    public void testDirecotryShouldBeDeleteIfGettingFullIndexThrow404AfterGetInremental() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE)
                ).insertToDiskDatabase(diskDatabase);

        indexDatabase.patchDirCreatedOrChanged(null, "/disk/A", false, false, false, false, false, false);
        final String indexEtag = "INDEX_ETAG";
        indexDatabase.updateIndexEtag("/disk/A", indexEtag);

        webdav = mock(WebdavClient.class);
        doThrow(new IndexNotExistsException("test")).when(webdav)
                .getIndex(anyString(), nullable(String.class), anyCustomHeader(), anyIndexParsingHandler(), anyString());

        syncOperation = createOperation();

        syncOperation.execute();

        assertThat("offline folder should be deleted", diskDatabase.queryDirectory(asPath("/disk/A")), nullValue());
    }

    private static List<CustomHeader> anyCustomHeader() {
        return anyList();
    }

    @Test
    public void testShouldUpdateDirectorySyncState() throws Exception {
        FileTree.create()
                .content(
                        directory("A").setOffline(MARKED_OFFLINE).setSyncStatus(DiskDatabase.DirectorySyncStatus.SYNCING)
                ).insertToDiskDatabase(diskDatabase);

        final FakeTransportClient fakeTransportClient = new FakeTransportClient();
        fakeTransportClient.addIndexDirCreatedEvent("/A");

        webdav = fakeTransportClient;
        syncOperation = createOperation();
        syncOperation.execute();

        final Path pathA = asPath("/disk/A");
        final DiskItem dirA = diskDatabase.queryDirectory(pathA);
        assertThat(dirA.getETagLocal(), equalTo(DiskDatabase.DirectorySyncStatus.SYNC_FINISHED));
        assertTrue(contentChangeNotifier.getChanges().contains(pathA));
    }

    @Test
    public void testShouldNotStartCopingIfNoChangedInIndex() throws Exception {
        final File internal = new File("data", "internal");
        internal.mkdirs();

        final FileTree tree = new FileTree("disk");
        tree.create().content(
                directory("A").setOffline(MARKED_OFFLINE)
        );
        tree.createInFileSystem(internal);
        tree.insertToDiskDatabase(diskDatabase);

        diskDatabase = spy(diskDatabase);

        syncOperation = createOperation();
        syncOperation.execute();

        verify(diskDatabase, never()).queryFilesRecursively(anyPath());
    }

    private static IndexParsingHandler anyIndexParsingHandler() {
        return any(IndexParsingHandler.class);
    }

    @NonnullByDefault
    public static class FakeTransportClient extends WebdavClient {

        private final List<IndexItem> index;

        public FakeTransportClient() throws WebdavClientInitException {
            //noinspection ConstantConditions
            super(null, WebdavConfig.DEFAULT_HOST, new OkHttpClient.Builder(),
                mock(AutoUploadSettings.class), new SeparatedAutouploadToggle(false),
                mock(DeveloperSettings.class));
            index = new ArrayList<>();
        }

        @Override
        public void getIndex(final String path, final String etag, final List<CustomHeader> headerList,
                             final IndexParsingHandler handler, final String cacheDir) {
            final String pathInIndex = path.substring(DiskDatabase.ROOT_PATH.getPath().length());
            for (final IndexItem item : index) {
                final String itemPath = item.getFullPath();
                if (itemPath.equals(pathInIndex) || itemPath.startsWith(pathInIndex + "/")) {
                    try {
                        handler.handleItem(item);
                    } catch (final Exception e) {
                        Exceptions.crash(e);
                    }
                }
            }
            handler.setNextEtag(path, "ETAG");
        }

        public void addIndexItem(final IndexItem item) {
            index.add(item);
        }

        public void addIndexDirCreatedEvent(final String path) {
            addIndexItem(new IndexItem.Builder()
                    .setOp(IndexItem.Operation.dir_created_or_changed)
                    .setFullPath(path).build());
        }

        public void addIndexFileCreatedEvent(final String path) {
            addIndexItem(new IndexItem.Builder()
                    .setOp(IndexItem.Operation.file_created_or_changed)
                    .setFullPath(path).build());
        }
    }
}
