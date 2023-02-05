package ru.yandex.disk.remote.webdav;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okio.Buffer;
import org.apache.http.HttpException;
import org.apache.http.protocol.HttpRequestExecutor;
import org.junit.Test;
import org.robolectric.shadows.httpclient.FakeHttp;
import ru.yandex.disk.remote.DisableHttpInterceptor;
import ru.yandex.disk.remote.FakeOkHttpInterceptor;
import ru.yandex.disk.test.TestCase2;
import ru.yandex.disk.util.IOHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import static org.hamcrest.Matchers.*;

public abstract class HttpClientUserTestCase extends TestCase2 {
    protected HttpRequestExecutor http;

    FakeOkHttpInterceptor fakeOkHttpInterceptor;
    OkHttpClient.Builder fakeOkHttpBuilder;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        FakeHttp.getFakeHttpLayer().interceptHttpRequests(false);
        fakeOkHttpInterceptor = new FakeOkHttpInterceptor();
        fakeOkHttpBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false)
                .addInterceptor(new DisableHttpInterceptor(fakeOkHttpInterceptor));

    }

    protected abstract void invokeMethod() throws Exception;

    @Test
    public void testHttpRequest() throws Exception {
        prepareGoodResponses();

        invokeMethod();

        checkHttpRequests();

        assertThatResponsesClosed();
    }

    protected void checkHttpRequests() throws Exception {
        final Request request = fakeOkHttpInterceptor.getRequest();
        assertThat(request, is(notNullValue()));
        checkHttpRequest(request);
    }

    protected abstract void checkHttpRequest(Request request) throws Exception;

    protected void testBadStatusCode(final int statusCode, final Class<? extends Exception> exceptionClass) throws Exception {
        if (exceptionClass == null) {
            throw new IllegalArgumentException("exceptionClass may not be null");
        }
        prepareToReturnCode(statusCode);

        try {
            invokeMethod();
            fail("status code " + statusCode + " does not lead to exception");
        } catch (final Exception e) {
            assertExceptionClass(exceptionClass, e);
        }
        assertThatResponsesClosed();
    }

    protected void assertThatResponsesClosed() {
        fakeOkHttpInterceptor.checkBodyClosed();
    }

    protected void testIOExceptionDuringRequest(final Class<? extends Exception> exceptionClass) throws IOException, HttpException {
        fakeOkHttpInterceptor.throwIOException();
        try {
            invokeMethod();
            fail("WebdavIOException expected, but not thrown");
        } catch (final Exception e) {
            assertExceptionClass(exceptionClass, e);
        }
    }

    protected void prepareGoodResponses() throws Exception {
        prepareGoodResponse();
    }

    protected void prepareGoodResponse() throws Exception {
        prepareToReturnResponse(200);
    }

    protected void prepareToReturnCode(final int statusCode) throws Exception {
        fakeOkHttpInterceptor.addResponse(statusCode);
    }

    protected void prepareToReturnResponse(final int statusCode) throws Exception {
        fakeOkHttpInterceptor.addResponse(statusCode);
    }

    protected static void assertHasHeader(final String headerName, final Object headerValue,
                                          final Request request) {
        assertEquals("lost header '" + headerName + "'", 1, request.headers(headerName).size());
        final String expected = headerValue.toString();
        final String actual = request.headers().get(headerName);
        assertEqualsHeaders(actual, expected);
    }

    private static void assertEqualsHeaders(final String actual, final String expected) {
        assertEquals(expected, decode(actual));
    }

    private static String decode(final String actual) {
        try {
            return URLDecoder.decode(actual, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected void prepareToReturnContentFromFile(final int statusCode, final String fileName) throws Exception {
        final String body = IOHelper.getResourceAsString("ru/yandex/disk/remote/webdav/" + fileName);
        prepareToReturnContent(statusCode, body);
    }

    protected void prepareToReturnContent(final int statusCode, final String contentAsString) throws Exception {
        fakeOkHttpInterceptor.addResponse(statusCode, contentAsString);
    }

    protected void prepareToReturnContentFromString(final int statusCode, final String data) throws Exception {
        fakeOkHttpInterceptor.addResponse(statusCode, data);
    }

    protected static String extractEntity(final Request request) throws IOException {
        final Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

    private static void assertExceptionClass(final Class<? extends Exception> eqpectedExceptionClass, final Exception e) {
        assertThat(e, instanceOf(eqpectedExceptionClass));
    }

}