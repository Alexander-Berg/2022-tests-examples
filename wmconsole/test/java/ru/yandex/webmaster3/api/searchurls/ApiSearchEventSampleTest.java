package ru.yandex.webmaster3.api.searchurls;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.api.searchurls.data.ApiExcludedUrlStatusEnum;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchEventEnum;
import ru.yandex.webmaster3.api.searchurls.data.ApiSearchEventSample;
import ru.yandex.webmaster3.core.sitestructure.SearchUrlStatusEnum;
import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlEventSample;
import ru.yandex.webmaster3.storage.searchurl.samples.data.SearchUrlEventType;
import ru.yandex.webmaster3.storage.searchurl.samples.data.UrlStatusInfo;


/**
 * @author leonidrom
 */
public class ApiSearchEventSampleTest {
    private static String PATH = "/path";
    private static String TITLE = "title";
    private static String URL = "https://lenta.ru" + PATH;

    @Test
    public void testFromSearchUrlEventSample() {
        SearchUrlEventSample rawSample;
        ApiSearchEventSample sample;
        UrlStatusInfo statusInfo;

        DateTime now = DateTime.now(TimeUtils.EUROPE_MOSCOW_ZONE);
        DateTime nowPlus = now.plusDays(2);

        // Страница в поиске
        statusInfo = new UrlStatusInfo(
                SearchUrlStatusEnum.INDEXED_SEARCHABLE, null, null, 200, null,
                null, null, null, null,
                false, false, false, false, false);
        rawSample = new SearchUrlEventSample(
                PATH, URL, TITLE, now.toLocalDate(), nowPlus.toLocalDateTime(), SearchUrlEventType.NEW, statusInfo);
        sample = ApiSearchEventSample.fromSearchUrlEventSample(rawSample);
        Assert.assertNotNull(sample);
        Assert.assertEquals(ApiSearchEventEnum.APPEARED_IN_SEARCH, sample.getEvent());
        Assert.assertEquals(URL, sample.getUrl().toExternalForm());
        Assert.assertEquals(TITLE, sample.getTitle());
        Assert.assertEquals(now.withTimeAtStartOfDay(), sample.getLastAccess());
        Assert.assertEquals(nowPlus.withTimeAtStartOfDay(), sample.getEventDate());
        Assert.assertNull(sample.getExcludedUrlStatus().orElse(null));
        Assert.assertNull(sample.getBadHttpStatus().orElse(null));
        Assert.assertNull(sample.getTargetUrl().orElse(null));

        // Страница не в поиске, статус HTTP_ERROR
        statusInfo = new UrlStatusInfo(
                SearchUrlStatusEnum.HTTP_ERROR, null, null, 502, null,
                null, null, null, null,
                false, false, false, false, false);
        rawSample = new SearchUrlEventSample(
                PATH, URL, TITLE, now.toLocalDate(), nowPlus.toLocalDateTime(), SearchUrlEventType.GONE, statusInfo);
        sample = ApiSearchEventSample.fromSearchUrlEventSample(rawSample);
        Assert.assertNotNull(sample);
        Assert.assertEquals(ApiSearchEventEnum.REMOVED_FROM_SEARCH, sample.getEvent());
        Assert.assertEquals(URL, sample.getUrl().toExternalForm());
        Assert.assertEquals(TITLE, sample.getTitle());
        Assert.assertEquals(now.withTimeAtStartOfDay(), sample.getLastAccess());
        Assert.assertEquals(nowPlus.withTimeAtStartOfDay(), sample.getEventDate());
        Assert.assertEquals(ApiExcludedUrlStatusEnum.HTTP_ERROR, sample.getExcludedUrlStatus().orElse(null));
        Assert.assertEquals(Integer.valueOf(502), sample.getBadHttpStatus().orElse(null));
        Assert.assertNull(sample.getTargetUrl().orElse(null));

        // Страница не в поиске, не пустой target url
        statusInfo = new UrlStatusInfo(
                SearchUrlStatusEnum.NOT_CANONICAL, null, null, 200, null,
                null, null, URL, null,
                false, false, false, false, false);
        rawSample = new SearchUrlEventSample(
                PATH, URL, TITLE, now.toLocalDate(), nowPlus.toLocalDateTime(), SearchUrlEventType.GONE, statusInfo);
        sample = ApiSearchEventSample.fromSearchUrlEventSample(rawSample);
        Assert.assertNotNull(sample);
        Assert.assertEquals(ApiSearchEventEnum.REMOVED_FROM_SEARCH, sample.getEvent());
        Assert.assertEquals(URL, sample.getUrl().toExternalForm());
        Assert.assertEquals(TITLE, sample.getTitle());
        Assert.assertEquals(now.withTimeAtStartOfDay(), sample.getLastAccess());
        Assert.assertEquals(nowPlus.withTimeAtStartOfDay(), sample.getEventDate());
        Assert.assertEquals(ApiExcludedUrlStatusEnum.NOT_CANONICAL, sample.getExcludedUrlStatus().orElse(null));
        Assert.assertNull(sample.getBadHttpStatus().orElse(null));
        Assert.assertEquals(URL, sample.getTargetUrl().map(java.net.URL::toExternalForm).orElse(null));
    }
}
