package ru.yandex.webmaster3.api.addurl;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.addurl.action.GetRecrawlRequestInfoAction;
import ru.yandex.webmaster3.api.addurl.action.GetRecrawlRequestInfoRequest;
import ru.yandex.webmaster3.api.addurl.action.GetRecrawlRequestInfoResponse;
import ru.yandex.webmaster3.api.addurl.data.RecrawlStatusEnum;
import ru.yandex.webmaster3.core.addurl.RecrawlState;
import ru.yandex.webmaster3.core.addurl.UrlForRecrawl;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.addurl.AddUrlRequestsService;

import java.net.URL;
import java.util.UUID;

import static org.mockito.Mockito.*;


/**
 * @author leonidrom
 */
@RunWith(MockitoJUnitRunner.class)
public class GetRecrawlRequestInfoActionTest {
    private final static UUID TASK_ID = UUID.fromString("c7fe80c0-36e3-11e6-8b2d-df96aa592c0a");

    private URL TEST_URL;

    @Mock
    private AddUrlRequestsService addUrlRequestsService;

    @InjectMocks
    private GetRecrawlRequestInfoAction action = new GetRecrawlRequestInfoAction();

    @Before
    public void setUp() throws Exception {
        TEST_URL = new URL("https://lenta.ru/aaa");
    }

    @Test
    public void testGet() {
        when(addUrlRequestsService.toRelativeUrlWithVerification(any(WebmasterHostId.class), anyString()))
                .thenCallRealMethod();

        WebmasterHostId hostId = IdUtils.urlToHostId(TEST_URL);
        String relativeUrl = addUrlRequestsService.toRelativeUrlWithVerification(hostId, TEST_URL.toExternalForm());
        DateTime now = DateTime.now();

        when(addUrlRequestsService.get(hostId, TASK_ID))
                .thenReturn(new UrlForRecrawl(hostId, TASK_ID, relativeUrl, now, now, RecrawlState.PROCESSED));

        GetRecrawlRequestInfoRequest request = createRequest();
        GetRecrawlRequestInfoResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof GetRecrawlRequestInfoResponse.NormalResponse);

        GetRecrawlRequestInfoResponse.NormalResponse normalResponse = (GetRecrawlRequestInfoResponse.NormalResponse)response;
        Assert.assertEquals(TASK_ID, normalResponse.getTaskId());
        Assert.assertEquals(TEST_URL, normalResponse.getUrl());
        Assert.assertEquals(RecrawlStatusEnum.DONE, normalResponse.getState());
    }

    @Test
    public void testGetNotFound() {
        WebmasterHostId hostId = IdUtils.urlToHostId(TEST_URL);
        when(addUrlRequestsService.get(hostId, TASK_ID))
                .thenReturn(null);

        GetRecrawlRequestInfoRequest request = createRequest();
        GetRecrawlRequestInfoResponse response = action.process(request);

        Assert.assertNotNull(response);
        Assert.assertTrue(response instanceof GetRecrawlRequestInfoResponse.TaskNotFoundResponse);
    }

    private GetRecrawlRequestInfoRequest createRequest() {
        GetRecrawlRequestInfoRequest request = new GetRecrawlRequestInfoRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        RecrawlQueueItemLocator locator = new RecrawlQueueItemLocator(12345, IdUtils.urlToHostId(TEST_URL), TASK_ID);
        request.setLocator(locator);

        return request;
    }
}
