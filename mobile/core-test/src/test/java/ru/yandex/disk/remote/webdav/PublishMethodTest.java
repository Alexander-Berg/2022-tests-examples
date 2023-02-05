package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.DiskItemFactory;
import ru.yandex.disk.remote.exceptions.PublishForbiddenException;

import static org.hamcrest.Matchers.equalTo;

public class PublishMethodTest extends WebdavClientMethodTestCase {

    private DiskItem fileA;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        fileA = DiskItemFactory.create("/disk/a");
    }

    @Override
    protected void invokeMethod() throws Exception {
        client.publish(fileA);
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        fakeOkHttpInterceptor.addResponseTemporarilyRedirect("https://ydi.sk/a");
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("POST", "/disk/a", "publish", request);
    }

    @Test
    public void shouldParseResponse() throws Exception {
        prepareGoodResponse();

        final String link = client.publish(fileA).link;

        assertThat(link, equalTo("https://ydi.sk/a"));

        assertThatResponsesClosed();
    }

    @Test
    @Override
    public void testProcess403() throws Exception {
        testBadStatusCode(403, PublishForbiddenException.class);
    }

}