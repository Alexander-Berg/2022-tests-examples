package ru.yandex.disk.remote.webdav;

import okhttp3.Request;
import ru.yandex.disk.DiskItem;
import ru.yandex.disk.DiskItemFactory;

public class UnpublishMethodTest extends WebdavClientMethodTestCase {

    private DiskItem fileA;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        fileA = DiskItemFactory.create("/disk/a");
    }

    @Override
    protected void invokeMethod() throws Exception {
        client.unpublish(fileA);
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        fakeOkHttpInterceptor.addResponse(200);
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("POST", "/disk/a", "unpublish", request);
    }

}
