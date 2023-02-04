package ru.yandex.webmaster3.viewer.http.addurl;

import junit.framework.TestCase;
import org.junit.Assert;

import ru.yandex.webmaster3.core.data.WebmasterHostId;
import ru.yandex.webmaster3.core.util.IdUtils;

/**
 * @author tsyplyaev
 */

public class AddRecrawlUrlRequestsActionTest extends TestCase {
    public void testPrepareURL() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "example.com"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "example.com:80"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "example.com/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "example.com:80/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://example.com/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://example.com:80/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://example.com/?"));

        Assert.assertEquals("/test", IdUtils.toRelativeUrl(hostId, "http://example.com/test"));
        Assert.assertEquals("/test", IdUtils.toRelativeUrl(hostId, "http://example.com/test?"));
        Assert.assertEquals("/test?test", IdUtils.toRelativeUrl(hostId, "http://example.com/test?test"));
        Assert.assertEquals("/test?test=test", IdUtils.toRelativeUrl(hostId, "http://example.com/test?test=test"));
        Assert.assertEquals("/?test=test", IdUtils.toRelativeUrl(hostId, "http://example.com/?test=test"));
        Assert.assertEquals("/?test", IdUtils.toRelativeUrl(hostId, "http://example.com/?test"));

        Assert.assertEquals("/?a={}", IdUtils.toRelativeUrl(hostId, "/?a={}"));
        Assert.assertEquals("/?a={}", IdUtils.toRelativeUrl(hostId, "http://example.com/?a={}"));

        Assert.assertEquals("/?a={}", IdUtils.toRelativeUrl(hostId, "/?a=%7B%7D"));
        Assert.assertEquals("/?a={}", IdUtils.toRelativeUrl(hostId, "http://example.com/?a=%7B%7D"));

        Assert.assertEquals("/f?ello={}&this=[1]", IdUtils.toRelativeUrl(hostId, "/f?ello={}&this=[1]"));
        Assert.assertEquals("/f?ello={}&this=[1]", IdUtils.toRelativeUrl(hostId, "/f?ello=%7B%7D&this=%5B1%5D"));
    }

    public void testHttps() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("https://example.com");

        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "https://example.com"));

        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "/"));
        Assert.assertNull(IdUtils.toRelativeUrl(hostId, "example.com"));
        Assert.assertNull(IdUtils.toRelativeUrl(hostId, "example.com:80"));
        Assert.assertNull(IdUtils.toRelativeUrl(hostId, "example.com:443"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "https://example.com/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "https://example.com/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "https://example.com:443/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "https://example.com/?"));

        Assert.assertEquals("/test", IdUtils.toRelativeUrl(hostId, "https://example.com/test"));
        Assert.assertEquals("/test", IdUtils.toRelativeUrl(hostId, "https://example.com/test?"));
        Assert.assertEquals("/test?test", IdUtils.toRelativeUrl(hostId, "https://example.com/test?test"));
        Assert.assertEquals("/test?test=test", IdUtils.toRelativeUrl(hostId, "https://example.com/test?test=test"));
        Assert.assertEquals("/?test=test", IdUtils.toRelativeUrl(hostId, "https://example.com/?test=test"));
        Assert.assertEquals("/?test", IdUtils.toRelativeUrl(hostId, "https://example.com/?test"));
    }

    public void testRussianURL() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://президент.рф");

        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://президент.рф"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://президент.рф:80"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://президент.рф/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://президент.рф/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://президент.рф:80/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://xn--d1abbgf6aiiy.xn--p1ai"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://xn--d1abbgf6aiiy.xn--p1ai/"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://xn--d1abbgf6aiiy.xn--p1ai:80"));
        Assert.assertEquals("/", IdUtils.toRelativeUrl(hostId, "http://xn--d1abbgf6aiiy.xn--p1ai:80/"));
    }

    public void testRussianUrl1() throws Exception {
        WebmasterHostId hostId = IdUtils.urlToHostId("http://example.com");

        Assert.assertEquals("/супер-товар", IdUtils.toRelativeUrl(hostId, "/супер-товар"));
        Assert.assertEquals("/супер-товар", IdUtils.toRelativeUrl(hostId, "example.com/супер-товар"));
        Assert.assertEquals("/супер-товар", IdUtils.toRelativeUrl(hostId, "example.com:80/супер-товар"));
        Assert.assertEquals("/супер-товар", IdUtils.toRelativeUrl(hostId, "http://example.com/супер-товар"));
        Assert.assertEquals("/супер-товар", IdUtils.toRelativeUrl(hostId, "http://example.com:80/супер-товар"));
        Assert.assertEquals("/супер-товар", IdUtils.toRelativeUrl(hostId, "http://example.com/супер-товар?"));

        Assert.assertEquals("/супер-товар?опция", IdUtils.toRelativeUrl(hostId, "http://example.com/супер-товар?опция"));
        Assert.assertEquals("/супер-товар?опция=", IdUtils.toRelativeUrl(hostId, "http://example.com/супер-товар?опция="));
        Assert.assertEquals("/супер-товар?опция=значение", IdUtils.toRelativeUrl(hostId, "http://example.com/супер-товар?опция=значение"));
    }
}
