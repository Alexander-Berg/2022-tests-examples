package ru.yandex.webmaster3.storage.crawl;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.storage.crawl.dao.BaseCrawlInfoYDao;
import ru.yandex.webmaster3.storage.crawl.service.CrawlSettingsService;
import ru.yandex.webmaster3.storage.host.CommonDataState;
import ru.yandex.webmaster3.storage.host.CommonDataType;
import ru.yandex.webmaster3.storage.settings.SettingsService;

/**
 * Created by ifilippov5 on 23.01.18.
 */
public class CrawlSettingsServiceTest {
    private final CrawlSettingsService crawlSettingsService = new CrawlSettingsService(null, null, new BaseCrawlInfoYDaoMock(), new SettingsServiceMock());


    @Test
    public void getBaseCrawlInfoTest() {
        Map<WebmasterHostId, Integer> result = ImmutableMap.<WebmasterHostId, Integer>builder()
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "lenta.ru", 443), 6)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "www.yandex.ru", 443), 2)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "a.yandex.ru", 443), 4)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "b.a.yandex.ru", 443), 5)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "d.b.a.yandex.ru", 443), 5)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "yandex.ru", 443), 2)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "tam.by", 443), 3)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "a.b.c", 443), 3)

                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "www.lenta.ru", 80), 13)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "www.yandex.ru", 80), 10)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "a.yandex.ru", 80), 11)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "b.a.yandex.ru", 80), 12)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "d.b.a.yandex.ru", 80), 12)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "yandex.ru", 80), 10)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "tam.by", 80), 16)
                .put(new WebmasterHostId(WebmasterHostId.Schema.HTTP, "a.b.c", 80), 16)

                .build();
        result.entrySet()
                .forEach(entry -> {
                    int answer = (int)(crawlSettingsService.getBaseCrawlInfo(entry.getKey()).getCrawlRPS());
                    Assert.assertEquals(entry.getValue().intValue(), answer);
                });
    }

    public static class BaseCrawlInfoYDaoMock extends BaseCrawlInfoYDao {
        private static final Map<String, BaseCrawlInfo> INFOS = ImmutableMap.<String, BaseCrawlInfo>builder()
                .put("https://www.lenta.ru", new BaseCrawlInfo(1))
                .put("https://*.yandex.ru", new BaseCrawlInfo(2))
                .put("https://*.a.yandex.ru", new BaseCrawlInfo(4))
                .put("https://*.b.a.yandex.ru", new BaseCrawlInfo(5))
                .put("https://*.ru", new BaseCrawlInfo(6))
                .put("https://www", new BaseCrawlInfo(9))

                .put("http://www.lenta.ru", new BaseCrawlInfo(13))
                .put("http://*.yandex.ru", new BaseCrawlInfo(10))
                .put("http://*.a.yandex.ru", new BaseCrawlInfo(11))
                .put("http://*.b.a.yandex.ru", new BaseCrawlInfo(12))
                .put("http://*.ru", new BaseCrawlInfo(13))
                .put("http://*", new BaseCrawlInfo(16))
                .build();

        public BaseCrawlInfo getInfo(String hostPattern, DateTime addDate) {
            return INFOS.get(hostPattern);
        }
    }

    public static class SettingsServiceMock extends SettingsService {
        @NotNull
        public CommonDataState getSettingCached(CommonDataType type) {
            if (type == CommonDataType.CRAWL_DELAY_DEFAULT_RPS) {
                return new CommonDataState(CommonDataType.CRAWL_DELAY_DEFAULT_RPS, "3.00", DateTime.now());
            }
            if (type == CommonDataType.CRAWL_DELAY_IMPORT_DATE) {
                return new CommonDataState(CommonDataType.CRAWL_DELAY_IMPORT_DATE, DateTime.now().toString(),
                        DateTime.now());
            }
            return null;
        }
    }
}
