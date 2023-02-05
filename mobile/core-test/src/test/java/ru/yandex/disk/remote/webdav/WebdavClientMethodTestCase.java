package ru.yandex.disk.remote.webdav;

import okhttp3.HttpUrl;
import okhttp3.Request;
import org.junit.Test;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.remote.exceptions.ConnectionException;
import ru.yandex.disk.remote.exceptions.ForbiddenException;
import ru.yandex.disk.remote.exceptions.NotAuthorizedException;
import ru.yandex.disk.remote.exceptions.PermanentException;
import ru.yandex.disk.remote.exceptions.ServerUnavailableException;
import ru.yandex.disk.remote.webdav.WebdavClient.WebdavConfig;
import ru.yandex.disk.settings.AutoUploadSettings;
import ru.yandex.disk.test.TestObjectsFactory;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;
import ru.yandex.disk.util.UserAgentProvider;

import javax.annotation.NonnullByDefault;
import java.util.TimeZone;

import static org.mockito.Mockito.mock;

@NonnullByDefault
public abstract class WebdavClientMethodTestCase extends HttpClientUserTestCase {
    private static final WebdavConfig WEBDAV_HOST =
            new WebdavConfig(TestConstants.TEST_HOST_BASE_URL);

    protected WebdavClient client;
    private TimeZone defaultTimeZone;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GTM"));
        final Credentials credentials = TestObjectsFactory.createCredentials();
        fakeOkHttpBuilder.addInterceptor(new PutFileInfoHeadersInterceptor());
        client = new WebdavClient(credentials, WEBDAV_HOST, fakeOkHttpBuilder,
            mock(AutoUploadSettings.class),
            new SeparatedAutouploadToggle(false),
            mock(DeveloperSettings.class));
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testProcess5xx() throws Exception {
        testBadStatusCode(500, ServerUnavailableException.class);
    }

    @Test
    public void testProcess401() throws Exception {
        testBadStatusCode(401, NotAuthorizedException.class);
    }

    @Test
    public void testProcess402() throws Exception {
        testBadStatusCode(402, PermanentException.class);
    }

    @Test
    public void testProcess403() throws Exception {
        testBadStatusCode(403, ForbiddenException.class);
    }

    @Test
    public void testIOExceptionDuringRequest() throws Exception {
        testIOExceptionDuringRequest(ConnectionException.class);
    }

    private static void assertHostMatch(final WebdavConfig config, final HttpUrl uri) {
        assertEquals(config.getProtocol(), uri.scheme());
        assertEquals(config.getPort(), uri.port());
        assertEquals(config.getHost(), uri.host());
    }

    protected static void verifyRequest(final String method, final String path, final String query,
                                        final Request request) {
        assertEquals(method, request.method());
        final HttpUrl uri = request.url();
        assertHostMatch(WEBDAV_HOST, uri);
        assertEquals(path, uri.encodedPath());
        assertEquals(query, uri.encodedQuery());
    }
}
