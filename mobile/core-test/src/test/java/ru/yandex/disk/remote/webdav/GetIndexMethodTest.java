package ru.yandex.disk.remote.webdav;

import com.google.common.io.ByteStreams;
import okhttp3.Headers;
import okhttp3.Request;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import ru.yandex.disk.client.IndexNotExistsException;
import ru.yandex.disk.client.IndexParsingHandler;

import java.io.InputStream;
import java.util.Collections;

import static org.mockito.Mockito.*;

public class GetIndexMethodTest extends WebdavClientReadMethodTestCase {

    private IndexParsingHandler mockIndexParsingHandler;

    @Test(expected = IndexNotExistsException.class)
    public void testGettingIndexShouldThrowIfOfflineDirectoryNotExists() throws Exception {
        prepareToReturnCode(404);
        invokeMethod();
    }

    @Override
    protected void invokeMethod() throws Exception {
        mockIndexParsingHandler = mock(IndexParsingHandler.class);
        client.getIndex("/disk/A", "OLD", Collections.singletonList(CustomHeaders.ACTIVITY_LIST_BACKGROUND),
                mockIndexParsingHandler,
                RuntimeEnvironment.application.getCacheDir().getAbsolutePath());
    }

    @Override
    protected void prepareGoodResponse() throws Exception {
        final InputStream body = getClass().getResourceAsStream("/com/yandex/disk/client/mediaExt.bin");
        final byte[] buffer = new byte[body.available()];
        ByteStreams.readFully(body, buffer);
        fakeOkHttpInterceptor.addResponse(200,
                buffer,
                Headers.of("Etag", "NEW"));
    }

    @Override
    protected void checkHttpRequest(final Request request) throws Exception {
        verifyRequest("GET", "/disk/A", "index&v=1&ext=media", request);
        assertHasHeader("If-Match", "OLD", request);
        verify(mockIndexParsingHandler, times(19)).handleItem(any());
        verify(mockIndexParsingHandler).setNextEtag("/disk/A", "NEW");
    }

    @Override
    protected int getGoodCode() {
        return 200;
    }
}
