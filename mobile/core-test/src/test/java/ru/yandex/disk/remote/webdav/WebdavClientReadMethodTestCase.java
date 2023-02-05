package ru.yandex.disk.remote.webdav;

import org.junit.Test;
import ru.yandex.disk.remote.exceptions.ConnectionException;

import java.io.IOException;

public abstract class WebdavClientReadMethodTestCase extends WebdavClientMethodTestCase {

    @Test
    public void testIOExceptionDuringRead() throws Exception {
        prepareToThrowIOExceptionDuringRead(getGoodCode());

        try {
            invokeMethod();
            fail("IOException during read operation should leads to WebdavIOException");
        } catch (ConnectionException | IOException e) {
        }

        assertThatResponsesClosed();
    }

    protected abstract int getGoodCode();

    private void prepareToThrowIOExceptionDuringRead(final int statusCode) throws Exception {
        fakeOkHttpInterceptor.throwIOExceptionDuringRead(statusCode);
    }
}
