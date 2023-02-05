package ru.yandex.disk.remote.webdav;

import com.yandex.disk.client.DownloadListener;
import com.yandex.disk.client.exceptions.ServerWebdavException;
import com.yandex.disk.client.exceptions.UnknownServerWebdavException;
import com.yandex.disk.client.exceptions.WebdavNotAuthorizedException;
import com.yandex.disk.client.exceptions.WebdavUserNotInitialized;
import okhttp3.Headers;
import okhttp3.Request;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;

public class DownloadFileTest extends WebdavClientMethodTestCase {

    private TestDownloadListener downloadListener;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        downloadListener = new TestDownloadListener();
    }

    @Override
    protected void invokeMethod() throws Exception {
        client.downloadFile("/disk/a", Collections.singletonList(CustomHeaders.ACTIVITY_LIST_USER), downloadListener);
    }

    @Test
    public void testProcess5xx() throws Exception {
        testBadStatusCode(500, ServerWebdavException.class);
    }

    @Test
    public void testProcess401() throws Exception {
        testBadStatusCode(401, WebdavNotAuthorizedException.class);
    }

    @Test
    public void testProcess402() throws Exception {
        testBadStatusCode(402, UnknownServerWebdavException.class);
    }

    @Test
    public void testProcess403() throws Exception {
        testBadStatusCode(403, WebdavUserNotInitialized.class);
    }

    @Test
    public void testIOExceptionDuringRequest() throws Exception {
        testIOExceptionDuringRequest(IOException.class);
    }

    @Test
    public void testShouldCallSetContentTypeInDownloadListener() throws Exception {
        fakeOkHttpInterceptor.addResponse(200, "image/jpg", "DATA");

        final TestDownloadListener downloadListener = new TestDownloadListener();
        client.downloadFile("/disk/a", Collections.singletonList(CustomHeaders.ACTIVITY_LIST_USER), downloadListener);

        assertEquals("image/jpg", downloadListener.contentType);
    }

    @Test
    public void testShouldGetEtagFromRedirectResponse() throws Exception {
        prepareGoodResponses();

        client.downloadFile("/disk/a", Collections.singletonList(CustomHeaders.ACTIVITY_LIST_USER), downloadListener);

        assertEquals("ETAG", downloadListener.etag);
    }

    @Override
    protected void checkHttpRequests() throws Exception {
        final Request firstGetRequest = fakeOkHttpInterceptor.getRequest(0);
        verifyGetRequest(TestConstants.TEST_HOST_BASE_URL + "/disk/a", firstGetRequest);
        final Request secondGetRequestAfterRedirect = fakeOkHttpInterceptor.getRequest(1);
        verifyGetRequest("https://downloader/a", secondGetRequestAfterRedirect);
        assertThatResponsesClosed();
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        //see checkHttpRequests()
    }

    private void verifyGetRequest(final String url, final Request request) {
        assertEquals("GET", request.method());
        assertThat(request.url().toString(), equalTo(url));
        assertHasHeader("Range", "bytes=0-", request);
        assertHasHeader("Yandex-Cloud-Mobile-Activity", "user", request);
    }

    @Test(expected = ServerWebdavException.class)
    public void shouldFollowUpOnlyFiveRedirects() throws Exception {
        final Headers.Builder builder = new Headers.Builder();
        fakeOkHttpInterceptor.addResponseWithHeaders(302,
                builder
                        .add("Etag", "ETAG")
                        .add("Location", "https://downloader/a")
                        .build());
        builder.removeAll("Etag"); // only first one has Etag

        fakeOkHttpInterceptor.addResponseWithHeaders(302, builder.set("Location", "https://downloader1/a").build());
        fakeOkHttpInterceptor.addResponseWithHeaders(302, builder.set("Location", "https://downloader2/a").build());
        fakeOkHttpInterceptor.addResponseWithHeaders(302, builder.set("Location", "https://downloader3/a").build());
        fakeOkHttpInterceptor.addResponseWithHeaders(302, builder.set("Location", "https://downloader4/a").build());
        fakeOkHttpInterceptor.addResponseWithHeaders(302, builder.set("Location", "https://downloader5/a").build());

        invokeMethod();

    }

    @Override
    protected void prepareGoodResponses() throws Exception {
        fakeOkHttpInterceptor.addResponseWithHeaders(302,
                new Headers.Builder()
                        .add("Etag", "ETAG")
                        .add("Location", "https://downloader/a")
                        .build());
        fakeOkHttpInterceptor.addResponse(200, "DATA");
    }

    private static final class TestDownloadListener extends DownloadListener {
        String etag;
        String contentType;

        @Override
        public OutputStream getOutputStream(final boolean append) throws IOException {
            return new ByteArrayOutputStream();
        }

        @Override
        public void setEtag(final String etag) {
            this.etag = etag;
        }

        @Override
        public void setContentType(final String contentType) {
            this.contentType = contentType;
        }
    }
}
