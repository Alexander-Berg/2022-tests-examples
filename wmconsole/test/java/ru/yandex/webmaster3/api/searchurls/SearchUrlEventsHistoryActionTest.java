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
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlEventsHistoryAction;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlEventsHistoryRequest;
import ru.yandex.webmaster3.api.searchurls.action.SearchUrlEventsHistoryResponse;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchEventEnum;
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
public class SearchUrlEventsHistoryActionTest {
    private static WebmasterHostId HOST_ID = IdUtils.stringToHostId("https:lenta.ru:443");

    @Mock
    private SearchUrlHistoryService searchUrlHistoryService;

    @InjectMocks
    SearchUrlEventsHistoryAction action = new SearchUrlEventsHistoryAction();

    @Test
    public void testGetEmpty() {
        NavigableMap<Instant, SearchUrlHistoryPoint> rawHistory = new TreeMap<>();

        when(searchUrlHistoryService.getSearchHistory(
                HOST_ID,
                Collections.singleton(NewSiteStructure.ROOT_NODE_ID),
                EnumSet.of(SearchUrlHistoryIndicator.GONE, SearchUrlHistoryIndicator.NEW),
                null, null))
                .thenReturn(rawHistory);

        SearchUrlEventsHistoryRequest request = createRequest();
        SearchUrlEventsHistoryResponse response = action.process(request);
        Assert.assertNotNull(response);

        Map<ApiSearchEventEnum, List<ApiHistoryPoint<Long>>> indicatorsMap = response.getIndicators();
        Assert.assertNotNull(indicatorsMap);
        Assert.assertEquals(0, indicatorsMap.size());
    }

    @Test
    public void testGet() {
        NavigableMap<Instant, SearchUrlHistoryPoint> rawHistory = new TreeMap<>();

        Instant instant1 = Instant.now();
        Map<Long, Map<SearchUrlHistoryIndicator, Long>> node2Values1 = new HashMap<>();
        node2Values1.put(NewSiteStructure.ROOT_NODE_ID, ImmutableMap.of(SearchUrlHistoryIndicator.NEW, 2L));
        rawHistory.put(instant1, new SearchUrlHistoryPoint(node2Values1));

        Instant instant2 = instant1.plus(Duration.standardMinutes(1));
        Map<Long, Map<SearchUrlHistoryIndicator, Long>> node2Values2 = new HashMap<>();
        node2Values2.put(NewSiteStructure.ROOT_NODE_ID, ImmutableMap.of(
                SearchUrlHistoryIndicator.GONE, 5L, SearchUrlHistoryIndicator.NEW, 7L));
        rawHistory.put(instant2, new SearchUrlHistoryPoint(node2Values2));

        when(searchUrlHistoryService.getSearchHistory(
                HOST_ID,
                Collections.singleton(NewSiteStructure.ROOT_NODE_ID),
                EnumSet.of(SearchUrlHistoryIndicator.GONE, SearchUrlHistoryIndicator.NEW),
                null, null))
                .thenReturn(rawHistory);

        SearchUrlEventsHistoryRequest request = createRequest();
        SearchUrlEventsHistoryResponse response = action.process(request);
        Assert.assertNotNull(response);

        Map<ApiSearchEventEnum, List<ApiHistoryPoint<Long>>> indicatorsMap = response.getIndicators();
        Assert.assertNotNull(indicatorsMap);

        List<ApiHistoryPoint<Long>> removedFromSearchHistory = indicatorsMap.get(ApiSearchEventEnum.REMOVED_FROM_SEARCH);
        Assert.assertNotNull(removedFromSearchHistory);
        Assert.assertEquals(1, removedFromSearchHistory.size());
        ApiHistoryPoint historyPoint = removedFromSearchHistory.get(0);
        Assert.assertEquals(instant2.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), historyPoint.getDate());
        Assert.assertEquals(5L, historyPoint.getValue());

        List<ApiHistoryPoint<Long>> appearedInSearchHistory = indicatorsMap.get(ApiSearchEventEnum.APPEARED_IN_SEARCH);
        Assert.assertNotNull(appearedInSearchHistory);
        Assert.assertEquals(2, appearedInSearchHistory.size());
        ApiHistoryPoint historyPoint1 = appearedInSearchHistory.get(0);
        Assert.assertEquals(instant1.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), historyPoint1.getDate());
        Assert.assertEquals(2L, historyPoint1.getValue());
        ApiHistoryPoint historyPoint2 = appearedInSearchHistory.get(1);
        Assert.assertEquals(instant2.toDateTime(TimeUtils.EUROPE_MOSCOW_ZONE), historyPoint2.getDate());
        Assert.assertEquals(7L, historyPoint2.getValue());
    }

    private SearchUrlEventsHistoryRequest createRequest() {
        SearchUrlEventsHistoryRequest request = new SearchUrlEventsHistoryRequest();
        request.setAuthorizedUserId(12345);
        request.setBalancerRequestId(new RequestId.BalancerRequestId("fuuuuu"));

        SearchUrlEventsHistoryLocator locator = new SearchUrlEventsHistoryLocator(12345, HOST_ID);
        request.setLocator(locator);

        return request;
    }
}
