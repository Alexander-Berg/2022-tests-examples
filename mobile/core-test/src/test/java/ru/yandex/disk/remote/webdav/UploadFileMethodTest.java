package ru.yandex.disk.remote.webdav;

import android.os.Environment;
import okhttp3.Request;
import org.apache.http.HttpException;
import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.disk.provider.DiskContract;
import ru.yandex.disk.remote.ServerConstants;
import ru.yandex.disk.remote.exceptions.FileTooBigServerException;
import ru.yandex.disk.remote.exceptions.FilesLimitExceededException;
import ru.yandex.disk.remote.exceptions.IntermediateFolderNotExistException;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.remote.exceptions.ServerUnavailableException;
import ru.yandex.disk.remote.webdav.WebdavClient.UploadListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;

public class UploadFileMethodTest extends WebdavClientMethodTestCase {

    private UploadListener uploadListener;
    private File file;
    private File testDirectory;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        prepareUploadingFile();
        uploadListener = mock(UploadListener.class);
    }

    private void prepareUploadingFile() throws IOException {
        testDirectory = new File(Environment.getExternalStorageDirectory(), "disk-unit-tests");
        testDirectory.mkdirs();
        file = new File(testDirectory, "file-to-upload");
        try (final FileOutputStream out = new FileOutputStream(file)) {
            out.write("DATA".getBytes());
        }
    }

    @Override
    protected void invokeMethod() throws Exception {
        invokeMethod(false);
    }

    protected void invokeMethod(final boolean reupload) throws Exception {
        client.uploadFile(file, "/directory", null, "MD5HASH", "SHA256HASH", 0, DiskContract.Queue.MediaTypeCode.NONE, false, reupload, null, uploadListener);
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        //see #checkHttpRequests()
    }

    @Override
    protected void prepareGoodResponses() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(201);
    }

    @Override
    protected void checkHttpRequests() throws Exception {
        verifyHeadRequest(fakeOkHttpInterceptor.getRequest(0));
        verifyPutRequest(fakeOkHttpInterceptor.getRequest(1), "/directory/file-to-upload");
    }

    private void verifyHeadRequest(final Request head) throws Exception {
        verifyRequest("HEAD", "/directory/file-to-upload", null, head);
        assertHasHeader("Etag", "MD5HASH", head);
        assertHasHeader("Sha256", "SHA256HASH", head);
        assertHasHeader("Size", "4", head);
    }

    private void verifyPutRequest(final Request put, final String path) {
        verifyRequest("PUT", path, null, put);
        assertHasHeader("Client-Capabilities", "base_location=/,put_redirect", put);
        assertHasHeader("Etag", "MD5HASH", put);
        assertHasHeader("Sha256", "SHA256HASH", put);
        assertHasHeader("Yandex-Cloud-Mobile-Activity", "user", put);
    }

    @Test(expected = RemoteExecutionException.class)
    public void testProcess4xxOnSecondRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(401);

        try {
            invokeMethod();
        } finally {
            assertThatResponsesClosed();
        }

    }

    @Test(expected = ServerUnavailableException.class)
    public void testProcess5xxOnSecondRequest() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(503);

        try {
            invokeMethod();
        } finally {
            assertThatResponsesClosed();
        }
    }

    @Test
    public void test412OnHead() throws Exception {
        testHeadReturnZeroLength(412);
    }

    @Test
    public void test404OnHead() throws Exception {
        testHeadReturnZeroLength(404);
    }

    private void testHeadReturnZeroLength(final int statusCode) throws Exception {
        fakeOkHttpInterceptor.addResponse(statusCode);
        fakeOkHttpInterceptor.addResponse(201);

        invokeMethod();

        final Request putRequest = capturePutRequest();
        assertThat(putRequest.header("Content-Range"), is(nullValue()));

        assertThatResponsesClosed();
    }

    @Test(expected = IntermediateFolderNotExistException.class)
    public void test409OnPut() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(409);

        invokeMethod();
    }

    @Test(expected = FilesLimitExceededException.class)
    public void test507OnPut() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(507);

        invokeMethod();
    }

    @Test(expected = FileTooBigServerException.class)
    public void test413OnPut() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(413);

        invokeMethod();
    }

    @Test(expected = PermanentException.class)
    public void test412OnPut() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(412);

        invokeMethod();
    }

    @Test(expected = PermanentException.class)
    public void test405OnPut() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(405);

        invokeMethod();
    }

    @Test
    public void testAddContentDispositionOnce() throws Exception {
        prepareGoodResponses();

        uploadPhotoToPhotostream();

        final Request headRequest = fakeOkHttpInterceptor.getRequest(0);
        assertThat(headRequest.header("Content-Disposition"), is(nullValue()));

        final Request putRequest = fakeOkHttpInterceptor.getRequest(1);
        assertThat(putRequest.header("Content-Disposition"), is(notNullValue()));
    }

    @Test
    public void testContentDispositionTypeForPhotoFile() throws Exception {
        prepareGoodResponses();

        uploadPhotoToPhotostream();

        final Request putRequest = capturePutRequest();

        final String header = putRequest.header("Content-Disposition");
        assertThat(header, Matchers.startsWith("attachment;"));
    }

    @Test
    public void testContentDispositionTypeForScreenshot() throws Exception {
        prepareGoodResponses();

        uploadScreenshotToScreenshot();

        final Request putRequest = capturePutRequest();

        final String header = putRequest.header("Content-Disposition");
        assertThat(header, startsWith("screenshot;"));
    }

    @Test
    public void testContentDispositionModificationDate() throws Exception {
        prepareGoodResponses();

        uploadPhotoToPhotostream("2013-12-27 10:03:00");

        final Request putRequest = capturePutRequest();

        final String header = putRequest.header("Content-Disposition");
        assertThat(header,
                startsWith("attachment; modification-date=\"Fri, 27 Dec 2013 10:03:00 GMT\""));
    }

    @Test
    public void shouldRepeatTheSamePutRequestOnRedirect() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponseTemporarilyRedirect(TestConstants.TEST_HOST_BASE_URL + "/some/new/location");
        fakeOkHttpInterceptor.addResponse(201);

        invokeMethod();

        final Request secondPut = fakeOkHttpInterceptor.getRequest(2);

        verifyPutRequest(secondPut, "/some/new/location");

        assertThatResponsesClosed();
    }

    @Test
    public void shouldAddRepeatedAutouploadheaderForReuploads() throws Exception {
        fakeOkHttpInterceptor.addResponse(404);
        fakeOkHttpInterceptor.addResponse(201);

        invokeMethod(true);

        final String autouploadTypeHeader = fakeOkHttpInterceptor.getRequest(1)
                .headers().get("X-Yandex-Autoupload-Type");

        assertThat(autouploadTypeHeader, equalTo("repeated"));
    }

    private void uploadPhotoToPhotostream(final String date) throws Exception {
        uploadPhotoToPhotostream(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(date).getTime());
    }

    private void uploadScreenshotToScreenshot() throws Exception {
        uploadFileToPhotostream(new File(testDirectory, "X-Screenshot-X.png"), 0);
    }

    private Request capturePutRequest() throws IOException, HttpException {
        return fakeOkHttpInterceptor.getRequest(1);
    }

    private void uploadPhotoToPhotostream() throws Exception {
        uploadFileToPhotostream(new File(testDirectory, "1.png"), 0);
    }

    private void uploadPhotoToPhotostream(final long modificationDate) throws Exception {
        uploadFileToPhotostream(new File(testDirectory, "1.png"), modificationDate);
    }

    private void uploadFileToPhotostream(final File file, final long modificationDate) throws Exception {
        file.getParentFile().mkdirs();
        file.createNewFile();
        file.setLastModified(modificationDate);
        final String autouploadingFolder = ServerConstants.AUTOUPLOADING_FOLDER;
        client.uploadFile(file, autouploadingFolder, null, "HASH", "HASH", 0, DiskContract.Queue.MediaTypeCode.NONE, false, false, null, uploadListener);
    }

}
