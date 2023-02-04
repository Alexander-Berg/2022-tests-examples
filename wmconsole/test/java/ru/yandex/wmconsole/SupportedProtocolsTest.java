package ru.yandex.wmconsole;

import org.junit.Test;
import ru.yandex.wmtools.common.SupportedProtocols;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests {@link SupportedProtocols}  enumeration.
 *
 * @author ailyin
 */
public class SupportedProtocolsTest {
    @Test
    public void testGetCanonicalHostname() throws MalformedURLException {
        assertEquals("www.yandex.ru", SupportedProtocols.getCanonicalHostname(new URL("http://www.yandex.ru")));
        assertEquals("www.yandex.ru", SupportedProtocols.getCanonicalHostname(new URL("http://www.yandex.ru:80")));
        assertEquals("www.yandex.ru:8080",
                   SupportedProtocols.getCanonicalHostname(new URL("http://www.yandex.ru:8080/test.xml")));

        assertEquals("https://www.yandex.ru", SupportedProtocols.getCanonicalHostname(new URL("https://www.yandex.ru")));
        assertEquals("https://www.yandex.ru:80",
                SupportedProtocols.getCanonicalHostname(new URL("https://www.yandex.ru:80")));
        assertEquals("https://www.yandex.ru",
                SupportedProtocols.getCanonicalHostname(new URL("https://www.yandex.ru:443/test.html")));
    }

    @Test
    public void testGetURLWithScheme() throws MalformedURLException, URISyntaxException, SupportedProtocols.UnsupportedProtocolException {
        assertEquals("http://www.yandex.ru", SupportedProtocols.getURL("http://www.yandex.ru").toString());
        assertEquals("https://www.yandex.ru", SupportedProtocols.getURL("https://www.yandex.ru").toString());
    }

    @Test
    public void testGetUrlWithoutScheme() throws MalformedURLException, URISyntaxException, SupportedProtocols.UnsupportedProtocolException {
        assertEquals("http://www.yandex.ru", SupportedProtocols.getURL("www.yandex.ru").toString());
    }

    @Test(expected = SupportedProtocols.UnsupportedProtocolException.class)
    public void testGetUrlUnknownProtocol() throws MalformedURLException, URISyntaxException, SupportedProtocols.UnsupportedProtocolException {
        SupportedProtocols.getURL("file://www.yandex.ru").toString();
        fail();
    }

    @Test
    public void testGetUrlWithSchemeDelimiterInParameters() throws MalformedURLException, URISyntaxException, SupportedProtocols.UnsupportedProtocolException {
        SupportedProtocols.getURL("webmaster.yandex.ru/?referer=http://webmaster.yandex.ru").toString();
        SupportedProtocols.getURL("http://webmaster.yandex.ru/?referer=http://webmaster.yandex.ru").toString();
    }

    @Test
    public void testUrlWithSchemeDelimiter() throws MalformedURLException, URISyntaxException, SupportedProtocols.UnsupportedProtocolException {
        SupportedProtocols.getURL("www.serik.gen.tr/%20http://serik.meb.gov.tr/index.php?option=com_content%2526task");
    }
}
