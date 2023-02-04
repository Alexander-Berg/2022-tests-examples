package ru.yandex.webmaster3.api.indexing2;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.indexing2.action.Indexing2SamplesAction;
import ru.yandex.webmaster3.api.indexing2.action.Indexing2SamplesRequest;
import ru.yandex.webmaster3.api.indexing2.action.Indexing2SamplesResponse;
import ru.yandex.webmaster3.api.indexing2.data.ApiIndexedPageSample;
import ru.yandex.webmaster3.api.indexing2.data.IndexingStatusEnum;
import ru.yandex.webmaster3.core.data.HttpCodeInfo;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.searchquery.OrderDirection;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.indexing2.samples.IndexingSamplesService;
import ru.yandex.webmaster3.storage.indexing2.samples.dao.IndexedUrlSamplesOrderField;
import ru.yandex.webmaster3.storage.indexing2.samples.data.IndexedUrlSample;
import ru.yandex.webmaster3.storage.util.clickhouse2.condition.Condition;
import ru.yandex.wmtools.common.util.http.YandexHttpStatus;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class Indexing2SamplesActionTest {
    private URL TEST_URL;
    private WebmasterHostId HOST_ID;

    @Mock
    private IndexingSamplesService indexingSamplesService;

    @InjectMocks
    Indexing2SamplesAction action = new Indexing2SamplesAction();

    @Before
    public void setUp() throws Exception {
        TEST_URL = new URL("https://lenta.ru/aaa");
        HOST_ID = IdUtils.urlToHostId(TEST_URL);
    }

    @Test
    public void testGetEmpty() {
        Condition cond = Condition.trueCondition();
        when(indexingSamplesService.getSamplesCount(HOST_ID, cond))
                .thenReturn(0L);

        Indexing2SamplesRequest request = createRequest();
        Indexing2SamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(0, response.getCount());

        List<ApiIndexedPageSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(0, samples.size());
    }

    @Test
    public void testGetBeyondCount() {
        Condition cond = Condition.trueCondition();
        when(indexingSamplesService.getSamplesCount(HOST_ID, cond))
                .thenReturn(3L);

        Indexing2SamplesRequest request = createRequest();
        request.setOffset(3);
        Indexing2SamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(3, response.getCount());

        List<ApiIndexedPageSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(0, samples.size());
    }

    @Test
    public void testGet() {
        Condition cond = Condition.trueCondition();
        when(indexingSamplesService.getSamplesCount(HOST_ID, cond))
                .thenReturn(1L);

        DateTime lastAccess = DateTime.now(TimeUtils.EUROPE_MOSCOW_ZONE);
        HttpCodeInfo codeInfo = HttpCodeInfo.fromHttpStatus(YandexHttpStatus.parseCode(400));
        IndexedUrlSample rawSample = new IndexedUrlSample(TEST_URL.getPath(), codeInfo, lastAccess);
        when(indexingSamplesService.getSamples(HOST_ID, cond, IndexedUrlSamplesOrderField.LAST_ACCESS, OrderDirection.ASC,0,100))
                .thenReturn(Collections.singletonList(rawSample));

        Indexing2SamplesRequest request = createRequest();
        Indexing2SamplesResponse response = action.process(request);
        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getCount());

        List<ApiIndexedPageSample> samples = response.getSamples();
        Assert.assertNotNull(samples);
        Assert.assertEquals(1, samples.size());

        ApiIndexedPageSample sample = samples.get(0);
        Assert.assertEquals(IndexingStatusEnum.HTTP_4XX, sample.getStatus());
        Assert.assertEquals(lastAccess, sample.getAccessDate());
        Assert.assertTrue(sample.getHttpCode().isPresent());
        Assert.assertEquals(400, (int)sample.getHttpCode().get());
        Assert.assertEquals(TEST_URL, sample.getUrl());
    }

    private Indexing2SamplesRequest createRequest() {
        Indexing2SamplesRequest request = new Indexing2SamplesRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));
        request.setLimit(100);

        Indexing2SamplesLocator locator = new Indexing2SamplesLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
