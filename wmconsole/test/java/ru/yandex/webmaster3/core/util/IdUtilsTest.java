package ru.yandex.webmaster3.core.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.webmaster3.core.data.WebmasterHostId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User: azakharov
 * Date: 03.06.15
 * Time: 12:43
 */
public class IdUtilsTest {
    @Test
    public void testSwitchHttpsToHttp() {
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "test.ru", 443);
        WebmasterHostId satelliteId = IdUtils.switchHttps(hostId);
        Assert.assertEquals(WebmasterHostId.Schema.HTTP, satelliteId.getSchema());
        Assert.assertEquals("test.ru", satelliteId.getPunycodeHostname());
        Assert.assertEquals(80, satelliteId.getPort());
    }

    @Test
    public void testSwitchHttpToHttps() {
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "test.ru", 80);
        WebmasterHostId satelliteId = IdUtils.switchHttps(hostId);
        Assert.assertEquals(WebmasterHostId.Schema.HTTPS, satelliteId.getSchema());
        Assert.assertEquals("test.ru", satelliteId.getPunycodeHostname());
        Assert.assertEquals(443, satelliteId.getPort());
    }

    @Test
    public void testSwitchHttpNonStandardPort() {
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "test.ru", 42);
        WebmasterHostId satelliteId = IdUtils.switchHttps(hostId);
        Assert.assertEquals(WebmasterHostId.Schema.HTTPS, satelliteId.getSchema());
        Assert.assertEquals("test.ru", satelliteId.getPunycodeHostname());
        Assert.assertEquals(42, satelliteId.getPort());
    }

    @Test
    public void testSwitchHttpsNonStandardPort() {
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "test.ru", 29);
        WebmasterHostId satelliteId = IdUtils.switchHttps(hostId);
        Assert.assertEquals(WebmasterHostId.Schema.HTTP, satelliteId.getSchema());
        Assert.assertEquals("test.ru", satelliteId.getPunycodeHostname());
        Assert.assertEquals(29, satelliteId.getPort());
    }

    @Test
    public void testWebIdStringToHostId() throws Exception {
        WebmasterHostId expected = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "tip_top_coroflake.7640906.ru",
                WebmasterHostId.DEFAULT_HTTP_PORT);
        WebmasterHostId hostId = IdUtils.webIdStringToHostId("http:tip_top_coroflake.7640906.ru:80", true);
        Assert.assertNotNull(hostId);
        Assert.assertEquals(expected, hostId);
    }

    @Test
    public void testSwitchSchema() throws Exception {
        WebmasterHostId expectedHttp = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "example.com", 80);
        WebmasterHostId expectedHttps = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "example.com", 443);

        Assert.assertEquals(expectedHttp, IdUtils.withSchema(expectedHttp, WebmasterHostId.Schema.HTTP));
        Assert.assertEquals(expectedHttps, IdUtils.withSchema(expectedHttp, WebmasterHostId.Schema.HTTPS));

        Assert.assertEquals(expectedHttp, IdUtils.withSchema(expectedHttps, WebmasterHostId.Schema.HTTP));
        Assert.assertEquals(expectedHttps, IdUtils.withSchema(expectedHttps, WebmasterHostId.Schema.HTTPS));
    }

    @Test
    public void testStringToHostId() throws Exception {
        WebmasterHostId expectedHttp = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "example.com", 80);
        Assert.assertEquals(expectedHttp, IdUtils.stringToHostId(expectedHttp.toStringId()));

        WebmasterHostId expectedHttps = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "example.com", 443);
        Assert.assertEquals(expectedHttps, IdUtils.stringToHostId(expectedHttps.toStringId()));

        WebmasterHostId expectedHttpPort = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "example.com", 8080);
        Assert.assertEquals(expectedHttpPort, IdUtils.stringToHostId(expectedHttpPort.toStringId()));

        WebmasterHostId expectedHttpsPort = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "example.com", 8080);
        Assert.assertEquals(expectedHttpsPort, IdUtils.stringToHostId(expectedHttpsPort.toStringId()));

        WebmasterHostId expectedHostWithUnderscore = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "region_19.joytaxi.ru", 8080);
        Assert.assertEquals(expectedHostWithUnderscore, IdUtils.stringToHostId(expectedHostWithUnderscore.toStringId()));
    }

    @Test
    public void testUrlToHostId() throws Exception {
        WebmasterHostId expectedUnderscoredHostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "region_19.joytaxi.ru", WebmasterHostId.DEFAULT_HTTP_PORT);
        Assert.assertEquals(expectedUnderscoredHostId, IdUtils.urlToHostId("region_19.joytaxi.ru"));
    }

    @Test
    public void testBadHostWilNotPass() {
        assertBadHostNameFails("http://abc,def.ru");
        assertBadHostNameFails("http://прези+дент.рф");
        assertBadHostNameFails("рф");
    }

    @Test
    public void toRelativeUrlTest() throws Exception {
        WebmasterHostId hostId = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "a.b.example.uk.com", 80);
        String url = "http://a.b.example.uk.com";
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, url));
    }

    @Test
    public void allHostsForDomainForHttp() {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");
        Set<WebmasterHostId> expected = new HashSet<>(Arrays.asList(
                IdUtils.urlToHostId("http://example.com"),
                IdUtils.urlToHostId("https://example.com"),
                IdUtils.urlToHostId("http://www.example.com"),
                IdUtils.urlToHostId("https://www.example.com")
        ));
        Assert.assertEquals(expected, new HashSet<>(IdUtils.allHostsForDomain(hostId)));
    }

    @Test
    public void allHostsForDomainForHttpWww() {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://www.example.com");
        Set<WebmasterHostId> expected = new HashSet<>(Arrays.asList(
                IdUtils.urlToHostId("http://example.com"),
                IdUtils.urlToHostId("https://example.com"),
                IdUtils.urlToHostId("http://www.example.com"),
                IdUtils.urlToHostId("https://www.example.com")
        ));
        Assert.assertEquals(expected, new HashSet<>(IdUtils.allHostsForDomain(hostId)));
    }

    @Test
    public void allHostsForDomainForHttps() {
        WebmasterHostId hostId = IdUtils.urlToHostId("https://example.com");
        Set<WebmasterHostId> expected = new HashSet<>(Arrays.asList(
                IdUtils.urlToHostId("http://example.com"),
                IdUtils.urlToHostId("https://example.com"),
                IdUtils.urlToHostId("http://www.example.com"),
                IdUtils.urlToHostId("https://www.example.com")
        ));
        Assert.assertEquals(expected, new HashSet<>(IdUtils.allHostsForDomain(hostId)));
    }

    @Test
    public void allHostsForDomainForHttpsWww() {
        WebmasterHostId hostId = IdUtils.urlToHostId("https://www.example.com");
        Set<WebmasterHostId> expected = new HashSet<>(Arrays.asList(
                IdUtils.urlToHostId("http://example.com"),
                IdUtils.urlToHostId("https://example.com"),
                IdUtils.urlToHostId("http://www.example.com"),
                IdUtils.urlToHostId("https://www.example.com")
        ));
        Assert.assertEquals(expected, new HashSet<>(IdUtils.allHostsForDomain(hostId)));
    }

    @Test
    public void allHostsForDomainForWwwSubdomain() {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://www.sbd.example.com");
        Set<WebmasterHostId> expected = new HashSet<>(Arrays.asList(
                IdUtils.urlToHostId("http://sbd.example.com"),
                IdUtils.urlToHostId("https://sbd.example.com"),
                IdUtils.urlToHostId("http://www.sbd.example.com"),
                IdUtils.urlToHostId("https://www.sbd.example.com")
        ));
        Assert.assertEquals(expected, new HashSet<>(IdUtils.allHostsForDomain(hostId)));
    }

    private static void assertBadHostNameFails(String hostName) {
        try {
            IdUtils.urlToHostId(hostName);
            Assert.fail("Should fail on " + hostName);
        } catch (Exception e) {
        }
    }
}
