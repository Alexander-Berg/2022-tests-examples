package ru.yandex.webmaster3.api.indexing2;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.http.common.data.ApiHistoryPoint;
import ru.yandex.webmaster3.api.indexing2.action.Indexing2HistoryAction;
import ru.yandex.webmaster3.api.indexing2.action.Indexing2HistoryRequest;
import ru.yandex.webmaster3.api.indexing2.action.Indexing2HistoryResponse;
import ru.yandex.webmaster3.api.indexing2.data.IndexingStatusEnum;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.sitestructure.NewSiteStructure;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.indexing2.history.IndexedUrlsCountHistoryService;
import ru.yandex.webmaster3.storage.indexing2.history.data.IndexingHistoryData;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.mockito.Mockito.when;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class Indexing2HistoryActionTest {
    private static WebmasterHostId HOST_ID = IdUtils.stringToHostId("https:lenta.ru:443");

    @Mock
    private IndexedUrlsCountHistoryService indexedUrlsCountHistoryService;

    @InjectMocks
    Indexing2HistoryAction action = new Indexing2HistoryAction();

    @Test
    public void testGetEmpty() {
        NavigableMap<Integer, IndexingHistoryData> rawHistory = new TreeMap<>();
        when(indexedUrlsCountHistoryService.getIndexedUrlsCountHistory(HOST_ID, NewSiteStructure.ROOT_NODE_ID, null, null))
                .thenReturn(rawHistory);

        Indexing2HistoryRequest request = createRequest();
        Indexing2HistoryResponse response = action.process(request);
        Assert.assertNotNull(response);

        Map<IndexingStatusEnum, List<ApiHistoryPoint<Long>>> indicators = response.getIndicators();
        Assert.assertNotNull(indicators);
        Assert.assertEquals(0, indicators.size());
    }

    @Test
    public void testGet() {
        NavigableMap<Integer, IndexingHistoryData> rawHistory = new TreeMap<>();

        Instant instant1 = Instant.now();
        NavigableMap<Instant, Long> timestamp2Value1 = new TreeMap<>();
        timestamp2Value1.put(instant1, 1L);
        IndexingHistoryData historyData1 = new IndexingHistoryData(timestamp2Value1);
        rawHistory.put(400, historyData1);

        Instant instant2 = instant1.plus(Duration.standardMinutes(1));
        NavigableMap<Instant, Long> timestamp2Value2 = new TreeMap<>();
        timestamp2Value2.put(instant1, 1L);
        timestamp2Value2.put(instant2, 1L);
        IndexingHistoryData historyData2 = new IndexingHistoryData(timestamp2Value2);
        rawHistory.put(404, historyData2);

        when(indexedUrlsCountHistoryService.getIndexedUrlsCountHistory(HOST_ID, NewSiteStructure.ROOT_NODE_ID, null, null))
                .thenReturn(rawHistory);

        Indexing2HistoryRequest request = createRequest();
        Indexing2HistoryResponse response = action.process(request);
        Assert.assertNotNull(response);

        Map<IndexingStatusEnum, List<ApiHistoryPoint<Long>>> indicators = response.getIndicators();
        Assert.assertNotNull(indicators);

        IndexingStatusEnum statuses[] = indicators.keySet().toArray(new IndexingStatusEnum[0]);
        Assert.assertEquals(1, statuses.length);
        Assert.assertEquals(IndexingStatusEnum.HTTP_4XX, statuses[0]);

        List<ApiHistoryPoint<Long>> historyPoints = indicators.get(statuses[0]);
        Assert.assertEquals(2, historyPoints.size());

        ApiHistoryPoint historyPoint1 = historyPoints.get(0);
        Assert.assertEquals(2L, historyPoint1.getValue());
        Assert.assertEquals(instant1.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), historyPoint1.getDate());

        ApiHistoryPoint historyPoint2 = historyPoints.get(1);
        Assert.assertEquals(1L, historyPoint2.getValue());
        Assert.assertEquals(instant2.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), historyPoint2.getDate());
    }

    private Indexing2HistoryRequest createRequest() {
        Indexing2HistoryRequest request = new Indexing2HistoryRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        Indexing2HistoryLocator locator = new Indexing2HistoryLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
