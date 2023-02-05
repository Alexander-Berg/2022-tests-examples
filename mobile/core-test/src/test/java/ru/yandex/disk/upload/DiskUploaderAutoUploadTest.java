package ru.yandex.disk.upload;

import com.google.common.collect.Lists;

import org.junit.Test;
import org.robolectric.annotation.Config;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.ServerDiskItem;
import ru.yandex.disk.provider.BucketAlbumsProvider;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.provider.DiskItemBuilder;
import ru.yandex.disk.provider.DiskUploadQueueCursor;
import ru.yandex.disk.remote.ServerConstants;
import ru.yandex.disk.remote.exceptions.FileTooBigServerException;
import ru.yandex.disk.remote.exceptions.FilesLimitExceededException;
import ru.yandex.disk.remote.exceptions.NotAuthorizedException;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.webdav.WebdavClient.UploadListener;
import ru.yandex.disk.settings.AutoUploadSettings;
import ru.yandex.disk.stats.AnalyticsAgent;
import ru.yandex.disk.stats.EventLogSettings;
import ru.yandex.disk.stats.TechEventKeys;
import ru.yandex.disk.stats.EventLog;
import ru.yandex.disk.test.AndroidTestCase2;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class DiskUploaderAutoUploadTest extends AndroidTestCase2 {
    private static final int UPLOAD_LISTENER_INDEX = DiskUploaderTestHelper.UPLOAD_LISTENER_INDEX;
    private DiskUploaderTestHelper helper;
    private String fileNameOnServer;
    private long fileCreationDate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        helper = new DiskUploaderTestHelper(mContext);

        fileNameOnServer = "2013-05-25 00-00-00";
        fileCreationDate = toLongDate(fileNameOnServer);
        helper.setFileCreationDate(fileCreationDate);
    }

    @Test
    public void testAutoUpload() throws Exception {
        String fileName = helper.queueImageToAutoUpload("photo");

        helper.startUpload();

        helper.verifyWebdavUploadFile(
                fileName, ServerConstants.AUTOUPLOADING_FOLDER, fileNameOnServer);

        helper.queryQueue();
    }

    @Test
    public void testAutoUploadTwoFiles() throws Exception {
        String fileName1 = helper.queueImageToAutoUpload("IMG_001.jpg");
        String fileName2 = helper.queueImageToAutoUpload("IMG_002.jpg");

        helper.prepareAnswer(invocation -> invocation.<File>getArgument(0).getAbsolutePath());

        helper.prepareMetadataAnswers(Arrays.asList(
                new DiskItemBuilder().setPath(fileName1).setEtime(111).build(),
                new DiskItemBuilder().setPath(fileName2).build()));

        helper.startUpload();

        List<File> uploadedFiles = helper.captureWebdavUploadFile();
        assertEquals(fileName1, uploadedFiles.get(0).getAbsolutePath());
        assertEquals(fileName2, uploadedFiles.get(1).getAbsolutePath());

        List<ServerDiskItem> storedItems = helper.captureStoredDiskItems();

        assertEquals(Arrays.asList(fileName1, fileName2), Lists.transform(storedItems, ServerDiskItem::getPath));
        assertEquals(Arrays.asList(111L, 0L), Lists.transform(storedItems, ServerDiskItem::getEtime));
        assertEquals(Arrays.asList(111L, fileCreationDate), helper.captureStoredPhotosliceTimes());

        helper.queryQueue();
    }

    @Test
    public void testPauseAutouploadIfWebdavException() throws Exception {
        testPauseAutouploadIfWebdavException(new RemoteExecutionException("test"));
    }

    @Test
    public void testPauseAutouploadIfFilesLimitExceededServerException() throws Exception {
        testPauseAutouploadIfWebdavException(new FilesLimitExceededException("test"));
    }

    @Test
    public void testPauseAlItemsIfFilesLimitExceededServerException() throws Exception {
        helper.queueImageToAutoUpload("photo0");
        helper.queueImageToAutoUpload("photo1");
        helper.prepareWebdavThrowException(new FilesLimitExceededException("test"));

        helper.startUpload();

        DiskUploadQueueCursor queue = helper.queryQueue();
        FileQueueItem item0 = queue.get(0);
        assertEquals(DiskContract.Queue.State.PAUSED, item0.getTransferState());
        FileQueueItem item1 = queue.get(1);
        assertEquals(DiskContract.Queue.State.PAUSED, item1.getTransferState());

        queue.close();
    }

    @Test
    public void testPauseAutouploadIfFileTooBigServerException() throws Exception {
        testPauseAutouploadIfWebdavException(new FileTooBigServerException("test"));
    }

    private void testPauseAutouploadIfWebdavException(Exception e) throws Exception {
        helper.queueImageToAutoUpload("photo");
        helper.prepareWebdavThrowException(e);

        helper.startUpload();

        DiskUploadQueueCursor queue = helper.queryQueue();
        FileQueueItem item = queue.get(0);
        assertEquals(1, queue.getCount());
        assertEquals(DiskContract.Queue.State.PAUSED, item.getTransferState());
        queue.close();
    }

    @Test
    public void testCancelUploadOnNetworkDisconnected() throws Exception {
        helper.queueImageToAutoUpload("IMAGE.jpg");
        helper.prepareAnswer(invocation -> {
            helper.setConnectivityManagerState(false);
            UploadListener listener = (UploadListener) invocation.getArguments()[UPLOAD_LISTENER_INDEX];
            assertFalse(listener.isUploadingCanceled());
            helper.moveInTime();
            assertTrue(listener.isUploadingCanceled());
            return null;
        });
        helper.startUpload();
    }

    @Test
    public void testCancelUploadIfNoWifiButRequired() throws Exception {
        helper.queueImageToAutoUpload("IMAGE.jpg");
        helper.getSettings().setUploadPhotoWhen(AutoUploadSettings.UploadWhen.WIFI);
        helper.prepareAnswer(invocation -> {
            helper.setConnectivityManagerState("3G");
            UploadListener listener = (UploadListener) invocation.getArguments()[UPLOAD_LISTENER_INDEX];
            assertFalse(listener.isUploadingCanceled());
            helper.moveInTime();
            assertTrue(listener.isUploadingCanceled());
            throw new StopUploadingException(null);
        });
        helper.startUpload();
    }

    @Test
    public void testCancelUploadIfAutoUploadTurnedOff() throws Exception {
        helper.queueImageToAutoUpload("IMAGE.jpg");
        helper.getSettings().setUploadPhotoWhen(AutoUploadSettings.UploadWhen.NEVER);
        helper.prepareAnswer(invocation -> {
            helper.setConnectivityManagerState(true);
            UploadListener listener = (UploadListener) invocation.getArguments()[UPLOAD_LISTENER_INDEX];
            assertFalse(listener.isUploadingCanceled());
            helper.moveInTime();
            assertTrue(listener.isUploadingCanceled());
            throw new StopUploadingException(null);
        });
        helper.startUpload();
    }

    @Test
    public void shouldLogoutOn401() throws Exception {
        helper.queueImageToAutoUpload("photo1");
        helper.queueImageToAutoUpload("photo2");

        helper.prepareWebdavThrowException(new NotAuthorizedException("test"));

        helper.startUpload();

        assertThat(helper.getActiveAccount(), is(nullValue()));
    }

    @Test
    public void shouldNotUploadIfDirDisabled() throws Exception {
        helper.queueImageToAutoUpload("photo1");
        helper.setAlbumAutouploadEnabled(false);

        helper.startUpload();

        helper.verifyWebdavUploadFileNeverCalled();
    }

    @Test
    public void shouldSendDisabledDirFileRemovedAnalytics() throws Exception {
        final AnalyticsAgent analyticsAgent = mock(AnalyticsAgent.class);
        EventLog.init(true, mock(EventLogSettings.class), analyticsAgent);

        final String path = helper.queueImageToAutoUpload("photo1");
        helper.setAlbumAutouploadEnabled(false);
        final BucketAlbumsProvider albumsProvider = helper.getBucketAlbumsProvider();
        when(albumsProvider.getAlbumUploadStateChangeTime(any())).thenReturn(100L);
        helper.getUploadQueue().updateOrInsertQueuedDate(path, 1000L,"wifi", true);

        helper.startUpload();

        verify(analyticsAgent).reportTechEvent(eq(TechEventKeys.ITEM_QUEUED_AFTER_DIR_DISABLE_FOUNDED));
    }

    private long toLongDate(String fileNameOnServer) throws ParseException {
        return new SimpleDateFormat("yyy-MM-dd hh-mm-ss").parse(fileNameOnServer).getTime();
    }

}
