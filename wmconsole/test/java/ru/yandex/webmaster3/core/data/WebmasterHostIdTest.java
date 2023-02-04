package ru.yandex.webmaster3.core.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class WebmasterHostIdTest {
    @Test
    public void testEquals() throws Exception {
        WebmasterHostId e1 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "example.com", 80);
        WebmasterHostId es1 = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "example.com", 443);

        Assert.assertEquals(e1, new WebmasterHostId(WebmasterHostId.Schema.HTTP, "example.com", 80));
        Assert.assertEquals(es1, new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "example.com", 443));
        Assert.assertNotEquals(e1, es1);

        WebmasterHostId e3 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "example.com", 8080);
        WebmasterHostId e4 = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "example.com", 8080);

        Assert.assertNotEquals(e1, e3);
        Assert.assertNotEquals(e1, e4);
        Assert.assertNotEquals(es1, e3);
        Assert.assertNotEquals(es1, e4);
        Assert.assertNotEquals(e3, e4);

        WebmasterHostId t1 = new WebmasterHostId(WebmasterHostId.Schema.HTTP, "test.com", 80);
        WebmasterHostId ts1 = new WebmasterHostId(WebmasterHostId.Schema.HTTPS, "test.com", 443);

        Assert.assertNotEquals(e1, t1);
        Assert.assertNotEquals(e1, ts1);
        Assert.assertNotEquals(es1, t1);
        Assert.assertNotEquals(es1, ts1);
    }
}
