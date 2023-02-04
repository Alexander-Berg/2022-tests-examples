package ru.yandex.webmaster3.viewer.http.links;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.codes.HttpCodeGroup;
import ru.yandex.webmaster3.core.data.HttpCodeInfo;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.link.LinkHistoryIndicatorType;
import ru.yandex.webmaster3.core.link.LinksHistoryIndicator;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.util.ydb.exception.WebmasterYdbException;
import ru.yandex.webmaster3.storage.links.LinksService;

import java.util.*;

/**
 * @author tsyplyaev
 */
public class LinksHistoryTest {
    static final String hostId = "http:test.ru:80";

    private static final Map<LinkHistoryIndicatorType, NavigableMap<LocalDate, Long>> historyData = new HashMap<>() {
        {
            put(LinkHistoryIndicatorType.EXTERNAL_LINK_GONE_URLS_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 1L);
                put(LocalDate.now(), 2L);
            }});
            put(LinkHistoryIndicatorType.EXTERNAL_LINK_GONE_HOSTS_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 3L);
                put(LocalDate.now(), 4L);
            }});
            put(LinkHistoryIndicatorType.EXTERNAL_LINK_NEW_URLS_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 5L);
                put(LocalDate.now(), 6L);
            }});
            put(LinkHistoryIndicatorType.EXTERNAL_LINK_NEW_HOSTS_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 7L);
                put(LocalDate.now(), 8L);
            }});
            put(LinkHistoryIndicatorType.EXTERNAL_LINK_HOSTS_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 9L);
                put(LocalDate.now(), 10L);
            }});
            put(LinkHistoryIndicatorType.EXTERNAL_LINK_URLS_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 11L);
                put(LocalDate.now(), 12L);
            }});
            put(LinkHistoryIndicatorType.INTERNAL_LINKS_URL_COUNT, new TreeMap<>() {{
                put(LocalDate.now().minusDays(1), 13L);
                put(LocalDate.now(), 14L);
            }});
        }
    };

    @Test
    public void testIndicators()  {
        List<LinksHistoryIndicator> data = Arrays.asList(new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.EXTERNAL_LINK_HOSTS_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.EXTERNAL_LINK_URLS_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.EXTERNAL_LINK_NEW_HOSTS_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.EXTERNAL_LINK_NEW_URLS_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.EXTERNAL_LINK_GONE_HOSTS_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.EXTERNAL_LINK_GONE_URLS_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_URL_COUNT, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES, 0),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES, 200),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES, 301),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES, 404),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES, 500),
                new LinksHistoryIndicator(DateTime.now().toInstant(), LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES, 2013));

        LinksService linksHistoryService = EasyMock.createMock(LinksService.class);
        EasyMock.expect(linksHistoryService.getActualIndicators(IdUtils.stringToHostId(hostId))).andReturn(data);
        EasyMock.replay(linksHistoryService);

        GetLinkIndicatorsTreeAction getLinkIndicatorsTreeAction = new GetLinkIndicatorsTreeAction();
        getLinkIndicatorsTreeAction.setLinksService(linksHistoryService);

        GetLinkIndicatorsTreeRequest request = new GetLinkIndicatorsTreeRequest();
        request.setHostId(hostId);
        request.setConvertedHostId(IdUtils.stringToHostId(hostId));

        GetLinkIndicatorsTreeResponse.NormalResponse response = (GetLinkIndicatorsTreeResponse.NormalResponse)getLinkIndicatorsTreeAction.process(request);
        List<GetLinkIndicatorsTreeResponse.Node> indicators = response.getIndicators();

        //Assert.assertEquals(indicators.size(), 10);
        Assert.assertEquals(indicators.get(0).getName(), LinkIndicator.All.LINKS);
        Assert.assertEquals(indicators.get(1).getName(), LinkIndicator.All.LINKS_EXTERNAL);
        Assert.assertEquals(indicators.get(2).getName(), LinkIndicator.All.LINKS_INTERNAL);
        Assert.assertEquals(indicators.get(3).getName(), LinkIndicator.All.LINKS_INTERNAL_NORMAL);
        Assert.assertEquals(indicators.get(4).getName(), LinkIndicator.All.LINKS_INTERNAL_REDIRECT);
        Assert.assertEquals(indicators.get(5).getName(), LinkIndicator.All.LINKS_INTERNAL_BROKEN);
        Assert.assertEquals(indicators.get(6).getName(), LinkIndicator.All.LINKS_INTERNAL_BROKEN_SITE_ERROR);
        Assert.assertEquals(indicators.get(7).getName(), LinkIndicator.All.LINKS_INTERNAL_BROKEN_DISALLOWED_BY_USER);
        Assert.assertEquals(indicators.get(8).getName(), LinkIndicator.All.LINKS_INTERNAL_BROKEN_UNSUPPORTED_BY_ROBOT);
        Assert.assertEquals(indicators.get(9).getName(), LinkIndicator.All.LINKS_INTERNAL_NOT_DOWNLOADED);
    }

    @Test
    public void testHistory() {
        LinksService linksHistoryService = EasyMock.createMock(LinksService.class);
        EasyMock.expect(linksHistoryService.getSimpleHistory(IdUtils.stringToHostId(hostId))).andReturn(historyData);
        EasyMock.replay(linksHistoryService);

        GetLinksHistoryAction action = new GetLinksHistoryAction();
        action.setLinksService(linksHistoryService);

        GetLinksHistoryRequest request = new GetLinksHistoryRequest();
        request.setHostId(hostId);
        request.setConvertedHostId(IdUtils.stringToHostId(hostId));

        GetLinksHistoryResponse.OrdinaryResponse response = (GetLinksHistoryResponse.OrdinaryResponse)action.process(request);
        List<GetLinksHistoryResponse.LinkHistory> histories = response.getHistories();

        Assert.assertEquals(histories.size(), 2);
        Assert.assertEquals(histories.get(0).getIndicatorName(), GetLinksHistoryResponse.Indicator.LINKS_EXTERNAL);
        Assert.assertEquals(histories.get(0).getData().size(), 2);
        Assert.assertEquals(histories.get(0).getData().get(0).getValue(), 11);
        Assert.assertEquals(histories.get(0).getData().get(1).getValue(), 12);
        Assert.assertEquals(histories.get(1).getIndicatorName(), GetLinksHistoryResponse.Indicator.LINKS_INTERNAL);
        Assert.assertEquals(histories.get(1).getData().size(), 2);
        Assert.assertEquals(histories.get(1).getData().get(0).getValue(), 13);
        Assert.assertEquals(histories.get(1).getData().get(1).getValue(), 14);
    }

    private static final Map<HttpCodeInfo, NavigableMap<LocalDate, Long>> codeHistoryData = new HashMap<>() {
        {
            final LocalDate today = LocalDate.now();
            put(new HttpCodeInfo(0), new TreeMap<>() {{
                put(today.minusDays(1), 1L);
                put(today, 2L);
            }});
            put(new HttpCodeInfo(200), new TreeMap<>() {{
                put(today.minusDays(1), 3L);
                put(today, 4L);
            }});
            put(new HttpCodeInfo(301), new TreeMap<>() {{
                put(today.minusDays(1), 5L);
                put(today, 6L);
            }});
            put(new HttpCodeInfo(404), new TreeMap<>() {{
                put(today.minusDays(1), 7L);
                put(today, 8L);
            }});
            put(new HttpCodeInfo(500), new TreeMap<>() {{
                put(today.minusDays(1), 9L);
                put(today, 10L);
            }});
            put(new HttpCodeInfo(1005), new TreeMap<>() {{
                put(today.minusDays(1), 11L);
                put(today, 12L);
            }});
        }
    };

    private LinksService mock(LinkHistoryIndicatorType expectIndicator) {
        LinksService linksHistoryService = EasyMock.createMockBuilder(LinksService.class)
                .addMockedMethod("getHttpHistory", WebmasterHostId.class, DateTime.class, DateTime.class, boolean.class, LinkHistoryIndicatorType.class)
                .createMock();
        EasyMock.expect(linksHistoryService
                .getHttpHistory(EasyMock.eq(IdUtils.stringToHostId(hostId)), EasyMock.anyObject(DateTime.class),
                        EasyMock.anyObject(DateTime.class), EasyMock.anyBoolean(), EasyMock.eq(expectIndicator))).andReturn(codeHistoryData)
                .anyTimes();
        EasyMock.replay(linksHistoryService);
        return linksHistoryService;
    }

    @Test
    public void testInternalBrokenHistory() {
        LinksService linksHistoryService = mock(LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES);

        GetInternalLinkBrokenHistoryAction action = new GetInternalLinkBrokenHistoryAction();
        action.setLinksService(linksHistoryService);

        GetInternalLinkBrokenHistoryRequest request = new GetInternalLinkBrokenHistoryRequest();
        request.setHostId(hostId);
        request.setConvertedHostId(IdUtils.stringToHostId(hostId));

        GetInternalLinkBrokenHistoryResponse.NormalResponse response = (GetInternalLinkBrokenHistoryResponse.NormalResponse)action.process(request);
        List<GetInternalLinkBrokenHistoryResponse.BrokenGroupHistory> histories = response.getHistories();

        histories.sort((a, b) -> a.getIndicatorName().compareTo(b.getIndicatorName()));

        Assert.assertEquals(histories.size(), 3);
        Assert.assertEquals(histories.get(0).getIndicatorName(), LinkIndicator.Broken.LINKS_INTERNAL_BROKEN_SITE_ERROR);
        Assert.assertEquals(histories.get(0).getData().size(), 2);
        Assert.assertEquals(histories.get(0).getData().get(0).getValue(), 9);
        Assert.assertEquals(histories.get(0).getData().get(1).getValue(), 10);
        Assert.assertEquals(histories.get(1).getIndicatorName(), LinkIndicator.Broken.LINKS_INTERNAL_BROKEN_DISALLOWED_BY_USER);
        Assert.assertEquals(histories.get(1).getData().size(), 2);
        Assert.assertEquals(histories.get(1).getData().get(0).getValue(), 7);
        Assert.assertEquals(histories.get(1).getData().get(1).getValue(), 8);
        Assert.assertEquals(histories.get(2).getIndicatorName(), LinkIndicator.Broken.LINKS_INTERNAL_BROKEN_UNSUPPORTED_BY_ROBOT);
        Assert.assertEquals(histories.get(2).getData().size(), 2);
        Assert.assertEquals(histories.get(2).getData().get(0).getValue(), 11);
        Assert.assertEquals(histories.get(2).getData().get(1).getValue(), 12);
    }

    @Test
    public void testInternalBrokenHttpCodesHistory() {
        LinksService linksHistoryService = mock(LinkHistoryIndicatorType.INTERNAL_LINKS_HTTP_CODES);

        GetInternalLinkBrokenHttpCodesHistoryAction actionSiteError = new GetInternalLinkBrokenHttpCodesHistoryAction();
        actionSiteError.setIndicator(LinkIndicator.Broken.LINKS_INTERNAL_BROKEN_SITE_ERROR);
        actionSiteError.setLinksService(linksHistoryService);
        GetInternalLinkBrokenHttpCodesHistoryAction actionDisallowed = new GetInternalLinkBrokenHttpCodesHistoryAction();
        actionDisallowed.setIndicator(LinkIndicator.Broken.LINKS_INTERNAL_BROKEN_DISALLOWED_BY_USER);
        actionDisallowed.setLinksService(linksHistoryService);
        GetInternalLinkBrokenHttpCodesHistoryAction actionUnsupported = new GetInternalLinkBrokenHttpCodesHistoryAction();
        actionUnsupported.setIndicator(LinkIndicator.Broken.LINKS_INTERNAL_BROKEN_UNSUPPORTED_BY_ROBOT);
        actionUnsupported.setLinksService(linksHistoryService);

        GetInternalLinkBrokenHttpCodesHistoryRequest request = new GetInternalLinkBrokenHttpCodesHistoryRequest();
        request.setHostId(hostId);
        request.setConvertedHostId(IdUtils.stringToHostId(hostId));

        GetInternalLinkBrokenHttpCodesHistoryResponse.NormalResponse response;
        List<GetInternalLinkBrokenHttpCodesHistoryResponse.HttpCodeHistory> histories;

        //site errors
        response = (GetInternalLinkBrokenHttpCodesHistoryResponse.NormalResponse)actionSiteError.process(request);
        histories = response.getHistories();

        Assert.assertEquals(histories.size(), 1);
        Assert.assertEquals(histories.get(0).getIndicatorName().getHttpCodeGroup(), HttpCodeGroup.HTTP_500_INTERNAL_SERVER_ERROR);
        Assert.assertEquals(histories.get(0).getData().size(), 2);
        Assert.assertEquals(histories.get(0).getData().get(0).getValue(), 9);
        Assert.assertEquals(histories.get(0).getData().get(1).getValue(), 10);

        //disallowed by users
        response = (GetInternalLinkBrokenHttpCodesHistoryResponse.NormalResponse)actionDisallowed.process(request);
        histories = response.getHistories();

        Assert.assertEquals(histories.size(), 1);
        Assert.assertEquals(histories.get(0).getIndicatorName().getHttpCodeGroup(), HttpCodeGroup.HTTP_404_NOT_FOUND);
        Assert.assertEquals(histories.get(0).getData().size(), 2);
        Assert.assertEquals(histories.get(0).getData().get(0).getValue(), 7);
        Assert.assertEquals(histories.get(0).getData().get(1).getValue(), 8);

        //unsupported by robots
        response = (GetInternalLinkBrokenHttpCodesHistoryResponse.NormalResponse)actionUnsupported.process(request);
        histories = response.getHistories();

        Assert.assertEquals(histories.size(), 1);
        Assert.assertEquals(histories.get(0).getIndicatorName().getHttpCodeGroup(), HttpCodeGroup.HTTP_GROUP_1005);
        Assert.assertEquals(histories.get(0).getData().size(), 2);
        Assert.assertEquals(histories.get(0).getData().get(0).getValue(), 11);
        Assert.assertEquals(histories.get(0).getData().get(1).getValue(), 12);
    }
}
