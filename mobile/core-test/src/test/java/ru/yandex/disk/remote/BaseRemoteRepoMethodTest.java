package ru.yandex.disk.remote;

import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import ru.yandex.disk.AppStartSessionProvider;
import ru.yandex.disk.Credentials;
import ru.yandex.disk.DeveloperSettings;
import ru.yandex.disk.commonactions.SingleWebdavClientPool;
import ru.yandex.disk.remote.webdav.WebdavClient;
import ru.yandex.disk.test.AndroidTestCase2;
import ru.yandex.disk.toggle.SeparatedAutouploadToggle;

import static org.mockito.Mockito.mock;

public abstract class BaseRemoteRepoMethodTest extends AndroidTestCase2 {
    protected FakeOkHttpInterceptor fakeOkHttpInterceptor;
    protected RemoteRepo remoteRepo;
    protected WebdavClient mockWebdavClient;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        fakeOkHttpInterceptor = new FakeOkHttpInterceptor();
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(false);

        final DisableHttpInterceptor interceptor = new DisableHttpInterceptor(fakeOkHttpInterceptor);
        builder.addInterceptor(interceptor);

        final RestApiClient restApiClient =
                new RestApiClient(builder, "http://test");
        mockWebdavClient = mock(WebdavClient.class);
        remoteRepo =
                new RemoteRepo(mock(Credentials.class), new SingleWebdavClientPool(mockWebdavClient), restApiClient,
                    mock(DeveloperSettings.class), new SeparatedAutouploadToggle(false), mock(AppStartSessionProvider.class));

        interceptor.prepare(builder.interceptors());
    }

    @After
    public void tearDown() throws Exception {
        fakeOkHttpInterceptor.checkBodyClosed();
    }

}
