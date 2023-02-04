package ru.yandex.webmaster3.api.searchurls;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.api.searchurls.action.SearchUrlEventSamplesAction;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlEventSamplesRequest;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlEventSamplesResponse;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchEventEnum;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchEventSample;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.sitestructure.SearchUrlStatusEnum;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.searchurl.SearchUrlSamplesService;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlEventSample;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlEventType;
import ru.yandex.webmaster3.storage.searchurl.samples.data.UrlStatusInfo;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.Condition;

import static org.mockito.Mockito.when;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class SearchUrlEventSamplesActionTest {
    private URL TEST_URL;
    private WebmasterHostId HOST_ID;

    @Mock
    private SearchUrlSamplesService searchUrlSamplesService;

    @InjectMocks
    private SearchUrlEventSamplesAction action = new SearchUrlEventSamplesAction();

    @Before
    public void setUp() throws Exception {
        TEST_URL = new URL("https://lenta.ru/aaa");
        HOST_ID = IdUtils.urlToHostId(TEST_URL);
    }

    @Test
    public void testGetEmpty() {
        Condition cond = Condition.trueCondition();

        when(searchUrlSamplesService.getSearchUrlEventSamplesCount(HOST_ID, cond,cond))
                .thenReturn(0L);

        SearchUrlEventSamplesRequest request = createRequest();
        SearchUrlEventSamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getCount());

        List<ApiSearchEventSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(0, samples.size());
    }

    @Test
    public void testGetBeyondCount() {
        Condition cond = Condition.trueCondition();
        when(searchUrlSamplesService.getSearchUrlEventSamplesCount(HOST_ID, cond,cond))
                .thenReturn(3L);

        SearchUrlEventSamplesRequest request = createRequest();
        request.setOffset(3);
        SearchUrlEventSamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getCount());

        List<ApiSearchEventSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(0, samples.size());
    }

    @Test
    public void testGet() {
        Condition cond = Condition.trueCondition();

        when(searchUrlSamplesService.getSearchUrlEventSamplesCount(HOST_ID, cond,cond))
                .thenReturn(1L);

        DateTime now = DateTime.now(TimeUtils.EUROPE_MOSCOW_ZONE);
        DateTime nowPlus = now.plusDays(2);
        UrlStatusInfo statusInfo = new UrlStatusInfo(
                SearchUrlStatusEnum.INDEXED_SEARCHABLE, null, null, 200, null,
                null, null, null, null,
                false, false, false, false, false);
        SearchUrlEventSample rawSample = new SearchUrlEventSample(
                TEST_URL.getPath(), TEST_URL.toExternalForm(), "title", now.toLocalDate(), nowPlus.toLocalDateTime(), SearchUrlEventType.NEW, statusInfo);
        when(searchUrlSamplesService.getSearchUrlEventSamples(HOST_ID, cond,cond, 0, 100))
                .thenReturn(Collections.singletonList(rawSample));

        SearchUrlEventSamplesRequest request = createRequest();
        SearchUrlEventSamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getCount());

        List<ApiSearchEventSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(1, samples.size());

        ApiSearchEventSample sample = samples.get(0);
        Assert.assertEquals(ApiSearchEventEnum.APPEARED_IN_SEARCH, sample.getEvent());
        Assert.assertEquals(TEST_URL, sample.getUrl());
        Assert.assertEquals("title", sample.getTitle());
        Assert.assertEquals(now.withTimeAtStartOfDay(), sample.getLastAccess());
        Assert.assertEquals(nowPlus.withTimeAtStartOfDay(), sample.getEventDate());
        Assert.assertNull(sample.getExcludedUrlStatus().orElse(null));
        Assert.assertNull(sample.getBadHttpStatus().orElse(null));
        Assert.assertNull(sample.getTargetUrl().orElse(null));
    }

    private SearchUrlEventSamplesRequest createRequest() {
        SearchUrlEventSamplesRequest request = new SearchUrlEventSamplesRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));
        request.setLimit(100);

        SearchUrlEventSamplesLocator locator = new SearchUrlEventSamplesLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
