package ru.yandex.webmaster3.viewer.http.user;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.common.util.collections.Cf;
import ru.yandex.webmaster3.core.data.HostDomainInfo;
import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;
import ru.yandex.webmaster3.storage.util.HostListTreeBuilder;

import java.util.Collections;
import java.util.List;

import static ru.yandex.webmaster3.core.data.WebmasterHostId.Schema.HTTP;
import static ru.yandex.webmaster3.core.data.WebmasterHostId.Schema.HTTPS;

/**
 * @author aherman
 */
public class HostListTreeBuilderTest {
    private static final HostData lentaRu = createHostData(HTTP, "", "lenta", "ru", 80);
    private static final HostData lentaCom = createHostData(HTTP, "", "lenta", "com", 80);
    private static final HostData lentaComUa = createHostData(HTTP, "", "lenta", "com.ua", 80);
    private static final HostData wwwLentaRu = createHostData(HTTP, "www", "lenta", "ru", 80);
    private static final HostData wwwLentaCom = createHostData(HTTP, "www", "lenta", "com", 80);
    private static final HostData wwwLentaComUa = createHostData(HTTP, "www", "lenta", "com.ua", 80);
    private static final HostData httpsLentaRu = createHostData(HTTPS, "", "lenta", "ru", 443);
    private static final HostData httpsLentaCom = createHostData(HTTPS, "", "lenta", "com", 443);
    private static final HostData httpsLentaComUa = createHostData(HTTPS, "", "lenta", "com.ua", 443);
    private static final HostData httpsWwwLentaRu = createHostData(HTTPS, "www", "lenta", "ru", 443);
    private static final HostData httpsWwwLentaCom = createHostData(HTTPS, "www", "lenta", "com", 443);
    private static final HostData httpsWwwLentaComUa = createHostData(HTTPS, "www", "lenta", "com.ua", 443);
    private static final HostData carobkaRu = createHostData(HTTP, "", "carobka", "ru", 80);
    private static final HostData wwwCasadeluxRu = createHostData(HTTP, "www", "casadelux", "ru", 80);
    private static final HostData lasoonComUa = createHostData(HTTP, "", "lasoon", "com.ua", 80);
    private static final HostData wwwComforttechRu = createHostData(HTTP, "www", "comforttech", "ru", 80);
    private static final HostData wwwComUa = createHostData(HTTP, "www", "", "com.ua", 80);
    private static final HostData prezidentRf = createHostData(HTTP, "", "президент", "рф", 80);
    private static final HostData wwwPrezidentRf = createHostData(HTTP, "www", "президент", "рф", 80);
    private static final HostData flagRf = createHostData(HTTP, "", "\uD83C\uDFC1", "рф", 80);
    private static final HostData wwwFlagRf = createHostData(HTTP, "www", "\uD83C\uDFC1", "рф", 80);

    @Test
    public void testCompareWebmasterHostId() throws Exception {
        List<HostData> testData = Cf.list(
                flagRf,
                wwwFlagRf,
                prezidentRf,
                wwwPrezidentRf,
                wwwLentaRu,
                wwwLentaCom,
                wwwLentaComUa,
                carobkaRu,
                httpsLentaRu,
                httpsLentaCom,
                httpsLentaComUa,
                lasoonComUa,
                wwwCasadeluxRu,
                httpsWwwLentaRu,
                httpsWwwLentaCom,
                httpsWwwLentaComUa,
                wwwComforttechRu,
                lentaRu,
                lentaCom,
                lentaComUa,
                wwwComUa
        );

        List<HostData> expected = Cf.list(
                wwwComUa,
                carobkaRu,
                wwwCasadeluxRu,
                wwwComforttechRu,
                lasoonComUa,
                lentaCom,
                wwwLentaCom,
                httpsLentaCom,
                httpsWwwLentaCom,
                lentaRu,
                wwwLentaRu,
                httpsLentaRu,
                httpsWwwLentaRu,
                lentaComUa,
                wwwLentaComUa,
                httpsLentaComUa,
                httpsWwwLentaComUa,
                prezidentRf,
                wwwPrezidentRf,
                flagRf,
                wwwFlagRf
        );

        Collections.sort(testData);

        Assert.assertEquals(expected, testData);
    }

    @Test
    public void compareTwoHosts() {
        Assert.assertTrue(lentaRu.compareTo(httpsLentaRu) < 0);
        Assert.assertTrue(httpsLentaRu.compareTo(lentaRu) > 0);

        Assert.assertTrue(lentaRu.compareTo(wwwLentaRu) < 0);
        Assert.assertTrue(wwwLentaRu.compareTo(lentaRu) > 0);

        Assert.assertTrue(wwwLentaRu.compareTo(httpsLentaRu) < 0);
        Assert.assertTrue(httpsLentaRu.compareTo(wwwLentaRu) > 0);

        Assert.assertTrue(wwwLentaRu.compareTo(httpsWwwLentaRu) < 0);
        Assert.assertTrue(httpsWwwLentaRu.compareTo(wwwLentaRu) > 0);

        Assert.assertTrue(httpsLentaRu.compareTo(httpsWwwLentaRu) < 0);
        Assert.assertTrue(httpsWwwLentaRu.compareTo(httpsLentaRu) > 0);
    }

    private static HostData createHostData(WebmasterHostId.Schema schema, String prefix, String mid, String owner, int port) {
        prefix = StringUtils.trimToNull(prefix);
        mid = StringUtils.trimToNull(mid);

        String p1 = prefix != null ? IdUtils.IDN.toASCII(prefix) + "." : "";
        String p2 = mid != null ? IdUtils.IDN.toASCII(mid) + "." : "";
        String p3 = IdUtils.IDN.toASCII(owner);
        String punycodeHostname = p1 + p2 + p3;
        WebmasterHostId hostId = new WebmasterHostId(schema, punycodeHostname, port);
        HostDomainInfo hostDomainInfo = new HostDomainInfo(punycodeHostname, p1.length() - 1, punycodeHostname.length() - p3.length());

        return new HostData(hostId, hostDomainInfo);
    }

    private static class HostData implements Comparable<HostData> {
        private final WebmasterHostId hostId;
        private final HostDomainInfo hostDomainInfo;

        public HostData(WebmasterHostId hostId, HostDomainInfo hostDomainInfo) {
            this.hostId = hostId;
            this.hostDomainInfo = hostDomainInfo;
        }

        @Override
        public int compareTo(HostData o) {
            return HostListTreeBuilder.compareWebmasterHostId(hostId, hostDomainInfo, o.hostId, o.hostDomainInfo);
        }

        @Override
        public String toString() {
            return hostId.toString();
        }
    }
}
