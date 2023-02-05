package ru.yandex.disk.remote.webdav;

import okhttp3.Headers;
import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.remote.exceptions.ConnectionException;
import ru.yandex.disk.remote.exceptions.RemoteExecutionException;
import ru.yandex.disk.util.IOHelper;
import ru.yandex.disk.util.Signal;

import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;

public class OpenStreamMethodTest extends WebdavClientReadMethodTestCase {

    private String data;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        data = "DATADATADATA";
    }

    @Override
    protected void invokeMethod() throws Exception {
        try (final InputStream in = client.openStream("/path", "query", null, true)) {
            in.read();
        }
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("GET", "/path", "query", request);
    }

    @Test
    public void testOpenStream() throws Exception {
        prepareToReturnContentFromString(200, data);

        final String actual = IOHelper.readInputStream(client.openStream("/path", "query", null, true));
        assertEquals(data, actual);
    }

    @Override
    protected int getGoodCode() {
        return 200;
    }

    private void callAndCatchWebdavIOException(final Signal signal) {
        try {
            client.openStream("/path", "query", signal, true);
            fail();
        } catch (final ConnectionException e) {
            //ok
        } catch (final RemoteExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCancelIfSignalBecomeBeforeExecute() throws Exception {
        final Signal signal = new Signal();
        signal.signal();
        callAndCatchWebdavIOException(signal);
    }

    @Test
    public void shouldAuthorizeAfterRedirect() throws Exception {
        fakeOkHttpInterceptor.addResponseWithHeaders(302,
                new Headers.Builder()
                        .add("Etag", "ETAG")
                        .add("Location", "https://downloader/a")
                        .build());
        fakeOkHttpInterceptor.addResponse(200, "DATA");

        invokeMethod();

        final Request firstGetRequest = fakeOkHttpInterceptor.getRequest(0);
        verifyGetRequest(TestConstants.TEST_HOST_BASE_URL + "/path?query", firstGetRequest);
        final Request secondGetRequestAfterRedirect = fakeOkHttpInterceptor.getRequest(1);
        verifyGetRequest("https://downloader/a", secondGetRequestAfterRedirect);
        assertThatResponsesClosed();

    }

    private void verifyGetRequest(final String url, final Request request) {
        assertEquals("GET", request.method());
        assertThat(request.url().toString(), equalTo(url));
        assertHasHeader("Yandex-Cloud-Mobile-Activity", "user", request);
    }


}
