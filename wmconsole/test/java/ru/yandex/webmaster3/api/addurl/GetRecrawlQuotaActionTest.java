package ru.yandex.webmaster3.api.addurl;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.addurl.action.GetRecrawlQuotaAction;
import ru.yandex.webmaster3.api.addurl.action.GetRecrawlQuotaRequest;
import ru.yandex.webmaster3.api.addurl.action.GetRecrawlQuotaResponse;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.util.DailyQuotaUtil;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.addurl.AddUrlRequestsService;

import java.net.URL;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class GetRecrawlQuotaActionTest {
    private final static DailyQuotaUtil.QuotaUsage QUOTA_USAGE = new DailyQuotaUtil.QuotaUsage(1, 0, 1);

    private URL TEST_URL;

    @Mock
    private AddUrlRequestsService addUrlRequestsService;

    @InjectMocks
    private GetRecrawlQuotaAction action = new GetRecrawlQuotaAction();

    @Before
    public void setUp() throws Exception {
        TEST_URL = new URL("https://lenta.ru/aaa");
    }

    @Test
    public void testGet() {
        when(addUrlRequestsService.getQuotaUsage(any(WebmasterHostId.class), any(DateTime.class)))
                .thenReturn(QUOTA_USAGE);

        GetRecrawlQuotaRequest request = createRequest();
        GetRecrawlQuotaResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getDailyQuota());
    }

    private GetRecrawlQuotaRequest createRequest() {
        GetRecrawlQuotaRequest request = new GetRecrawlQuotaRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        RecrawlQuotaLocator locator = new RecrawlQuotaLocator(12345, IdUtils.urlToHostId(TEST_URL));
        request.setLocator(locator);

        return request;
    }
}
