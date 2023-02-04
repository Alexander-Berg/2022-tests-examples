package ru.yandex.webmaster3.storage.addurl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.webmaster3.core.addurl.OwnerRequest;
import ru.yandex.webmaster3.core.addurl.RecrawlState;
import ru.yandex.webmaster3.core.addurl.UrlForRecrawl;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.domain.PublicSuffixListCacheService;
import ru.yandex.webmaster3.core.host.service.HostCanonizer;
import ru.yandex.webmaster3.core.host.service.HostOwnerService;
import ru.yandex.webmaster3.core.util.DailyQuotaUtil;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.core.util.UrlUtils;


/**
 * Created by ifilippov5 on 20.11.17.
 */
@Slf4j
public class AddUrlRequestsServiceTest {
    private AddUrlRequestsService addUrlRequestsService;
    private HostOwnerService hostOwnerService;
    private final static LocalDate TODAY = LocalDate.parse("2017-11-20");

    @Before
    public void init() {
        AddUrlLimitsService addUrlLimitsService = new AddUrlLimitsServiceMock();

        PublicSuffixListCacheService service = new PublicSuffixListCacheService();
        service.setPublicSuffixListData(new ClassPathResource("/public_suffix_list.dat"));
        service.setWebmasterSuffixList(new ClassPathResource("/webmaster_suffix_list.dat"));

        hostOwnerService = new HostOwnerService(new HostCanonizerWrapperMock(), service);


        addUrlRequestsService = new AddUrlRequestsService(
                addUrlLimitsService,
                null,
                hostOwnerService,
                new AddUrlRequestsYDaoMock(),
                new AddUrlOwnerRequestsYDaoMock()
        );

        addUrlRequestsService.setCanonizeUrlForRobotWrapper(new CanonizeUrlForRobotWrapperMock());
    }

    @Test
    public void getQuotaUsageTest() {
        WebmasterHostId hostId = IdUtils.stringToHostId("http:tam.by:80");
        WebmasterHostId hostId1 = IdUtils.stringToHostId("http:example.tam.by:80");
        WebmasterHostId hostId2 = IdUtils.stringToHostId("https:example.lenta.ru:443");

        DateTime now = TODAY.toDateTime(LocalTime.MIDNIGHT);
        DailyQuotaUtil.QuotaUsage quotaUsage = addUrlRequestsService.getQuotaUsage(hostId, now);
        Assert.assertEquals(158, quotaUsage.getQuotaRemain());
        Assert.assertEquals(2, quotaUsage.getQuotaUsed());
        Assert.assertEquals(160, quotaUsage.getTodayQuota());

        quotaUsage = addUrlRequestsService.getQuotaUsage(hostId1, now);
        Assert.assertEquals(AddUrlLimitsService.DEFAULT_LIMIT - 2, quotaUsage.getQuotaRemain());
        Assert.assertEquals(2, quotaUsage.getQuotaUsed());
        Assert.assertEquals(AddUrlLimitsService.DEFAULT_LIMIT, quotaUsage.getTodayQuota());

        quotaUsage = addUrlRequestsService.getQuotaUsage(hostId2, now);
        Assert.assertEquals(170, quotaUsage.getQuotaRemain());
        Assert.assertEquals(0, quotaUsage.getQuotaUsed());
        Assert.assertEquals(170, quotaUsage.getTodayQuota());
    }

    @Test
    public void findOwnerTest() {
        WebmasterHostId hostId = IdUtils.stringToHostId("http:tam.by:80");
        WebmasterHostId hostId1 = IdUtils.stringToHostId("http:example.tam.by:80");
        WebmasterHostId hostId2 = IdUtils.stringToHostId("https:example.lenta.ru:443");

        Assert.assertEquals("tam.by", hostOwnerService.getLongestOwner(hostId));
        Assert.assertEquals("example.tam.by", hostOwnerService.getLongestOwner(hostId1));
        Assert.assertEquals("lenta.ru", hostOwnerService.getLongestOwner(hostId2));
    }

    @Test
    public void getPendingRequestsForSamovarUrlTest() {
        List<UrlForRecrawl> requests;
        WebmasterHostId hostId = IdUtils.stringToHostId("http:lenta.ru:80");

        requests = addUrlRequestsService.getPendingRequests(hostId, "http://lenta.ru/test");
        Assert.assertTrue(requests.isEmpty());

        requests = addUrlRequestsService.getPendingRequests(hostId, "http://lenta.ru/test/");
        Assert.assertEquals(requests.size(), 1);
        Assert.assertEquals(requests.get(0), AddUrlRequestsYDaoMock.URL_FOR_RECRAWL_2);

        requests = addUrlRequestsService.getPendingRequests(hostId, "http://lenta.ru/%D1%82%D0%B5%D1%81%D1%82%201/");
        Assert.assertEquals(requests.size(), 1);
        Assert.assertEquals(requests.get(0), AddUrlRequestsYDaoMock.URL_FOR_RECRAWL_3);
    }

    private static class AddUrlLimitsServiceMock extends AddUrlLimitsService {
        private static final Map<String, Integer> LIMITS = Map.of(
                "tam.by", 160,
                "lenta.ru", 170
        );

        public AddUrlLimitsServiceMock() {
            super(null);
        }

        @Override
        public int getActualLimit(String owner) {
            int limit = LIMITS.getOrDefault(owner, 0);
            if (limit == 0) {
                limit = AddUrlLimitsService.DEFAULT_LIMIT;
            }

            return limit;
        }
    }

    private static class HostCanonizerWrapperMock extends HostCanonizer.HostCanonizerWrapper {
        private static final Map<String, String> MAP = Map.of(
                "http://tam.by", "tam.by",
                "http://example.tam.by", "example.tam.by",
                "https://example.lenta.ru", "lenta.ru"
        );

        @Override
        public String getHostOwner(String host) {
            return MAP.get(host);
        }

        @Override
        public String getMascotHostOwner(String host) {
            return MAP.get(host);
        }

        @Override
        public String getUrlOwner(String host) {
            return MAP.get(host);
        }

        @Override
        public String getMascotUrlOwner(String host) {
            return MAP.get(host);
        }
    }

    private static class AddUrlOwnerRequestsYDaoMock extends AddUrlOwnerRequestsYDao {
        public AddUrlOwnerRequestsYDaoMock() {
        }

        private static final Map<String, List<LocalDate>> MAP = new HashMap<>();

        static {
            MAP.put("tam.by", Arrays.asList(TODAY, TODAY.minusDays(1), TODAY));
            MAP.put("example.tam.by", Arrays.asList(TODAY, TODAY.minusDays(1), TODAY));
            MAP.put("lenta.ru", new ArrayList<>());
        }

        @Override
        public List<OwnerRequest> list(String owner)  {
            return MAP.get(owner).stream()
                    .map(d -> new OwnerRequest(owner, UUID.randomUUID(), d, d.toDateTimeAtStartOfDay()))
                    .collect(Collectors.toList());
        }
    }


    private static class CanonizeUrlForRobotWrapperMock extends UrlUtils.CanonizeUrlForRobotWrapper {
        private static final Map<String, String> MAP = Map.of(
                "http://lenta.ru/test/", "http://lenta.ru/test/",
                "http://lenta.ru/тест 1/", "http://lenta.ru/%D1%82%D0%B5%D1%81%D1%82%201/"
        );

        @Override
        public String canonizeUrlForRobot(String url) {
            return MAP.get(url);
        }
    }

    private static class AddUrlRequestsYDaoMock extends AddUrlRequestsYDao {
        public AddUrlRequestsYDaoMock() {
            super();
        }

        private final static WebmasterHostId HOST_ID = IdUtils.stringToHostId("http:lenta.ru:80");
        private final static UrlForRecrawl URL_FOR_RECRAWL_1 = new UrlForRecrawl(
                HOST_ID,
                UUID.randomUUID(),
                "/test/",
                TODAY.toDateTimeAtCurrentTime(),
                TODAY.toDateTimeAtCurrentTime(),
                RecrawlState.PROCESSED
        );

        private final static UrlForRecrawl URL_FOR_RECRAWL_2 = new UrlForRecrawl(
                HOST_ID,
                UUID.randomUUID(),
                "/test/",
                TODAY.toDateTimeAtCurrentTime(),
                TODAY.toDateTimeAtCurrentTime(),
                RecrawlState.IN_PROGRESS
        );

        private final static UrlForRecrawl URL_FOR_RECRAWL_3 = new UrlForRecrawl(
                HOST_ID,
                UUID.randomUUID(),
                "/тест 1/",
                TODAY.toDateTimeAtCurrentTime(),
                TODAY.toDateTimeAtCurrentTime(),
                RecrawlState.IN_PROGRESS
        );

        private final Map<WebmasterHostId, List<UrlForRecrawl>> MAP = Map.of(
                HOST_ID, List.of(URL_FOR_RECRAWL_1, URL_FOR_RECRAWL_2, URL_FOR_RECRAWL_3)
        );

        @Override
        public List<UrlForRecrawl> list(WebmasterHostId hostId, DateTime fromDate)  {
            return MAP.getOrDefault(hostId, Collections.emptyList());
        }
    }
}
