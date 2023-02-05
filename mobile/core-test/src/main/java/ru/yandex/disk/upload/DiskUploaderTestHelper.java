package ru.yandex.disk.upload;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.MatrixCursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import com.google.common.collect.Lists;
import com.yandex.pulse.histogram.StubHistogram;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.ApplicationStorage;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.Mocks;
import ru.yandex.disk.ServerDiskItem;
import ru.yandex.disk.Storage;
import ru.yandex.disk.WifiLockWrapper;
import ru.yandex.disk.WifiLocks;
import ru.yandex.disk.asyncbitmap.GeneratePreviewCommandRequest;
import ru.yandex.disk.autoupload.AutouploadCheckDebouncer;
import ru.yandex.disk.autoupload.AutouploadDecider;
import ru.yandex.disk.autoupload.AutouploadQueueHelper;
import ru.yandex.disk.autoupload.AutouploadQueueHelperKt;
import ru.yandex.disk.autoupload.observer.StorageListProvider;
import ru.yandex.disk.autoupload.recent.RecentAutouploadsDatabase;
import ru.yandex.disk.background.BackgroundWatcher;
import ru.yandex.disk.commonactions.FetchCapacityInfoCommandRequest;
import ru.yandex.disk.commonactions.SingleWebdavClientPool;
import ru.yandex.disk.connectivity.NetworkState;
import ru.yandex.disk.connectivity.NetworkStateN;
import ru.yandex.disk.download.DownloadQueue;
import ru.yandex.disk.event.EventLogger;
import ru.yandex.disk.event.EventSender;
import ru.yandex.disk.event.EventSource;
import ru.yandex.disk.gallery.data.command.MergePhotosliceCommandRequest;
import ru.yandex.disk.gallery.data.command.ProcessUploadedFileCommandRequest;
import ru.yandex.disk.gallery.data.provider.MediaStoreProvider;
import ru.yandex.disk.gallery.data.provider.MediaStoreProviderImpl;
import ru.yandex.disk.imports.ImportingFilesStorage;
import ru.yandex.disk.mocks.CredentialsManagerWithUser;
import ru.yandex.disk.domain.albums.BucketAlbum;
import ru.yandex.disk.domain.albums.BucketAlbumId;
import ru.yandex.disk.photoslice.MomentsDatabase;
import ru.yandex.disk.provider.BucketAlbumsProvider;
import ru.yandex.disk.provider.DH;
import ru.yandex.disk.provider.DiskContentProvider;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.DiskContract.Queue;
import ru.yandex.disk.provider.DiskDatabase;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.provider.DiskUploadQueueCursor;
import ru.yandex.disk.provider.SafeContentResolver;
import ru.yandex.disk.remote.RemoteRepo;
import ru.yandex.disk.remote.RestApiClient;
import ru.yandex.disk.remote.ServerConstants;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.remote.webdav.WebdavClient.UploadListener;
import ru.yandex.disk.replication.NeighborsContentProviderClient;
import ru.yandex.disk.replication.SelfContentProviderClient;
import ru.yandex.disk.service.Command;
import ru.yandex.disk.service.CommandRequest;
import ru.yandex.disk.service.CommandScheduler;
import ru.yandex.disk.service.CommandStarter;
import ru.yandex.disk.service.TestCommandStarter;
import ru.yandex.disk.settings.ApplicationSettings;
import ru.yandex.disk.settings.AutoUploadSettings;
import ru.yandex.disk.settings.PaidAutoUploadSettingsManager;
import ru.yandex.disk.settings.UserSettings;
import ru.yandex.disk.stats.AnalyticsAgent;
import ru.yandex.disk.stats.EventLog;
import ru.yandex.disk.stats.EventLogSettings;
import ru.yandex.disk.test.SeclusiveContext;
import ru.yandex.disk.test.TestEnvironment;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.toggle.LimitedPhotosToggle;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;
import ru.yandex.disk.toggle.UiMultiAccountsToggle;
import ru.yandex.disk.ui.ActivityTracker;
import ru.yandex.disk.upload.hash.CalculatingFileHashesObtainer;
import ru.yandex.disk.upload.hash.Hash;
import ru.yandex.disk.upload.hash.HashCalculator;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.util.Exceptions;
import ru.yandex.disk.util.FileSystem;
import ru.yandex.disk.utils.DiskBatteryManager;
import ru.yandex.disk.utils.FixedSystemClock;
import ru.yandex.disk.util.IOHelper;
import ru.yandex.disk.util.MetaDataTools;
import ru.yandex.disk.util.NetworkAvailabilityInterceptor;
import ru.yandex.disk.util.SystemClock;
import ru.yandex.disk.util.SystemClockWrapper;
import ru.yandex.disk.util.error.ErrorReporter;
import ru.yandex.util.Path;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.mocks.Stubber.stub;
import static ru.yandex.disk.test.MoreMatchers.anyFile;
import static ru.yandex.disk.util.Collections3.asArrayList;

public class DiskUploaderTestHelper {

    static final String[] MEDIASTORE_PROJ = new String[]{
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.DATE_ADDED
    };

    private SeclusiveContext context;
    private WebdavClient webdav;
    private DiskUploader uploader;
    private DiskDatabase diskDatabase;
    private UploadCommand uploadCommand;
    private QueueAutouploadsCommand queueAutouploadsCommand;
    private QueueUploadsCommand queueUploadsCommand;
    private ConnectivityManager connectivityManager;
    private long fileCreationDate;
    private ContentProvider mockMediaProvider;
    private NetworkState networkState;
    private CredentialsManager cm;

    private int expectRestartUploadTask;

    private SystemClockWrapper systemClockInDiskUploader;

    private FixedSystemClock systemClock;

    public static final int UPLOAD_LISTENER_INDEX = 10;
    private ApplicationSettings applicationSettings;
    private final DiskContentProvider contentProvider;
    private UploadQueue uploadQueue;
    public TestCommandStarter commandStarter;
    private final SelfContentProviderClient selfClient;
    private final NeighborsContentProviderClient client;
    private final BucketAlbumsProvider bucketAlbumsProvider;

    public DiskUploaderTestHelper(Context baseContext) throws Exception {
        context = new SeclusiveContext(baseContext);
        context.setWifiManager(Mocks.mockWifiManager());

        context.setActivityManager(Mocks.mockActivityManager());
        final DH db = new DH(context);
        contentProvider = spy(Mocks.createDiskContentProvider(context, db));
        Mocks.addContentProvider(context, contentProvider, DiskContentProvider.getAuthority(context));
        cm = new CredentialsManagerWithUser("user");
        applicationSettings = TestObjectsFactory.createApplicationSettings(context);
        final ContentResolver cr = context.getContentResolver();
        selfClient = TestObjectsFactory.createSelfContentProviderClient(context, null);
        client = TestObjectsFactory.createNeighborsProviderClient(context);
        final StorageListProvider storageListProviderStub = new StorageListProviderStub();

        ApplicationStorage applicationStorage = TestObjectsFactory.createApplicationStorage(this.context,
                applicationSettings, stub(CredentialsManager.class),
                mock(CommandStarter.class), storageListProviderStub, mock(Diagnostics.class));

        Storage storage = TestObjectsFactory.createStorage(new CredentialsManagerWithUser("test.user").getActiveAccountCredentials(),
            applicationStorage, diskDatabase, mock(DownloadQueue.class), mock(Storage.CacheStateObserver.class));

        commandStarter = new TestCommandStarter();

        webdav = mock(WebdavClient.class);
        mockMediaProvider = mock(ContentProvider.class);

        systemClockInDiskUploader = new SystemClockWrapper();

        systemClock = new FixedSystemClock();
        systemClockInDiskUploader.setWrappee(systemClock);
        connectivityManager = mock(ConnectivityManager.class);
        final CommandScheduler commandScheduler = mock(CommandScheduler.class);
        networkState = new NetworkStateN(connectivityManager, mock(EventSender.class),
                commandScheduler, mock(CommandStarter.class),
                mock(CredentialsManager.class), mock(Handler.class), mock(Context.class),
                mock(SystemClock.class), mock(NetworkAvailabilityInterceptor.class),
                mock(DeveloperSettings.class), mock(AutouploadCheckDebouncer.class));

        setConnectivityManagerState(true);

        this.context.setConnectivityManager(connectivityManager);

        AutoUploadSettings settings = getSettings();
        settings.setUploadPhotoWhen(AutoUploadSettings.UploadWhen.ALWAYS);
        settings.setSettingsWereSetManually(true);
        settings.setSettingsSendingStatus(AutoUploadSettings.SettingsSendingStatus.SUCCESS);
        EventLogger eventLogger = new EventLogger();

        initMocks();

        uploadQueue = TestObjectsFactory.createUploadQueue(client, selfClient, db, cr,
            storageListProviderStub, cm.getActiveAccountCredentials());

        WebdavClient.Pool webdavClientPool = new SingleWebdavClientPool(webdav);
        WifiLocks wifiLocks = new WifiLocks(mock(WifiLockWrapper.class), mock(WifiLockWrapper.class),
                networkState);
        uploader = new DiskUploader(eventLogger, mock(EventSource.class), commandStarter);

        Credentials credentials = cm.getActiveAccountCredentials();
        UserSettings userSettings = applicationSettings.getUserSettings(credentials);
        DeveloperSettings developerSettings = new DeveloperSettings(PreferenceManager.getDefaultSharedPreferences(context));
        AppStartSessionProvider appStartSessionProvider = mock(AppStartSessionProvider.class);

        RemoteRepo remoteRepo = new RemoteRepo(credentials, webdavClientPool, mock(RestApiClient.class),
            mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false), appStartSessionProvider);

        final AutouploadDecider autouploadDecider = mock(AutouploadDecider.class);
        when(autouploadDecider.shouldAutoupload()).thenReturn(true);
        final ImportingFilesStorage importingFilesStorage = new ImportingFilesStorage(storage);

        final HashMap<Integer, UploadProcessor> processors = new HashMap<>();
        final WebdavUploadProcessor webdavUploadProcessor = new WebdavUploadProcessor(remoteRepo);
        processors.put(Queue.UploadItemType.DEFAULT, webdavUploadProcessor);
        processors.put(Queue.UploadItemType.AUTOUPLOAD, webdavUploadProcessor);

        diskDatabase = mock(DiskDatabase.class);
        final MetaDataTools metaDataTools = mock(MetaDataTools.class);
        when(metaDataTools.extractExifTime(anyFile())).thenReturn(0L);

        final AnalyticsAgent analyticsAgent = mock(AnalyticsAgent.class);
        EventLog.init(true, mock(EventLogSettings.class), analyticsAgent);
        when(analyticsAgent.getHistogram(any())).thenReturn(StubHistogram.INSTANCE);

        final Diagnostics diagnostics = mock(Diagnostics.class);
        final RecentAutouploadsDatabase mockRecentAutouploadsDatabase = mock(RecentAutouploadsDatabase.class);
        when(mockRecentAutouploadsDatabase.queryUploadedStat(anyInt(), anyInt())).thenReturn(AutouploadQueueHelperKt.getSTAT_NO_DELAY());
        final AutouploadQueueHelper autouploadHelper = new AutouploadQueueHelper(
            uploadQueue, mockRecentAutouploadsDatabase,
            diagnostics, mock(SharedPreferences.class), mock(AutoUploadSettings.class),
            mock(NetworkState.class), mock(ActivityTracker.class));

        bucketAlbumsProvider = mock(BucketAlbumsProvider.class);

        uploadCommand = new UploadCommand(
            credentials, userSettings, importingFilesStorage, uploadQueue, wifiLocks,
            uploader, remoteRepo, diskDatabase, mock(MomentsDatabase.class),
            eventLogger, systemClockInDiskUploader, cm,
            networkState, commandStarter, FileSystem.getInstance(),
            autouploadDecider, autouploadHelper, mock(BackgroundWatcher.class),
            processors, mock(ErrorReporter.class),
            new CalculatingFileHashesObtainer(new JavaSEHashCalculator()), metaDataTools, mock(UploadNotificator.class),
            AccessMediaLocationCoordinator.Stub.INSTANCE,
            mock(DiskBatteryManager.class),
            new SeparatedAutouploadToggle(false), bucketAlbumsProvider, mock(CycledAutoUploadingDetector.class),
            new UploadProgressCapacityFetcher(commandStarter, systemClock), mock(AutouploadNotificator.class),
            developerSettings,
            mock(PaidAutoUploadSettingsManager.class),
            new LimitedPhotosToggle(true, false),
            mock(LimitedPhotosHandler.class),
            new UiMultiAccountsToggle(false)
        );

        final SafeContentResolver safeContentResolver = new SafeContentResolver(context.getContentResolver(), diagnostics);

        final MediaStoreProvider mediaStoreProvider = new MediaStoreProviderImpl(context, safeContentResolver);

        final MediaStoreCollector mediaStoreCollector = new MediaStoreCollector(
            mediaStoreProvider, commandScheduler);

        setAlbumAutouploadEnabled(true);

        queueAutouploadsCommand = new QueueAutouploadsCommand(userSettings,
            bucketAlbumsProvider, mediaStoreCollector, autouploadHelper, uploader, new EventLogger(),
            commandStarter, new SeparatedAutouploadToggle(false));

        queueUploadsCommand = new QueueUploadsCommand(uploader, eventLogger, uploadQueue,
                importingFilesStorage, diskDatabase, metaDataTools, commandStarter);

        commandStarter.registerCommand(UploadCommandRequest.class, uploadCommand);
        commandStarter.registerCommand(QueueAutouploadsCommandRequest.class, queueAutouploadsCommand);
        commandStarter.registerCommand(QueueUploadsCommandRequest.class, queueUploadsCommand);
        commandStarter.registerCommand(GeneratePreviewCommandRequest.class, mock(Command.class));
        commandStarter.registerCommand(ProcessUploadedFileCommandRequest.class, mock(Command.class));
        commandStarter.registerCommand(MergePhotosliceCommandRequest.class, mock(Command.class));
        commandStarter.registerCommand(CheckForNewAlbumsCommandRequest.class, mock(Command.class));
        commandStarter.registerCommand(FetchCapacityInfoCommandRequest.class, mock(Command.class));
        commandStarter.registerCommand(PauseHugeNotAutouploadFilesCommandRequest.class, mock(Command.class));
    }

    public void initMocks() throws Exception {

        MatrixCursor mediastoreCursor = new MatrixCursor(MEDIASTORE_PROJ);
        when(mockMediaProvider
                .query(any(Uri.class), nullable(String[].class), nullable(String.class),
                        nullable(String[].class), nullable(String.class)))
                .thenReturn(mediastoreCursor);

        Mocks.addContentProvider(context, mockMediaProvider, "media");

        prepareMetadataAnswers(Collections.emptyList());
    }

    public UploadQueue getUploadQueue() {
        return uploadQueue;
    }

    public SeclusiveContext getContext() {
        return context;
    }

    public DiskUploader getDiskUploader() {
        return uploader;
    }

    public WebdavClient getWebdav() {
        return webdav;
    }

    public BucketAlbumsProvider getBucketAlbumsProvider() {
        return bucketAlbumsProvider;
    }

    public void startUploadFiles(String destination, String... src) {
        startUploadFiles(false, destination, src);
    }

    public void startUploadFiles(boolean allTasks, String destination, String... src) {
        startUpload(makeUploadRequest(destination, src), allTasks);
    }

    public DiskUploadQueueCursor queryQueue() {
        Credentials activeAccount = cm.getActiveAccountCredentials();
        String queueUri = Queue.makeQueuePath(activeAccount.getUser(), null);
        String order =
                DiskContract.Queue.IS_DIR + ", "
                        + DiskContract.Queue.DEST_DIR
                        + ", " + DiskContract.Queue.SRC_NAME;
        Cursor queue = selfClient.query(queueUri, TestUploadQueue.getUploadItemDefaultProjection(), null, null, order);
        return new DiskUploadQueueCursor(queue);
    }

    public File makeFileOnSD(String fileName) throws IOException {
        File testDirectory = getTestRootDirectory();

        File file = new File(testDirectory, fileName);
        file.createNewFile();
        return file;
    }

    public File getTestRootDirectory() {
        return TestEnvironment.getTestRootDirectory();
    }

    public void dump() {
        Cursor queue = queryQueue();
        DatabaseUtils.dumpCursor(queue);
        queue.close();
    }

    public void assertEmptyQueue() {
        Cursor queue = queryQueue();
        if (queue.getCount() > 0) {
            DatabaseUtils.dumpCursor(queue);
        }
        assertEquals(0, queue.getCount());
        queue.close();
    }

    public void verifyWebdavUploadFile(String fileName, String destination) throws Exception {
        String fileNameOnServer = new Path(fileName).getName();
        verifyWebdavUploadFile(fileName, destination, fileNameOnServer);
    }

    public void verifyWebdavUploadFile(String fileName, String destination, String fileNameOnServer)
            throws Exception {
        verify(webdav).uploadFile(eq(new File(fileName)), eq(destination), eq(fileNameOnServer),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());
    }

    public List<File> captureWebdavUploadFile() throws Exception {
        ArgumentCaptor<File> files = forClass(File.class);
        verify(webdav, atLeastOnce()).uploadFile(files.capture(), anyString(), nullable(String.class),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());
        return files.getAllValues();
    }

    public List<ServerDiskItem> captureStoredDiskItems() {
        ArgumentCaptor<ServerDiskItem> items = forClass(ServerDiskItem.class);

        verify(diskDatabase, atLeastOnce()).updateOrInsertPhotosliceItem(items.capture(), anyLong());

        return items.getAllValues();
    }

    public List<Long> captureStoredPhotosliceTimes() {
        ArgumentCaptor<Long> times = forClass(Long.class);

        verify(diskDatabase, atLeastOnce()).updateOrInsertPhotosliceItem(any(), times.capture());

        return times.getAllValues();
    }

    public void restartUpload() {
        startUpload(new UploadCommandRequest(), false);
    }

    private void startUpload(CommandRequest request, boolean allTasks) {
//        commandStarter.setExecuteCommands(false);
        commandStarter.start(request);
        assertEquals(1, commandStarter.getQueue().size());

//        commandStarter.setExecuteCommands(true);
        commandStarter.executeQueue(allTasks);
//        commandStarter.setExecuteCommands(false);

        // TODO
//        assertEquals(expectRestartUploadTask, getUploadsStartCount());
//        commandStarter.getCompleted().clear();
        context.clearStartServiceRequests();
        expectRestartUploadTask = 0;
    }

//    private int getUploadsStartCount() {
//        int uploadStarts = 0;
//        List<Class> requests = commandStarter.getCompleted();
//        for (Class request : requests) {
//            if (request.equals(UploadCommandRequest.class)) {
//                uploadStarts++;
//            }
//        }
//        return uploadStarts;
//    }

    public CommandRequest makeUploadRequest(String destination, String... src) {
        return new QueueUploadsCommandRequest(asArrayList(src), destination);
    }

    public void doTasks() {
        commandStarter.executeQueue(false);
    }

    public Credentials getActiveAccount() {
        return cm.getActiveAccountCredentials();
    }

    public CredentialsManager getCredentialsManager() {
        return cm;
    }

    public void prepareWebdavThrowException(Exception e) throws Exception {
        doThrow(e).when(webdav).uploadFile(any(File.class), anyString(), nullable(String.class),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());
    }

    public void prepareAnswer(final Answer<String> answer) throws Exception {
        doAnswer(answer).when(webdav).uploadFile(anyFile(), anyString(), nullable(String.class),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());
    }

    public void prepareMetadataAnswers(final List<DiskItem> items) throws Exception {
        OngoingStubbing<List<? extends DiskItem>> stubbing = when(webdav.getFileListWithMinimalMetadata(anyCollection()));

        for (DiskItem item : items) {
            stubbing = stubbing.thenReturn(Collections.singletonList(item));
        }
        stubbing.thenAnswer(invocation -> Lists.transform(
                invocation.<List<String>>getArgument(0),
                path -> new DiskItemBuilder().setPath(path).build()));
    }

    public void moveInTime() {
        assertSame(systemClock, systemClockInDiskUploader.getWrappee());
        systemClock.move(1500);
    }

    public void setConnectivityManagerState(boolean connected) {
        String type = connected ? "WIFI" : null;
        setConnectivityManagerState(type);
        networkState.updateState();
    }

    public void setConnectivityManagerState(String type) {
        ConnectivityManager cm = connectivityManager;
        reset(cm);
        NetworkInfo networkInfo;
        if (type != null) {
            networkInfo = mock(NetworkInfo.class);
            when(networkInfo.getTypeName()).thenReturn(type);
            when(networkInfo.isConnected()).thenReturn(true);
            when(networkInfo.isConnectedOrConnecting()).thenReturn(true);
        } else {
            networkInfo = null;
        }
        NetworkInfo[] networkInfos = networkInfo != null ?
                new NetworkInfo[]{networkInfo} : new NetworkInfo[]{};
        when(cm.getAllNetworkInfo()).thenReturn(networkInfos);
        when(cm.getActiveNetworkInfo()).thenReturn(networkInfo);
    }

    public AutoUploadSettings getSettings() {
        UserSettings userSettings = applicationSettings.getUserSettings(getActiveAccount());
        return userSettings.getAutoUploadSettings();
    }

    public UserSettings getUserSettings() {
        return applicationSettings.getUserSettings(getActiveAccount());
    }

    public String queueVideoToAutoUpload(String fileName) throws IOException {
        return queueToAutoUpload(fileName, Queue.MediaTypeCode.VIDEO);
    }

    public String queueImageToAutoUpload(String fileName) throws Exception {
        return queueToAutoUpload(fileName, Queue.MediaTypeCode.IMAGE);
    }

    public String queueSkippedFileToAutoUpload(String fileName, int mimeType) throws IOException {
        getDiskUploader().markQueueChanged();
        final FileQueueItem item = createQueueItem(fileName, mimeType);

        uploadQueue.addSkippedAutouploadFile(item);

        return item.getSrcName();
    }

    private String queueToAutoUpload(final String fileName, final int mimeType) throws IOException {
        getDiskUploader().markQueueChanged();

        final FileQueueItem item = createQueueItem(fileName, mimeType);

        uploadQueue.addToQueue(item);

        return item.getSrcName();
    }

    private FileQueueItem createQueueItem(final String fileName, final int mediaType) throws IOException {
        File file = makeFileOnSD(fileName);
        file.setLastModified(fileCreationDate);
        return new DiskQueueItem(file.getAbsolutePath(),
            ServerConstants.AUTOUPLOADING_FOLDER, mediaType, fileCreationDate, Queue.UploadItemType.AUTOUPLOAD);
    }

    public void setFileCreationDate(long fileCreationDate) {
        this.fileCreationDate = fileCreationDate;
    }

    public void queueFileToUpload(String src) {
        commandStarter.start(makeUploadRequest("/disk", src));
        commandStarter.executeQueue(false);
    }

    public void expectRestartUploadTask() {
        expectRestartUploadTask = 1;
    }

    public void expectRestartUploadTask(int times) {
        expectRestartUploadTask = times;
    }

    public void verifyMakeFolder(String directoryName) throws RemoteExecutionException {
        verify(getWebdav()).makeFolder(directoryName);
    }

    public void verifyWebdavUploadFileNeverCalled() throws RemoteExecutionException, StopUploadingException {
        verify(webdav, never()).uploadFile(anyFile(), anyString(), nullable(String.class),
                anyString(), anyString(), anyLong(), anyInt(), anyBoolean(), anyBoolean(), nullable(String.class),
                anyUploadListener());
    }

    public void startUploadFiles(String destination, File src) {
        startUploadFiles(destination, src.getAbsolutePath());
    }

    public void queueFileToUpload(String destination, File src) {
        queueFileToUpload(src.getAbsolutePath());
    }

    public void startUploadFiles(File file) {
        startUploadFiles("/disk", file);
    }

    public void queueFileToUpload(File srcDirectory) {
        queueFileToUpload(srcDirectory.getAbsolutePath());
    }

    static UploadListener anyUploadListener() {
        return any(UploadListener.class);
    }

    public void verifyMakeFolderNeverCalled() throws RemoteExecutionException {
        verify(webdav, never()).makeFolder(anyString());
    }

    public void resetMocks() throws Exception {
        reset(webdav, mockMediaProvider);
        initMocks();
    }

    public void startUpload() {
        startUpload(new UploadCommandRequest(), false);
    }

    public void setSystemClock(SystemClock systemClock) {
        this.systemClockInDiskUploader.setWrappee(systemClock);
    }

    public ContentProvider getMockMediaProvider() {
        return mockMediaProvider;
    }

    public static class JavaSEHashCalculator implements HashCalculator {
        @Override
        public Hash calculate(File file) throws IOException {
            InputStream is = new FileInputStream(file);
            try {
                return getHash(is);
            } finally {
                IOHelper.closeSilently(is);
            }
        }

        private Hash getHash(InputStream is) throws IOException {
            try {
                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
                byte[] buf = new byte[8192];
                int count;
                while ((count = is.read(buf)) > 0) {
                    md5Digest.update(buf, 0, count);
                    sha256Digest.update(buf, 0, count);
                }
                return new Hash(com.yandex.disk.rest.util.Hash.toString(md5Digest.digest()),
                        com.yandex.disk.rest.util.Hash.toString(sha256Digest.digest()));
            } catch (NoSuchAlgorithmException e) {
                return Exceptions.crashValue(e);
            }
        }
    }

    public DiskContentProvider getContentProvider() {
        return contentProvider;
    }

    public SelfContentProviderClient getSelfClient() {
        return selfClient;
    }

    public void setAlbumAutouploadEnabled(final boolean enabled) {
        final int state = enabled ? BucketAlbum.AutoUploadMode.ENABLED : BucketAlbum.AutoUploadMode.DISABLED;
        final BucketAlbum album = new BucketAlbum(new BucketAlbumId("mockBucketId"), "MockAlbum", 1, 0,
            state, "", 0, true, true, true);
        when(bucketAlbumsProvider.getAlbumsSync()).thenReturn(Collections.singletonList(album));
        when(bucketAlbumsProvider.getAlbumForItem(any())).thenReturn(album);
    }
}
