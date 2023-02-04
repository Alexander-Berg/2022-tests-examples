package ru.yandex.webmaster3.api.addurl;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.addurl.action.AddRecrawlUrlAction;
import ru.yandex.webmaster3.api.addurl.action.AddRecrawlUrlRequest;
import ru.yandex.webmaster3.api.addurl.action.AddRecrawlUrlResponse;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.util.DailyQuotaUtil;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.worker.client.WorkerClient;
import ru.yandex.webmaster3.storage.abt.AbtService;
import ru.yandex.webmaster3.storage.addurl.AddUrlRequestsService;
import ru.yandex.webmaster3.storage.spam.DeepSpamHostFilter;
import ru.yandex.webmaster3.storage.spam.FastSpamHostFilter;

import java.net.URL;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class AddRecrawlUrlActionTest {
    private final static DailyQuotaUtil.QuotaUsage QUOTA_USAGE = new DailyQuotaUtil.QuotaUsage(1, 0, 1);

    private URL TEST_URL;
    private URL TEST_INVALID_URL;

    @Mock
    private AddUrlRequestsService addUrlRequestsService;

    @Mock
    private DeepSpamHostFilter deepSpamHostFilter;

    @Mock
    private FastSpamHostFilter fastSpamHostFilter;

    @Mock
    private WorkerClient workerClient;

    @Mock
    private AbtService abtService;

    @InjectMocks
    private AddRecrawlUrlAction action;

    @Before
    public void setUp() throws Exception {
        TEST_URL = new URL("https://lenta.ru/aaa");
        TEST_INVALID_URL = new URL("https://lenta.ru/a\ta");
    }

    @Test
    public void testAdd() {
        when(addUrlRequestsService.getQuotaUsage(any(WebmasterHostId.class), any(DateTime.class)))
                .thenReturn(QUOTA_USAGE);

        when(addUrlRequestsService.toRelativeUrlWithVerification(any(WebmasterHostId.class), anyString()))
                .thenCallRealMethod();

        AddRecrawlUrlRequest request = createRequest();
        AddRecrawlUrlResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof AddRecrawlUrlResponse.NormalResponse);
        AddRecrawlUrlResponse.NormalResponse normalResponse = (AddRecrawlUrlResponse.NormalResponse)response;

        RecrawlQueueItemLocator locator = normalResponse.getLocation();
        Assert.assertNotNull(locator);

        WebmasterHostId hostId = locator.getHostId();
        Assert.assertEquals(IdUtils.urlToHostId(TEST_URL), hostId);

        UUID taskId = locator.getTaskId();
        Assert.assertNotNull(taskId);

        int quotaRemainder = normalResponse.getQuotaRemainder();
        Assert.assertEquals(QUOTA_USAGE.getQuotaRemain(), quotaRemainder);
    }

    @Test
    public void testInvalidUrl() {
        when(addUrlRequestsService.getQuotaUsage(any(WebmasterHostId.class), any(DateTime.class)))
                .thenReturn(QUOTA_USAGE);

        when(addUrlRequestsService.toRelativeUrlWithVerification(any(WebmasterHostId.class), anyString()))
                .thenCallRealMethod();

        AddRecrawlUrlRequest request = createRequest();
        request.setEntity(new AddRecrawlUrlRequest.Entity(TEST_INVALID_URL));
        AddRecrawlUrlResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof AddRecrawlUrlResponse.InvalidUrl);
    }

    @Test
    public void testAlreadyAdded() {
        when(addUrlRequestsService.getQuotaUsage(any(WebmasterHostId.class), any(DateTime.class)))
                .thenReturn(QUOTA_USAGE);

        when(addUrlRequestsService.toRelativeUrlWithVerification(any(WebmasterHostId.class), anyString()))
                .thenCallRealMethod();

        when(addUrlRequestsService.requestExists(any(WebmasterHostId.class), anyString()))
                .thenReturn(true);

        AddRecrawlUrlRequest request = createRequest();
        AddRecrawlUrlResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof AddRecrawlUrlResponse.UrlAlreadyAdded);
    }

    @Test
    public void testQuotaExceeded() {
        when(addUrlRequestsService.getQuotaUsage(any(WebmasterHostId.class), any(DateTime.class)))
                .thenReturn(new DailyQuotaUtil.QuotaUsage(1, 0, 0));

        AddRecrawlUrlRequest request = createRequest();
        AddRecrawlUrlResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof AddRecrawlUrlResponse.QuotaExceededResponse);
        AddRecrawlUrlResponse.QuotaExceededResponse quotaExceededResponse = (AddRecrawlUrlResponse.QuotaExceededResponse)response;

        Assert.assertEquals(1, quotaExceededResponse.getDailyQuota());
    }

    private AddRecrawlUrlRequest createRequest() {
        AddRecrawlUrlRequest request = new AddRecrawlUrlRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));
        request.setEntity(new AddRecrawlUrlRequest.Entity(TEST_URL));

        RecrawlQueueLocator locator = new RecrawlQueueLocator(12345, IdUtils.urlToHostId(TEST_URL));
        request.setLocator(locator);

        return request;
    }
}
