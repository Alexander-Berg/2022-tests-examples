package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.junit.Test;

import static org.hamcrest.Matchers.*;

public class ResolveUrlMethodTest extends WebdavClientMethodTestCase {
    @Override
    protected void invokeMethod() throws Exception {
        client.resolveUrl("https://videostreamer/orgin");
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        fakeOkHttpInterceptor.addResponseTemporarilyRedirect("https://videostreamer/redirect");
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        assertThat(request.url().toString(), equalTo("https://videostreamer/orgin"));
        assertHasHeader("Yandex-Cloud-Mobile-Activity", "user", request);
        assertThat(request.header("Authorization"), is(nullValue()));
    }

    @Test
    public void shouldExtractLocationHeaderIfRedirect() throws Exception {
        prepareGoodResponse();

        final String url = client.resolveUrl("https://videostreamer/orgin");
        assertThat(url, equalTo("https://videostreamer/redirect"));
        assertThatResponsesClosed();
    }

    @Test
    public void shouldReturnOriginIfRedirect() throws Exception {
        fakeOkHttpInterceptor.addResponse(200);
        final String url = client.resolveUrl("https://videostreamer/orgin");
        assertThat(url, equalTo("https://videostreamer/orgin"));
        assertThatResponsesClosed();
    }

}
