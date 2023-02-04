package ru.yandex.webmaster3.storage.spam;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;

import java.util.Arrays;
import java.util.function.Predicate;

/**
 * @author avhaliullin
 */
public class SpamFilterUtilTest {
    private static Predicate<WebmasterHostId> YANDEX_GOOGLE() {
        return SpamFilterUtil.makeIsBannedPredicateForSuffixes(Arrays.asList(
                ".yandex.ru",
                ".google.com",
                ".vintronddns.com"
        ));
    }

    private void assertBan(Predicate<WebmasterHostId> predicate, WebmasterHostId hostId) {
        Assert.assertTrue(hostId + " should be banned", predicate.test(hostId));
    }

    private void assertNoBan(Predicate<WebmasterHostId> predicate, WebmasterHostId hostId) {
        Assert.assertFalse(hostId + " should not be banned", predicate.test(hostId));
    }

    @Test
    public void testBanForSuffix() {
        WebmasterHostId siteYaRU = IdUtils.stringToHostId("http:site.yandex.ru:80");
        assertBan(YANDEX_GOOGLE(), siteYaRU);
        WebmasterHostId analyticsGoogle = IdUtils.stringToHostId("https:analytics.google.com:443");
        assertBan(YANDEX_GOOGLE(), analyticsGoogle);
        assertBan(YANDEX_GOOGLE(), IdUtils.urlToHostId("http://knowunerer.vintronddns.com/315689.cfm"));
    }

    @Test
    public void testNoBanForUnrelated() {
        WebmasterHostId lenta = IdUtils.stringToHostId("https:lenta.ru:443");
        assertNoBan(YANDEX_GOOGLE(), lenta);
    }
}
