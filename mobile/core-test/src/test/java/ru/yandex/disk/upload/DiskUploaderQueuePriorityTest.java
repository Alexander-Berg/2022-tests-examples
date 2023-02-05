package ru.yandex.disk.upload;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.remote.webdav.WebdavClient.UploadListener;

public class DiskUploaderQueuePriorityTest extends AndroidTestCase2 {

    private DiskUploaderTestHelper helper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        helper = new DiskUploaderTestHelper(mContext);
    }

    @Test
    public void testCancelVideoAutoUploadIfImageAdded() throws Exception {
        helper.queueVideoToAutoUpload("VIDEO.avi");
        helper.prepareAnswer(new UploadTaskCancelCheckerTemplate() {

            @Override
            protected void changeQueue() throws Exception {
                helper.queueImageToAutoUpload("IMAGE.jpg");

            }

            @Override
            protected boolean expectUploadingCanceled() {
                return true;
            }
        });
        helper.expectRestartUploadTask();
        helper.startUpload();
    }

    @Test
    public void testAutouploadCanceledAfterAddingUserUpload() throws Exception {
        helper.queueVideoToAutoUpload("VIDEO.avi");
        helper.prepareAnswer(new UploadTaskCancelCheckerTemplate() {

            @Override
            protected void changeQueue() throws Exception {
                helper.queueFileToUpload("IMAGE.jpg");
            }

            @Override
            protected boolean expectUploadingCanceled() {
                return true;
            }
        });
        helper.expectRestartUploadTask();
        helper.startUpload();
    }

    @Test
    public void testStartedAutouploadCanceledAfterAddingUserUpload() throws Exception {
        helper.queueVideoToAutoUpload("VIDEO.avi");
        helper.prepareAnswer(new UploadTaskCancelCheckerTemplate() {

            @Override
            protected void uploadProgress(UploadListener listener) {
                listener.uploadProgress(10, 1000);
            }

            @Override
            protected void changeQueue() throws Exception {
                helper.queueFileToUpload("IMAGE.jpg");

            }

            @Override
            protected boolean expectUploadingCanceled() {
                return true;
            }
        });
        helper.expectRestartUploadTask();
        helper.startUpload();
    }

    @Test
    public void testAutouploadNotCanceledIfFirstItemInQueueUnchanged() throws Exception {
        helper.queueImageToAutoUpload("IMAGE.jpg");
        helper.prepareAnswer(new UploadTaskCancelCheckerTemplate() {

            @Override
            protected void changeQueue() throws Exception {
                helper.queueVideoToAutoUpload("VIDEO.avi");
            }

            @Override
            protected boolean expectUploadingCanceled() {
                return false;
            }

        });
        helper.expectRestartUploadTask();
        helper.startUpload();
    }

    private abstract class UploadTaskCancelCheckerTemplate implements Answer<String> {

        @Override
        public String answer(InvocationOnMock invocation) throws Throwable {
            UploadListener listener = (UploadListener) invocation.getArguments()[DiskUploaderTestHelper.UPLOAD_LISTENER_INDEX];

            assertFalse(listener.isUploadingCanceled());

            helper.moveInTime();
            uploadProgress(listener);

            changeQueue();

            helper.moveInTime();
            assertEquals(expectUploadingCanceled(), listener.isUploadingCanceled());
            return "/disk/testLocation";
        }

        protected void uploadProgress(UploadListener listener) {
        }

        protected abstract void changeQueue() throws Exception;

        protected abstract boolean expectUploadingCanceled();

    }

}