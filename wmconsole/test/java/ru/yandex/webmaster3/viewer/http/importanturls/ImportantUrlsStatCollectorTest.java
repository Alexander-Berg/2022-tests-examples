package ru.yandex.webmaster3.viewer.http.importanturls;

import junit.framework.TestCase;
import ru.yandex.webmaster3.storage.importanturls.data.ImportantUrl;
import ru.yandex.webmaster3.storage.importanturls.data.ImportantUrlStatus;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author akhazhoyan 03/2018
 */
public final class ImportantUrlsStatCollectorTest extends TestCase {

    private List<ImportantUrl> urlsFromCodes(int... codes) {
        return Arrays.stream(codes)
                .mapToObj(c -> new ImportantUrlStatus.IndexingInfo(c, null, null))
                .map(i -> new ImportantUrlStatus(null, null, false, false, false, false, false, i, null))
                .map(s -> new ImportantUrl(null, null, null, s, false))
                .collect(Collectors.toList());
    }

    public void testDoCollectStatsNoUrls() {
        ImportantUrlsStatCollector actual = new ImportantUrlsStatCollector();
        urlsFromCodes().forEach(actual::update);
        assertEquals(new EnumMap<>(ImportantUrlsStatGroup.class), actual.getResult());
    }

    public void testDoCollectStatsReguralCodes() {
        {
            ImportantUrlsStatCollector actual = new ImportantUrlsStatCollector();
            urlsFromCodes(
                    101,
                    201, 203,
                    302,
                    402, 404,
                    500, 503, 505
            ).forEach(actual::update);
            Map<ImportantUrlsStatGroup, Integer> expected = new EnumMap<>(ImportantUrlsStatGroup.class);
            expected.put(ImportantUrlsStatGroup.TOTAL_URLS, 9);
            expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_1XX, 1);
            expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_2XX, 2);
            expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_3XX, 1);
            expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_4XX, 2);
            expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_5XX, 3);
            assertEquals(expected, actual.getResult());
        }
        {
            ImportantUrlsStatCollector actual = new ImportantUrlsStatCollector();
            urlsFromCodes(
                    500, 500, 500
            ).forEach(actual::update);
            Map<ImportantUrlsStatGroup, Integer> expected = new EnumMap<>(ImportantUrlsStatGroup.class);
            expected.put(ImportantUrlsStatGroup.TOTAL_URLS, 3);
            expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_5XX, 3);
            assertEquals(expected, actual.getResult());
        }
    }

    public void testDoCollectStatsWithExtCodes() {
        ImportantUrlsStatCollector actual = new ImportantUrlsStatCollector();
        urlsFromCodes(
                101,
                201, 203,
                302,
                402, 404,
                500, 503, 505,
                1001, 1043, 2012, 2025, 3001
        ).forEach(actual::update);
        Map<ImportantUrlsStatGroup, Integer> expected = new EnumMap<>(ImportantUrlsStatGroup.class);
        expected.put(ImportantUrlsStatGroup.TOTAL_URLS, 14);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_1XX, 1);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_2XX, 2);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_3XX, 1);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_4XX, 2);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_5XX, 3);
        expected.put(ImportantUrlsStatGroup.URLS_NA, 5);
        assertEquals(expected, actual.getResult());
    }

    public void testDoCollectStatsNullStatus() {
        ImportantUrlsStatCollector actual = new ImportantUrlsStatCollector();
        List<ImportantUrl> urls = urlsFromCodes(
                101,
                201, 203,
                302,
                404, 402,
                503, 505, 500
        );
        urls.add(new ImportantUrl(null, null, null, null, false));
        urls.add(new ImportantUrl(null, null, null, null, false));
        urls.forEach(actual::update);
        Map<ImportantUrlsStatGroup, Integer> expected = new EnumMap<>(ImportantUrlsStatGroup.class);
        expected.put(ImportantUrlsStatGroup.TOTAL_URLS, 11);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_1XX, 1);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_2XX, 2);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_3XX, 1);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_4XX, 2);
        expected.put(ImportantUrlsStatGroup.URLS_WITH_CODE_5XX, 3);
        expected.put(ImportantUrlsStatGroup.URLS_NOT_INDEXED_YET, 2);
        assertEquals(expected, actual.getResult());
    }
}
