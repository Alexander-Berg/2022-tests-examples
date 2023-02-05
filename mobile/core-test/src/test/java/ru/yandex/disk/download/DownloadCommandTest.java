package ru.yandex.disk.download;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import com.yandex.disk.client.exceptions.CancelledDownloadException;
import com.yandex.disk.client.exceptions.FileNotModifiedException;
import com.yandex.disk.client.exceptions.RangeNotSatisfiableException;
import com.yandex.disk.client.exceptions.RemoteFileNotFoundException;
import com.yandex.disk.client.exceptions.WebdavNotAuthorizedException;
import org.junit.Test;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.FileTransferProgress;
import ru.yandex.disk.Mocks;
import ru.yandex.disk.connectivity.NetworkState;
import ru.yandex.disk.Storage;
import ru.yandex.disk.WifiLockWrapper;
import ru.yandex.disk.WifiLocks;
import ru.yandex.disk.download.WebdavDownloadSimulator.DownloadAnswer;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.event.EventSender;
import ru.yandex.disk.event.EventSource;
import ru.yandex.disk.event.GuavaEventBus;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.mocks.FakeRemoteRepo;
import ru.yandex.disk.offline.OfflineProgressNotificator;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskFileCursor;
import ru.yandex.disk.provider.FakeContentChangeNotifier;
import ru.yandex.disk.provider.FileTree;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.service.CommandLogger;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.sync.SyncStateManagerStub;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.test.ContextActionRequest;
import ru.yandex.disk.test.ContextActionRequestPredicate;
import ru.yandex.disk.test.IntentActionEqualsPredicate;
import ru.yandex.disk.test.IntentEqualsPredicate;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.upload.StorageListProviderStub;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.utils.FixedSystemClock;
import ru.yandex.disk.util.MediaTypes;
import ru.yandex.disk.util.SystemClock;
import ru.yandex.disk.util.error.ErrorReporter;
import ru.yandex.util.Path;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import static com.google.common.collect.Collections2.filter;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.mocks.Stubber.stub;
import static ru.yandex.disk.provider.FileTree.*;
import static ru.yandex.disk.test.MoreMatchers.anyFile;
import static ru.yandex.util.Path.asPath;

public class DownloadCommandTest extends AndroidTestCase2 {

    private static final String DATA_ETAG = "e44f9e348e41cb272efa87387728571b";
    private static final String DATA = "DATA";
    private static final int DATA_LENGTH = DATA.length();
    private String externalStorageRoot;

    private SeclusiveContext context;
    private DownloadCommand command;
    private DownloadQueue queue;
    private DiskDatabase diskDatabase;
    private FakeContentChangeNotifier contentChangeNotifier;

    private DownloadAnswer returnData;
    private DownloadAnswer returnSecondPartAnswer;
    private DownloadAnswer returnFirstPartAndThrowIOException;
    private WebdavDownloadSimulator webdavDownloadSimulator;

    private UnfinishedFileContentAccessor cache;
    private UnfinishedFileContentAccessor externalStorage;
    private DownloadAnswer throwIOException;
    private FixedSystemClock systemClock;
    private DownloadProcessState processState;
    private DownloadNotifier notifier;
    private EventSender sender;
    private CommandLogger commandLogger;
    private ApplicationStorage applicationStorage;
    private Storage storage;
    private WifiLocks wifiLocks;
    private WebdavClient.Pool webdavClientPool;
    private ErrorReporter errorReporter;
    private CredentialsManagerWithUser credentialsManager;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        context = new SeclusiveContext(mContext);
        Mocks.addContentProviders(context);
        Mocks.initCredentials(context);
        wifiLocks = new WifiLocks(mock(WifiLockWrapper.class), mock(WifiLockWrapper.class),
                mock(NetworkState.class));

        queue = TestObjectsFactory.createDownloadQueue(context);
        contentChangeNotifier = new FakeContentChangeNotifier();
        context.setActivityManager(Mocks.mockActivityManager());
        diskDatabase = TestObjectsFactory.createDiskDatabase(TestObjectsFactory.createSqlite(context),
                contentChangeNotifier, null);
        commandLogger = new CommandLogger();
        applicationStorage = TestObjectsFactory.createApplicationStorage(context,
                stub(ApplicationSettings.class), stub(CredentialsManager.class),
                request -> commandLogger.start(request),
                new StorageListProviderStub(), mock(Diagnostics.class));
        credentialsManager = new CredentialsManagerWithUser("test.user");
        Credentials credentials = credentialsManager.getActiveAccountCredentials();
        storage = TestObjectsFactory.createStorage(credentials, applicationStorage, diskDatabase, queue, stub(Storage.CacheStateObserver.class));
        webdavClientPool = mock(WebdavClient.Pool.class);

        GuavaEventBus eventBus = new GuavaEventBus(mock(Diagnostics.class));

        systemClock = new FixedSystemClock();
        notifier = spy(new DownloadNotifier(SystemClock.REAL, eventBus));
        processState = new DownloadProcessState(notifier);
        sender = new EventLogger();
        errorReporter = mock(ErrorReporter.class);

        command = createCommand();

        returnData = new DownloadAnswer()
                .setEtag(DATA_ETAG)
                .setContentType("text/plain")
                .setData(DATA);

        returnFirstPartAndThrowIOException = new DownloadAnswer()
                .setEtag(DATA_ETAG)
                .setData("DA")
                .setContentLength(DATA_LENGTH)
                .setExceptionDuringDownloading(new IOException("test"));

        throwIOException = new DownloadAnswer()
                .setCheckRequest(false)
                .setExceptionBeforeDownloading(new IOException("no network"));

        returnSecondPartAnswer = new DownloadAnswer()
                .setExpectedLocalLength(2)
                .setExpectedEtag(DATA_ETAG)
                .setData("TA")
                .setContentLength(DATA_LENGTH);

        WebdavClient webdavClient = mock(WebdavClient.class);
        when(webdavClientPool.getClient(any(), any())).thenReturn(webdavClient);

        webdavDownloadSimulator = new WebdavDownloadSimulator(webdavClient);

        cache = new UnfinishedFileContentAccessor(storage.getStoragePath());
        externalStorageRoot = Environment.getExternalStorageDirectory().getPath();
        externalStorage = new UnfinishedFileContentAccessor(externalStorageRoot);
        externalStorage.deleteUnfinishedFiles();

        storage.dropUserCachedFiles();
    }

    private DownloadCommand createCommand() {
        return new DownloadCommand(
            context,
            diskDatabase,
            queue,
            new SyncStateManagerStub(),
            storage,
            applicationStorage,
            systemClock,
            processState,
            stub(OfflineProgressNotificator.class),
            sender,
            commandLogger,
            wifiLocks,
            new FakeRemoteRepo(webdavClientPool),
            (r, l) -> {},
            errorReporter,
            credentialsManager
        );
    }

    @Test
    public void testSuccessDownload() throws Exception {
        Path dest = asPath("a");
        externalStorage.delete(dest);

        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        String downloadedContent = externalStorage.read(dest);
        assertEquals(DATA, downloadedContent);
    }

    @Test
    public void testDownloadProcessStateAfterSuccessDownload() throws Exception {
        prepareToDownload("a");
        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        assertFalse(processState.isNotSpaceEnoughInStorage());
    }

    @Test
    public void testDownloadProcessStateIfNotEnoughSpace() throws Exception {
        storage = mock(Storage.class);
        when(storage.getFreeSpaceLimited(anyFile(), anyLong())).thenReturn(0L);

        command = createCommand();

        webdavDownloadSimulator.addAnswer(new DownloadAnswer().setContentLength(100));

        prepareToDownload("a");

        command.execute(new DownloadCommandRequest());

        assertTrue(processState.isNotSpaceEnoughInStorage());
    }

    @Test
    public void testProgressStateSavedOnIOException() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        prepareToDownload("a");

        command.execute(new DownloadCommandRequest());

        FileTransferProgress progress = processState.getProgress();
        assertNotNull(progress);
        assertEquals(DATA_LENGTH / 2, progress.getLoaded());
        assertEquals(DATA_LENGTH, progress.getTotal());
    }

    @Test
    public void testProgressStateResetOnFileDownloadException() throws Exception {
        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                .setEtag(DATA_ETAG)
                .setData(DATA)
                .setContentLength(100)
                .setExceptionDuringDownloading(new RemoteFileNotFoundException("test")));

        command.execute(new DownloadCommandRequest());

        FileTransferProgress progress = processState.getProgress();
        assertNull(progress);
    }

    @Test
    public void testLastProgressResetIfQueueEmpty() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        prepareToDownload("a");

        command.execute(new DownloadCommandRequest());

        queue.poll();

        command.execute(new DownloadCommandRequest());

        FileTransferProgress progress = processState.getProgress();
        assertNull(progress);
    }

    @Test
    public void testProgressStateResetOnCancelException() throws Exception {
        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                .setEtag(DATA_ETAG)
                .setData(DATA)
                .setContentLength(100)
                .setExceptionDuringDownloading(new CancelledDownloadException()));

        command.execute(new DownloadCommandRequest());

        FileTransferProgress progress = processState.getProgress();
        assertNull(progress);
    }

    @Test
    public void testRestoreProgressOnFirstLoad() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        prepareToDownload("a", DownloadQueueItem.Type.SYNC);

        command.execute(new DownloadCommandRequest());

        fillIOExceptionAnswers();
        webdavDownloadSimulator.addAnswer(throwIOException);

        command.execute(new DownloadCommandRequest());

        FileTransferProgress progress = processState.getProgress();
        assertNotNull(progress);
        assertEquals(DATA_LENGTH / 2, progress.getLoaded());
        assertEquals(DATA_LENGTH, progress.getTotal());
    }

    @Test
    public void testNotifyGalleryAfterFileExport() throws Exception {
        prepareToDownload("a", DownloadQueueItem.Type.UI);

        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                .setData(Uri.fromFile(new File(externalStorageRoot + "/a")));

        LinkedList<ContextActionRequest> broadcasts = context.getSendBroadcastRequests();
        assertFalse(filter(broadcasts,
                new ContextActionRequestPredicate(new IntentEqualsPredicate(intent))).isEmpty());
    }

    @Test
    public void testDoNotNotifyGalleryAfterFileSync() throws Exception {
        prepareToDownload("a", DownloadQueueItem.Type.SYNC);

        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        LinkedList<ContextActionRequest> broadcasts = context.getSendBroadcastRequests();
        assertTrue(filter(broadcasts, new ContextActionRequestPredicate(
                new IntentActionEqualsPredicate(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE))).isEmpty());
    }

    @Test
    public void testCopyCachedFileIfItMatchesToServer() throws Exception {
        Path src = asPath("/disk/a");
        cache.write(src, DATA);

        File savedFile = new File(externalStorageRoot, "a");
        savedFile.delete();
        assertFalse(savedFile.exists());

        addExportTask(src);

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                .setExpectedEtag(DATA_ETAG)
                .setExceptionBeforeDownloading(new FileNotModifiedException()));

        command.execute(new DownloadCommandRequest());

        assertThat(externalStorage.getRoot().getPath(), equalTo(externalStorageRoot));

        assertTrue(savedFile.exists());
    }

    @Test
    public void testOfflineFileDownloadContinuesAfterIOException() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        Path src = asPath("/disk/a");
        addSyncFileTask(src);

        command.execute(new DownloadCommandRequest());

        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer);

        command.execute(new DownloadCommandRequest());

        String downloadedContent = cache.read(src);
        assertEquals(DATA, downloadedContent);
    }

    @Test
    public void testFileExportContinuesAfterIOException() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        Path dest = asPath("a");
        externalStorage.delete(dest);

        Path src = asPath("/disk/a");
        addExportTask(src);

        command.execute(new DownloadCommandRequest());

        addExportTask(src);

        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer);

        command.execute(new DownloadCommandRequest());

        String downloadedContent = externalStorage.read(dest);
        assertEquals(DATA, downloadedContent);
    }

    @Test
    public void testFileCachingContinuesAfterIOException() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        Path src = asPath("/disk/a");
        addCacheFileTask(src);

        command.execute(new DownloadCommandRequest());

        addCacheFileTask(src);

        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer);

        command.execute(new DownloadCommandRequest());

        String downloadedContent = cache.read(src);
        assertEquals(DATA, downloadedContent);
    }

    @Test
    public void testDeleteUnfinishedFileNotIOException() throws Exception {
        Path src = asPath("/disk/a");
        addExportTask(src);

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                        .setEtag(DATA_ETAG)
                        .setData(DATA)
                        .setExceptionDuringDownloading(new WebdavNotAuthorizedException("test"))
        );

        command.execute(new DownloadCommandRequest());

        assertEquals(0, externalStorage.listUnfinishedFiles().length);
    }

    @Test
    public void testContinueUnfinishedFileIfSyncItemAddedAfterUIItemInterruptedByIOException() throws Exception {
        Path src = asPath("/disk/a");
        addCacheFileTask(src);

        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        command.execute(new DownloadCommandRequest());
        assertEquals(1, cache.listUnfinishedFiles("disk").length);

        queue.poll();
        queue.add(DownloadQueueItem.Type.SYNC, src, null, 0, 0);

        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer);

        command.execute(new DownloadCommandRequest());

        assertEquals(0, externalStorage.listUnfinishedFiles().length);

        String downloadedContent = cache.read(src);
        assertEquals(DATA, downloadedContent);
    }

    @Test
    public void testDoNotDeleteUnfinishedIfAnotherSyncItemAdded() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        Path src = asPath("/disk/a");
        addExportTask(src);

        command.execute(new DownloadCommandRequest());
        assertEquals(1, externalStorage.listUnfinishedFiles().length);

        queue.poll();
        addSyncFileTask(src);

        command.execute(new DownloadCommandRequest());

        assertEquals(1, externalStorage.listUnfinishedFiles().length);
    }

    @Test
    public void testDeleteUnfinishedIfAnotherUIItemAdded() throws Exception {
        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();
        webdavDownloadSimulator.addAnswer(returnData);

        Path src = asPath("/disk/a");
        addExportTask(src);

        command.execute(new DownloadCommandRequest());

        assertEquals(1, externalStorage.listUnfinishedFiles().length);

        queue.poll();
        addCacheFileTask(src);

        command.execute(new DownloadCommandRequest());

        assertEquals(0, externalStorage.listUnfinishedFiles().length);
    }

    @Test
    public void testContinueUnfinishedFileIfSyncItemInterruptedByUI() throws Exception {
        final Path src = asPath("/disk/a");
        queue.add(DownloadQueueItem.Type.SYNC, src, null, 0, 0);
        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                .setEtag(DATA_ETAG)
                .setData("DA")
                .setActionDuringDownload(new Runnable() {
                    @Override
                    public void run() {
                        queue.add(DownloadQueueItem.Type.UI, src, null, 0, 0);
                    }
                }));

        command.execute(new DownloadCommandRequest());
        assertEquals(1, cache.listUnfinishedFiles("disk").length);

        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer);

        command.execute(new DownloadCommandRequest());

        assertEquals(0, externalStorage.listUnfinishedFiles().length);

        String downloadedContent = cache.read(src);
        assertEquals(DATA, downloadedContent);
    }

    @Test
    public void testDeleteUnfinishedFileAndQueueItemIfRemoteFileNotFoundOnServer() throws Exception {
        Path src = asPath("/disk/a");
        addSyncFileTask(src);

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                .setExceptionBeforeDownloading(new RemoteFileNotFoundException("test")));

        command.execute(new DownloadCommandRequest());

        assertEquals(0, externalStorage.listUnfinishedFiles().length);
        assertNull(queue.poll());
    }

    @Test
    public void testDeleteUnfinishedFileButNotDeleteQueueItemIfRangeNotSatisfiable() throws Exception {
        Path src = asPath("/disk/a");
        addSyncFileTask(src);

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                .setExceptionBeforeDownloading(new RangeNotSatisfiableException("test")));

        command.execute(new DownloadCommandRequest());

        assertEquals(0, externalStorage.listUnfinishedFiles().length);
        assertNotNull(queue.poll());
    }

    @Test
    public void testUpdateEtagLocalInDiskDatabaseOnSuccessDownload() throws Exception {
        prepareToDownload("a", DownloadQueueItem.Type.SYNC);
        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        DiskFileCursor all = diskDatabase.queryAll();
        DiskItem file = all.get(0);
        assertEquals("/disk/a", file.getPath());

        assertEquals(DATA_ETAG, file.getETagLocal());
    }

    @Test
    public void testUpdateDirectorySyncStatusOnSuccessDownload() throws Exception {
        FileTree.create().content(
                directory("A").content(
                        file("a").setEtag(DATA_ETAG)
                )
        ).insertToDiskDatabase(diskDatabase);
        diskDatabase.updateSyncStatus(asPath("/disk/A"));

        addItemToDownloadQueue(DownloadQueueItem.Type.SYNC, "/disk/A/a");

        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        DiskFileCursor all = diskDatabase.queryAll();
        DiskItem dirA = all.get(0);
        assertEquals("/disk/A", dirA.getPath());

        assertEquals(null, dirA.getETagLocal());
    }

    @Test
    public void testShouldUpdateContentDescriptionInDiskDatabase() throws Exception {
        FileTree.create()
                .content(
                        file("a.txt").setEtag(DATA_ETAG).setContentType(null)
                ).insertToDiskDatabase(diskDatabase);
        addItemToDownloadQueue(DownloadQueueItem.Type.SYNC, "/disk/a.txt");

        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        DiskFileCursor all = diskDatabase.queryAll();
        DiskItem fileA = all.get(0);
        assertEquals("/disk/a.txt", fileA.getPath());

        assertEquals("text/plain", fileA.getMimeType());
        assertEquals(true, MediaTypes.isDocument(fileA.getMediaType()));
        assertEquals(true, fileA.getHasThumbnail());
        assertEquals(DATA_LENGTH, fileA.getSize());
    }

    @Test
    public void testShouldNotUpdateContentDescriptionInDiskDatabaseIfCachedContentDescriptionUpToDate() throws Exception {
        FileTree.create().content(
                file("a.txt")
                        .setEtag(DATA_ETAG)
                        .setContentType("application/vnd.ms-excel").setMediaType("document")
                        .setHasThumbnail(true)
        ).insertToDiskDatabase(diskDatabase);
        addItemToDownloadQueue(DownloadQueueItem.Type.SYNC, "/disk/a.txt");

        returnData.setContentType("application/vnd.ms-excel");
        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        DiskFileCursor all = diskDatabase.queryAll();
        DiskItem fileA = all.get(0);
        assertEquals("/disk/a.txt", fileA.getPath());

        assertEquals("application/vnd.ms-excel", fileA.getMimeType());
        assertEquals(true, MediaTypes.isDocument(fileA.getMediaType()));
        assertEquals(true, fileA.getHasThumbnail());
    }

    @Test
    public void testShouldNotifyFileChangedAfterDownload() throws Exception {
        FileTree.create().content(
                directory("A").content(
                        file("a").setEtag(DATA_ETAG)
                )
        ).insertToDiskDatabase(diskDatabase);
        addItemToDownloadQueue(DownloadQueueItem.Type.SYNC, "/disk/A/a");

        webdavDownloadSimulator.addAnswer(returnData);

        command.execute(new DownloadCommandRequest());

        assertThat(contentChangeNotifier.getChanges(), equalTo(asList(asPath("/disk/A/a"))));
    }

    @Test
    public void testShouldContinueAfterOneIOException() throws Exception {
        Path dest = asPath("a");
        externalStorage.delete(dest);

        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer);

        command.execute(new DownloadCommandRequest());

        String downloadedContent = externalStorage.read(dest);
        assertEquals(DATA, downloadedContent);

        assertThat(queue.getInactiveItems().size(), equalTo(0));
    }

    @Test
    public void testShouldContinueAfterTimeout() throws Exception {
        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer.setActionDuringDownload(new Runnable() {
            @Override
            public void run() {
                assertThat(systemClock.elapsedRealtime(), equalTo(1000L));
            }
        }));

        command.execute(new DownloadCommandRequest());
    }

    @Test
    public void testShouldNotSendErrorNotificationAfterFirstIOException() throws Exception {
        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        webdavDownloadSimulator.addAnswer(returnSecondPartAnswer.setActionDuringDownload(new Runnable() {
            @Override
            public void run() {
                verify(notifier, never()).sendTaskFailedEvent(anyLong());
            }
        }));

        command.execute(new DownloadCommandRequest());
    }

    @Test
    public void testShouldSendErrorNotificationAfterSecondIOException() throws Exception {
        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(returnFirstPartAndThrowIOException);
        fillIOExceptionAnswers();

        command.execute(new DownloadCommandRequest());

        verify(notifier).sendIOError(anyLong(), any());
    }

    @Test
    public void testShouldContinueAfterIOExceptionIfGetSomeData() throws Exception {
        Path dest = asPath("a");
        externalStorage.delete(dest);

        prepareToDownload("a");

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                        .setEtag(DATA_ETAG)
                        .setData("D")
                        .setContentLength(DATA_LENGTH)
                        .setExceptionDuringDownloading(new IOException("test"))
        );

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                        .setData("A")
                        .setExpectedLocalLength(1)
                        .setExpectedEtag(DATA_ETAG)
                        .setContentLength(DATA_LENGTH)
                        .setExceptionDuringDownloading(new IOException("test"))
        );

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                        .setData("T")
                        .setExpectedLocalLength(2)
                        .setExpectedEtag(DATA_ETAG)
                        .setContentLength(DATA_LENGTH)
                        .setExceptionDuringDownloading(new IOException("test"))
        );

        webdavDownloadSimulator.addAnswer(new DownloadAnswer()
                        .setData("A")
                        .setExpectedLocalLength(3)
                        .setExpectedEtag(DATA_ETAG)
                        .setContentLength(DATA_LENGTH)
        );

        command.execute(new DownloadCommandRequest());

        String downloadedContent = externalStorage.read(dest);
        assertEquals(DATA, downloadedContent);

        assertThat(queue.getInactiveItems().size(), equalTo(0));
    }

    private String prepareToDownload(String fileName) {
        return prepareToDownload(fileName, DownloadQueueItem.Type.UI);
    }

    private String prepareToDownload(String fileName, DownloadQueueItem.Type type) {
        Path serverPath = new Path(DiskDatabase.ROOT_PATH, fileName);

        insertToDB(fileName);
        addItemToDownloadQueue(type, serverPath);

        return serverPath.toString();
    }

    private void addExportTask(Path src) {
        addItemToDownloadQueue(DownloadQueueItem.Type.UI, src);
    }

    private void addCacheFileTask(Path src) {
        queue.add(DownloadQueueItem.Type.UI, src, null, 0, 0);
    }

    private void addSyncFileTask(Path src) {
        addItemToDownloadQueue(DownloadQueueItem.Type.SYNC, src);
    }

    private void addItemToDownloadQueue(DownloadQueueItem.Type type, String serverPath) {
        addItemToDownloadQueue(type, asPath(serverPath));
    }

    private void addItemToDownloadQueue(DownloadQueueItem.Type type, Path serverPath) {
        Path localPath = type == DownloadQueueItem.Type.UI ? externalStorage.getRootPath() : null;
        queue.add(type, serverPath, localPath, 0, DATA_LENGTH);
    }

    private void insertToDB(String fileName) {
        FileTree tree = new FileTree();
        tree.root().content(file(fileName));
        tree.insertToDiskDatabase(diskDatabase);
    }

    private void fillIOExceptionAnswers() {
        for (int i = 0; i < DownloadQueueProcessor.RETRY_MAX; i++) {
            webdavDownloadSimulator.addAnswer(throwIOException);
        }
    }
}
