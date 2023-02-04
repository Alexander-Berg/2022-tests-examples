package ru.yandex.wmtools.common.util.uri;

import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class WebmasterUriUtilsTest {
    @Test
    public void testUrlEncode() throws Exception {
        final String URL_ORIG = "http://example.com/проверка.html";
        final String URL_ENCODED = "http://example.com/%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D1%80%D0%BA%D0%B0.html";

        Assert.assertEquals(URL_ENCODED, WebmasterUriUtils.toOldUri(URL_ORIG).toString());
    }

    @Test
    public void testNoReencode() throws Exception {
        final String URL_ENCODED = "http://example.com/%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D1%80%D0%BA%D0%B0.html";
        Assert.assertEquals(URL_ENCODED, WebmasterUriUtils.toOldUri(URL_ENCODED).toString());
    }

    @Test
    public void testPunycode() throws Exception {
        final String URL_ORIG = "http://ввв.пример.рф";
        final String HOST = "http://xn--b1aaa.xn--e1afmkfd.xn--p1ai";

        Assert.assertEquals(HOST, WebmasterUriUtils.toOldUri(URL_ORIG).toString());
    }

    @Test
    public void testOldUri1() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("http://deaction.com/lmages/index.php?p=chloe+lesbian+porna64");
        Assert.assertEquals("http://deaction.com/lmages/index.php?p=chloe%20lesbian%20porna64", uri.toString());
    }

    @Test
    public void testOldUri2() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri(
                "http://www.annazhuk.ru/#fotosessija-beremennyh-fotosessija-detej-semejnaja-semka-love-story");
        Assert.assertEquals("http://www.annazhuk.ru/", uri.toString());
    }

    @Test
    public void testOldUri3() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("http://arte-life.ru/sitemap[202531_6].xml");
        Assert.assertEquals("http://arte-life.ru/sitemap%5B202531_6%5D.xml", uri.toString());
    }

    @Test
    public void testOldUri4() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri(
                "http://Rx24.ru/?p=1&q=%D0%BF%D1%80%D0%BE%D1%81%D1%82%D0%B8%D1%82%D1%83%D1%82%D0%BA%D0%B8+%D0%B2%D0%BE%D1%80%D0%BE%D0%BD%D0%B5%D0%B6%D0%B0");
        Assert.assertEquals(
                "http://rx24.ru/?p=1&q=%D0%BF%D1%80%D0%BE%D1%81%D1%82%D0%B8%D1%82%D1%83%D1%82%D0%BA%D0%B8%20%D0%B2%D0%BE%D1%80%D0%BE%D0%BD%D0%B5%D0%B6%D0%B0", uri.toString());
    }

    @Test
    public void testIPv4Hostname() throws Exception {
        try {
            WebmasterUriUtils.toOldUri("http://127.0.0.1/weqwe");
            Assert.fail();
        } catch (Exception e) {}
    }

    @Test
    public void testIPv6Hostname() throws Exception {
        try {
            WebmasterUriUtils.toOldUri("http://[2001:db8::ff00:42:8329]/weqwe");
            Assert.fail();
        } catch (Exception e) {}
    }

    @Test
    public void testSchemaFix() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("lenta.ru");
        Assert.assertEquals("http://lenta.ru", uri.toASCIIString());
    }

    @Test
    public void testWrongSchema() throws Exception {
        try {
            WebmasterUriUtils.toOldUri("tcp://lenta.ru/weqwe");
            Assert.fail();
        } catch (Exception e) {}
    }

    @Test
    public void testPortFix1() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("lenta.ru:80");
        Assert.assertEquals("http://lenta.ru", uri.toASCIIString());
    }

    @Test
    public void testPortFix2() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("https://lenta.ru:443");
        Assert.assertEquals("https://lenta.ru", uri.toASCIIString());
    }

    @Test
    public void testPortFix3() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("lenta.ru:443");
        Assert.assertEquals("http://lenta.ru:443", uri.toASCIIString());
    }

    @Test
    public void testPortFix4() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("lenta.ru:90");
        Assert.assertEquals("http://lenta.ru:90", uri.toASCIIString());
    }

    @Test
    public void testPortFix5() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("https://lenta.ru:90");
        Assert.assertEquals("https://lenta.ru:90", uri.toString());
    }

    @Test
    public void testNormalizeURI() throws Exception {
        URI uri = WebmasterUriUtils.toOldUri("HttP://LeNTa.rU:80/dDd%3adDd%41dDd");
        Assert.assertEquals("http://lenta.ru/dDd%3AdDd%41dDd", uri.toString());
    }

    @Test
    public void testHostWithNonLDHASCII() throws Exception{
        try {
            String s = "<title>Яндекс.Словари</title><link rel=\"search\" href=\"/opensearch.xml";
            URI uri = WebmasterUriUtils.toOldUri(s);
            Assert.fail("Url with host, containing non-LDH ASCII " + s + " converted to url: " + uri.toASCIIString());
        } catch (Exception e){
        }
    }
}
