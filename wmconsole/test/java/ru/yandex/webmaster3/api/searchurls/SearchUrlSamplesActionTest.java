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

import ru.yandex.webmaster3.api.searchurls.action.SearchUrlSamplesAction;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlSamplesRequest;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlSamplesResponse;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchUrlSample;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.searchurl.SearchUrlSamplesService;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlSample;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.Condition;

import static org.mockito.Mockito.when;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class SearchUrlSamplesActionTest {
    private URL TEST_URL;
    private WebmasterHostId HOST_ID;

    @Mock
    private SearchUrlSamplesService searchUrlSamplesService;

    @InjectMocks
    SearchUrlSamplesAction action = new SearchUrlSamplesAction();

    @Before
    public void setUp() throws Exception {
        TEST_URL = new URL("https://lenta.ru/aaa");
        HOST_ID = IdUtils.urlToHostId(TEST_URL);
    }

    @Test
    public void testGetEmpty() {
        Condition cond = Condition.trueCondition();

        when(searchUrlSamplesService.getSearchUrlSamplesCount(HOST_ID, cond, cond))
                .thenReturn(0L);

        SearchUrlSamplesRequest request = createRequest();
        SearchUrlSamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getCount());

        List<ApiSearchUrlSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(0, samples.size());
    }

    @Test
    public void testGetBeyondCount() {
        Condition cond = Condition.trueCondition();
        when(searchUrlSamplesService.getSearchUrlSamplesCount(HOST_ID, cond, cond))
                .thenReturn(3L);

        SearchUrlSamplesRequest request = createRequest();
        request.setOffset(3);
        SearchUrlSamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getCount());

        List<ApiSearchUrlSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(0, samples.size());
    }

    @Test
    public void testGet() {
        Condition cond = Condition.trueCondition();

        when(searchUrlSamplesService.getSearchUrlSamplesCount(HOST_ID, cond, cond))
                .thenReturn(1L);

        DateTime now = DateTime.now(TimeUtils.EUROPE_MOSCOW_ZONE);
        SearchUrlSample rawSample = new SearchUrlSample(TEST_URL.getPath(), TEST_URL.toExternalForm(), "title", now, null);
        when(searchUrlSamplesService.getSearchUrlSamples(HOST_ID, cond, cond, 0, 100))
                .thenReturn(Collections.singletonList(rawSample));

        SearchUrlSamplesRequest request = createRequest();
        SearchUrlSamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getCount());

        List<ApiSearchUrlSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(1, samples.size());

        ApiSearchUrlSample sample = samples.get(0);
        Assert.assertEquals(sample.getUrl(), TEST_URL);
        Assert.assertEquals(sample.getTitle(), "title");
        Assert.assertEquals(sample.getLastAccess(), now);
    }

    private SearchUrlSamplesRequest createRequest() {
        SearchUrlSamplesRequest request = new SearchUrlSamplesRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));
        request.setLimit(100);

        SearchUrlSamplesLocator locator = new SearchUrlSamplesLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
