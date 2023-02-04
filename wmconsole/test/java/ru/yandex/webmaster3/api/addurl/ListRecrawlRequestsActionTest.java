package ru.yandex.webmaster3.api.addurl;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.addurl.action.ListRecrawlRequestsAction;
import ru.yandex.webmaster3.api.addurl.action.ListRecrawlRequestsRequest;
import ru.yandex.webmaster3.api.addurl.action.ListRecrawlRequestsResponse;
import ru.yandex.webmaster3.api.addurl.data.RecrawlRequestInfo;
import ru.yandex.webmaster3.api.addurl.data.RecrawlStatusEnum;
import ru.yandex.webmaster3.core.addurl.RecrawlState;
import ru.yandex.webmaster3.core.addurl.UrlForRecrawl;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.addurl.AddUrlRequestsService;

import java.net.URL;
import java.util.*;

import static org.mockito.Mockito.*;


/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class ListRecrawlRequestsActionTest {
    private final static UUID TASK_ID_1 = UUID.fromString("c7fe80c0-36e3-11e6-8b2d-df96aa592c0a");
    private final static UUID TASK_ID_2 = UUID.fromString("c7fe80c0-36e3-11e6-8b2d-df96aa592c0b");

    private URL TEST_URL_1;
    private URL TEST_URL_2;

    @Mock
    private AddUrlRequestsService addUrlRequestsService;

    @InjectMocks
    ListRecrawlRequestsAction action = new ListRecrawlRequestsAction();

    @Before
    public void setUp() throws Exception {
        TEST_URL_1 = new URL("https://lenta.ru/aaa");
        TEST_URL_2 = new URL("https://lenta.ru/bbb");
    }

    @Test
    public void testList() {
        DateTime now = DateTime.now();

        when(addUrlRequestsService.toRelativeUrlWithVerification(any(WebmasterHostId.class), anyString()))
                .thenCallRealMethod();

        WebmasterHostId hostId = IdUtils.urlToHostId(TEST_URL_1);
        String relativeUrl = addUrlRequestsService.toRelativeUrlWithVerification(hostId, TEST_URL_1.toExternalForm());
        UrlForRecrawl url1 = new UrlForRecrawl(hostId, TASK_ID_1, relativeUrl, now, now, RecrawlState.PROCESSED);

        hostId = IdUtils.urlToHostId(TEST_URL_2);
        relativeUrl = addUrlRequestsService.toRelativeUrlWithVerification(hostId, TEST_URL_2.toExternalForm());
        UrlForRecrawl url2 = new UrlForRecrawl(hostId, TASK_ID_2, relativeUrl, now, now, RecrawlState.STALE);

        when(addUrlRequestsService.count(any(WebmasterHostId.class), isNull(), isNull()))
                .thenReturn(2);

        when(addUrlRequestsService.list(any(WebmasterHostId.class), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(Arrays.asList(url1, url2));

        ListRecrawlRequestsRequest request = createRequest();
        ListRecrawlRequestsResponse response = action.process(request);
        Assert.assertNotNull(response);

        List<RecrawlRequestInfo> tasks = response.getTasks();
        Assert.assertNotNull(tasks);
        Assert.assertEquals(2, tasks.size());
        Assert.assertEquals(2, response.getCount());

        tasks.sort(Comparator.comparing(RecrawlRequestInfo::getTaskId));
        Assert.assertEquals(TASK_ID_1, tasks.get(0).getTaskId());
        Assert.assertEquals(TASK_ID_2, tasks.get(1).getTaskId());
        Assert.assertEquals(RecrawlStatusEnum.DONE, tasks.get(0).getState());
        Assert.assertEquals(RecrawlStatusEnum.FAILED, tasks.get(1).getState());
    }

    private ListRecrawlRequestsRequest createRequest() {
        ListRecrawlRequestsRequest request = new ListRecrawlRequestsRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        RecrawlQueueLocator locator = new RecrawlQueueLocator(12345, IdUtils.urlToHostId(TEST_URL_1));
        request.setLocator(locator);

        return request;
    }
}
