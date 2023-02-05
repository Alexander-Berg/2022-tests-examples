package ru.yandex.disk.upload;

import android.content.ContentProvider;
import android.database.SQLException;
import android.os.Environment;
import android.provider.MediaStore;
import org.junit.Test;
import ru.yandex.disk.CredentialsManager;
import ru.yandex.disk.provider.DiskContract.Queue;
import ru.yandex.disk.provider.DiskContract.Queue.State;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.util.FileContentAccessor;
import ru.yandex.util.Path;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.yandex.disk.test.MoreMatchers.*;

public class DiskUploaderQueueTest extends AndroidTestCase2 {
    private DiskUploaderTestHelper helper;
    private ContentProvider cp;
    private Upload.Builder uploadTaskBuilder;
    private MediaProviderMocker.MediaStoreRecord.Builder mediaStoreRecordBuilder;
    private MediaProviderMocker mediaProviderMocker;
    private TestUploadQueue testUploadQueue;
    private FileContentAccessor fileContentAccessor;

    private String sdcard = Environment.getExternalStorageDirectory().getPath();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        helper = new DiskUploaderTestHelper(mContext);
        cp = helper.getContentProvider();

        long defaultDateTaken = 1000000;
        uploadTaskBuilder = new Upload.Builder()
                .mediaTypeCode(Queue.MediaTypeCode.IMAGE)
                .state(State.IN_QUEUE)
                .auto(true)
                .date(defaultDateTaken);

        mediaStoreRecordBuilder = new MediaProviderMocker.MediaStoreRecord.Builder()
                .mimeType("image/jpg")
                .dateTaken(defaultDateTaken);

        testUploadQueue = new TestUploadQueue(helper.getSelfClient(), helper.getCredentialsManager());
        mediaProviderMocker = new MediaProviderMocker(helper);
        fileContentAccessor = new FileContentAccessor();
    }

    @Test
    public void testDoNotQueueWhatAlreadyQueued() throws Exception {
        Path path = new Path(sdcard, "/DCIM/img1.jpg");

        fileContentAccessor.write(path, "DATA");

        Upload uploadTask = uploadTaskBuilder
                .source(path)
                .size(fileContentAccessor.length(path))
                .build();

        testUploadQueue.add(uploadTask);

        MediaProviderMocker.MediaStoreRecord mediaStoreRecord = mediaStoreRecordBuilder
                .path(path)
                .build();

        mediaProviderMocker.whenQueryImagesThenReturn(mediaStoreRecord);

        doQueueing();

        verify(cp, never()).bulkInsert(anyUri(), anyContentValuesArray());
    }

    @Test
    public void testDoNotQueueDirectoriesFromMediaContentProvider() {
        Path dir = new Path(sdcard, "DCIM/DIR");

        fileContentAccessor.mkdirs(dir);

        MediaProviderMocker.MediaStoreRecord mediaStoreRecord = new MediaProviderMocker.MediaStoreRecord.Builder()
                .path(dir)
                .mimeType(null)
                .dateTaken(0)
                .build();

        mediaProviderMocker.whenQueryImagesThenReturn(mediaStoreRecord);

        doQueueing();

        assertNull(testUploadQueue.peek());
    }

    @Test
    public void testDoNotConfuseDates() throws Exception {
        Path path = new Path(sdcard, "DCIM/img1.jpg");
        fileContentAccessor.write(path, "DATA");

        Upload uploadTask = uploadTaskBuilder
                .source(path)
                .size(fileContentAccessor.length(path))
                .build();

        TestUploadQueue testUploadQueue = new TestUploadQueue(helper.getSelfClient(), helper.getCredentialsManager());
        testUploadQueue.add(uploadTask);

        long noExifDate = Long.MIN_VALUE;
        MediaProviderMocker.MediaStoreRecord mediaStoreRecord = mediaStoreRecordBuilder
                .path(path)
                .dateTaken(noExifDate)
                .build();
        mediaProviderMocker.whenQueryImagesThenReturn(mediaStoreRecord);

        doQueueing();

        verify(cp, never()).bulkInsert(anyUri(), anyContentValuesArray());
    }

    @Test
    public void testDoNotQueueFileWithZeroLength() throws Exception {
        Path path = new Path(sdcard, "/DCIM/TESTIMAGE.jpg");

        fileContentAccessor.write(path, "");

        MediaProviderMocker.MediaStoreRecord mediaStoreRecord = new MediaProviderMocker.MediaStoreRecord.Builder()
                .path(path)
                .mimeType(null)
                .dateTaken(0)
                .build();
        mediaProviderMocker.whenQueryImagesThenReturn(mediaStoreRecord);

        doQueueing();

        assertNull(testUploadQueue.peek());
    }

    @Test
    public void testNullMimeTypeFromMediaStore() throws Exception {
        //MOBDISK-2126
        Path path = new Path(sdcard, "/DCIM/TESTIMAGE.jpg");

        MediaProviderMocker.MediaStoreRecord mediaStoreRecord = new MediaProviderMocker.MediaStoreRecord.Builder()
                .path(path)
                .mimeType(null)
                .dateTaken(0)
                .build();
        mediaProviderMocker.whenQueryImagesThenReturn(mediaStoreRecord);

        fileContentAccessor.write(path, "DATA");

        doQueueing();

        assertNotNull(testUploadQueue.peek());
    }

    @Test
    public void testHandleExceptionFromMediaContentProvider() {
        ContentProvider mockMediaProvider = helper.getMockMediaProvider();
        when(mockMediaProvider.query(anyUri(), nullable(String[].class),
                nullable(String.class), nullable(String[].class), nullable(String.class)))
                .thenThrow(new SQLException("test"));

        doQueueing();
    }

    @Test
    public void testHandleNullFromMediaContentProvider() {
        ContentProvider mockMediaProvider = helper.getMockMediaProvider();
        when(mockMediaProvider.query(anyUri(), nullable(String[].class),
                nullable(String.class), nullable(String[].class), nullable(String.class)))
                .thenReturn(null);

        doQueueing();
    }

    @Test
    public void testHandleExceptionFromMediaContentProviderThenQueryDateTaken() {
        Path path = new Path(sdcard, "/DCIM/TESTIMAGE.jpg");

        fileContentAccessor.write(path, "DATA");

        MediaProviderMocker.MediaStoreRecord mediaStoreRecord = mediaStoreRecordBuilder
                .path(path)
                .build();
        mediaProviderMocker.whenQueryImagesThenReturn(mediaStoreRecord);

        String dateTakenSelection = MediaStore.MediaColumns.DATA + " = ?";

        ContentProvider mockMediaProvider = helper.getMockMediaProvider();
        when(mockMediaProvider.query(anyUri(), nullable(String[].class),
                eq(dateTakenSelection), nullable(String[].class), nullable(String.class)))
                .thenThrow(new SQLException("test"));

        doQueueing();
    }

    private void doQueueing() {
        helper.commandStarter.start(new QueueAutouploadsCommandRequest());
        helper.doTasks();
    }
}
