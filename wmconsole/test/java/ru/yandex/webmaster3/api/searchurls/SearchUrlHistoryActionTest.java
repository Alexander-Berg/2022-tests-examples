package ru.yandex.webmaster3.api.searchurls;

import com.google.common.collect.ImmutableMap;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.webmaster3.api.http.common.data.ApiHistoryPoint;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlHistoryAction;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlHistoryRequest;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlHistoryResponse;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.http.request.RequestId;
import ru.yandex.webmaster3.core.sitestructure.NewSiteStructure;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.searchurl.history.SearchUrlHistoryService;
import ru.yandex.webmaster3.storage.searchurl.history.data.SearchUrlHistoryIndicator;
import ru.yandex.webmaster3.storage.searchurl.history.data.SearchUrlHistoryPoint;

import java.util.*;

import static org.mockito.Mockito.when;

/**
 * @author leonidrom
 */

@RunWith(MockitoJUnitRunner.class)
public class SearchUrlHistoryActionTest {
    private static WebmasterHostId HOST_ID = IdUtils.stringToHostId("https:lenta.ru:443");

    @Mock
    private SearchUrlHistoryService searchUrlHistoryService;

    @InjectMocks
    SearchUrlHistoryAction action = new SearchUrlHistoryAction();

    @Test
    public void testGetEmpty() {
        NavigableMap<Instant, SearchUrlHistoryPoint> rawHistory = new TreeMap<>();

        when(searchUrlHistoryService.getSearchHistory(
                HOST_ID,
                Collections.singleton(NewSiteStructure.ROOT_NODE_ID),
                Collections.singleton(SearchUrlHistoryIndicator.COUNT),
                null, null))
                .thenReturn(rawHistory);

        SearchUrlHistoryRequest request = createRequest();
        SearchUrlHistoryResponse response = action.process(request);
        Assert.assertNotNull(response);

        List<ApiHistoryPoint<Long>> historyPoints = response.getHistory();
        Assert.assertNotNull(historyPoints);
        Assert.assertEquals(0, historyPoints.size());
    }

    @Test
    public void testGet() {
        NavigableMap<Instant, SearchUrlHistoryPoint> rawHistory = new TreeMap<>();

        Instant instant1 = Instant.now();
        Map<Long, Map<SearchUrlHistoryIndicator, Long>> node2Values1 = new HashMap<>();
        node2Values1.put(NewSiteStructure.ROOT_NODE_ID, ImmutableMap.of(SearchUrlHistoryIndicator.COUNT, 1L));
        rawHistory.put(instant1, new SearchUrlHistoryPoint(node2Values1));

        Instant instant2 = instant1.plus(Duration.standardMinutes(1));
        Map<Long, Map<SearchUrlHistoryIndicator, Long>> node2Values2 = new HashMap<>();
        node2Values2.put(NewSiteStructure.ROOT_NODE_ID, ImmutableMap.of(SearchUrlHistoryIndicator.COUNT, 5L));
        rawHistory.put(instant2, new SearchUrlHistoryPoint(node2Values2));

        when(searchUrlHistoryService.getSearchHistory(
                HOST_ID,
                Collections.singleton(NewSiteStructure.ROOT_NODE_ID),
                Collections.singleton(SearchUrlHistoryIndicator.COUNT),
                null, null))
                .thenReturn(rawHistory);

        SearchUrlHistoryRequest request = createRequest();
        SearchUrlHistoryResponse response = action.process(request);
        Assert.assertNotNull(response);

        List<ApiHistoryPoint<Long>> historyPoints = response.getHistory();
        Assert.assertNotNull(historyPoints);
        Assert.assertEquals(2, historyPoints.size());

        ApiHistoryPoint point1 = historyPoints.get(0);
        Assert.assertEquals(instant1.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), point1.getDate());
        Assert.assertEquals(1L, point1.getValue());

        ApiHistoryPoint point2 = historyPoints.get(1);
        Assert.assertEquals(instant2.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), point2.getDate());
        Assert.assertEquals(5L, point2.getValue());
    }

    private SearchUrlHistoryRequest createRequest() {
        SearchUrlHistoryRequest request = new SearchUrlHistoryRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        SearchUrlHistoryLocator locator = new SearchUrlHistoryLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
