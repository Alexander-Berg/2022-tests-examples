package ru.yandex.wmconsole.util;

import org.junit.Test;
import ru.yandex.wmtools.common.error.UserException;
import ru.yandex.wmtools.common.servantlet.AbstractServantlet;
import ru.yandex.wmtools.common.util.URLUtil;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import static org.junit.Assert.*;

/**
 * @author ailyin
 */
public class UrlUtilTest {
    @Test
    public void testIsValid() throws MalformedURLException, URISyntaxException {
        assertTrue(AbstractServantlet.isValid(new URL("http://www.yandex.ru")));
        assertFalse(AbstractServantlet.isValid(new URL("http://www.yan dex.ru")));
        assertTrue(AbstractServantlet.isValid(new URL("http://www.apelsin.travel")));
        assertTrue(AbstractServantlet.isValid(new URL("http://www.hermitage.museum")));
        assertFalse(AbstractServantlet.isValid(new URL("http://www.apel sin.travel")));
        //assertFalse(AbstractServantlet.isValid(new URL("http://www.test.longdomain")));
        assertTrue(AbstractServantlet.isValid(new URL("http://www.travel.ru")));
        assertTrue(AbstractServantlet.isValid(new URL("http://www.fashionbank.ru/models/user/51214%27%20UNION%20SELECT%201,2,concat_ws(%22:%22,table_name,column_name),4,5,6,7%20from%20information_schema.columns%20--%20.html")));
        assertTrue(AbstractServantlet.isValid(new URL("http://ru.wikipedia.org/wiki/%D0%9E%D0%B1%D1%81%D1%83%D0%B6%D0%B4%D0%B5%D0%BD%D0%B8%D0%B5:%D0%A2%D0%B0%D1%80%D0%B0%D1%81_%D0%91%D1%83%D0%BB%D1%8C%D0%B1%D0%B0_(%D1%84%D0%B8%D0%BB%D1%8C%D0%BC)")));
        assertFalse(AbstractServantlet.isValid(new URL("http://россия.рф")));
        assertFalse(AbstractServantlet.isValid(new URL("http://www.xn--h1aaia0ab.xn--p1ai/туры")));
        assertFalse(AbstractServantlet.isValid(new URL("http://yandex")));
        assertFalse(AbstractServantlet.isValid(new URL("http://yandex.ru/?%A%DO")));
        assertTrue(AbstractServantlet.isValid(new URL("http://yandex.ru/?%A0%DO")));
        assertTrue(AbstractServantlet.isValid(new URL("https://yandex.ru:80/?%A0%DO")));
        assertFalse(AbstractServantlet.isValid(new URL("ftp://yandex.ru/")));
        assertFalse(AbstractServantlet.isValid(new URL("http://yandex.ru:100500")));
        assertTrue(AbstractServantlet.isValid(new URL("http://pc_znak.spravka.ua")));
        // Не должно быть "_" в доменах второго и первого уровня
        assertFalse(AbstractServantlet.isValid(new URL("http://some_site.ru")));
        assertFalse(AbstractServantlet.isValid(new URL("http://first_level")));
        
        assertFalse(AbstractServantlet.isValid(new URL("http://allmoments.ruseller.com/?nasty>")));
    }

    @Test
    public void testUrlWithFragment() {
        assertTrue(AbstractServantlet.isValid("http://some.domain.ru/602446_%3A%2F%2F#comment42311"));
        assertFalse(AbstractServantlet.isValid("http://some.domain.ru/602446_%3A%2F%2F#comment-4231#comment-2545#comment3"));
        assertFalse(AbstractServantlet.isValid("http://new.klassa.bg/news/Read/article/62445_%25D0%25A1%25D1%2582%25D0%25BE%25D1%258F%25D0%25BD%2B%25D0%259A%25D1%2583%25D1%2588%25D0%25BB%25D0%25B5%25D0%25B2:%25BD%25D0%25B0%25D1%2588%25D1%2583%25D0%25BC%25D0%25B5%25D0%25BB%25D0%25B8%2B%25D0%25B1%25D0%25B0%25D0%25BD%25D0%25B4%25D0%25B8%25D1%2582%25D0%25B8%2522/#comment-426627/#comment-564045/#comment-829755"));
        assertFalse(AbstractServantlet.isValid("http://www.maxelt.ru/links.php?id=<a href=http://techdorcomp.ru>site</a>"));
        assertFalse(AbstractServantlet.isValid("http://www.nanocad.ru/blogs/index.php?page=search&tags=%D1%C0%"));
        assertFalse(AbstractServantlet.isValid("http://piyo.fc2.com/contents/jump/?jumpuri=http://opera-mini4you.ru /"));
        assertFalse(AbstractServantlet.isValid("http://www.zomex.ru/app/download/5525063110/sitemap%5B33585_349%.xml?t=1324301654"));
        assertFalse(AbstractServantlet.isValid("http://worldmusik.ru/?q=&#226;&#224;&#229;&#237;&#227;&#224;&amp;page=4"));
        assertFalse(AbstractServantlet.isValid("http://russianslc.com/r.php?\"http://1yagody.ru\""));
        assertFalse(AbstractServantlet.isValid("http://yandex.ru/?1=1%"));
    }

    @Test
    public void testUrlWithNonStandardPaths() {
        // Расширение - поддержка квадратных скобок в запросе
        assertTrue(AbstractServantlet.isValid("http://www.euro-dacha.ru/shop/CID_66.html?v[40]=89"));
    }

    @Test
    public void testGetEncodedUrlSize() throws UnsupportedEncodingException {
        final String urlPram = "это тестовая строка с различными симоволами: !\"№%:,.;()<>";
        final String encoding = "UTF-8";
        String expected = URLEncoder.encode(urlPram, encoding);
        assertEquals(expected.length(), URLUtil.getEncodedURLSize(urlPram, encoding));
    }

    @Test
    public void testIsHomepageNoSlash() throws UserException {
        URL url = AbstractServantlet.prepareUrl("http://test.ru", true);
        assertTrue(URLUtil.isHomePage(url));
    }

    @Test
    public void testIsHomepageWithSlash() throws UserException {
        URL url = AbstractServantlet.prepareUrl("http://test.ru/", true);
        assertTrue(URLUtil.isHomePage(url));
    }

    @Test
    public void testIsHomepageWithNonEmptyPath() throws UserException {
        URL url = AbstractServantlet.prepareUrl("http://test.ru/testing/", true);
        assertFalse(URLUtil.isHomePage(url));
    }

    @Test
    public void testIsHomepageWithSlashAndEmptyQuery() throws UserException {
        URL url = AbstractServantlet.prepareUrl("http://test.ru/?", true);
        assertFalse(URLUtil.isHomePage(url));
    }

    @Test
    public void testIsHomePageWithFragment() throws UserException {
        URL url = AbstractServantlet.prepareUrl("http://test.ru/#fragment", true);
        assertFalse(URLUtil.isHomePage(url));
    }
    
    @Test
    public void testIsSpamerDomain() {
        assertTrue(URLUtil.isSpamerDomain("a-b.c.ru.com"));
        assertTrue(URLUtil.isSpamerDomain("a-b-c.d.ru.com"));
        assertFalse(URLUtil.isSpamerDomain("a.b.ru.com"));
        assertFalse(URLUtil.isSpamerDomain("a.b.ru com"));
        assertFalse(URLUtil.isSpamerDomain("a.b.rucom"));
        assertFalse(URLUtil.isSpamerDomain("a-c.bru.com"));
        assertFalse(URLUtil.isSpamerDomain("a-c.b.ru.com.org"));
        assertFalse(URLUtil.isSpamerDomain("a-c.b.ru.company.org"));
        assertFalse(URLUtil.isSpamerDomain("b.ru.com"));
        assertTrue(URLUtil.isSpamerDomain("https://a-b.c.ru.com"));
        assertTrue(URLUtil.isSpamerDomain("a-b.c.ru.com:80"));
        assertTrue(URLUtil.isSpamerDomain("a-b.c.ru.com:81"));
        assertTrue(URLUtil.isSpamerDomain("https://a-b.c.ru.com:443"));
        assertTrue(URLUtil.isSpamerDomain("https://a-b.c.ru.com:444"));
        assertFalse(URLUtil.isSpamerDomain("lenta.ru:80"));
        assertFalse(URLUtil.isSpamerDomain("lenta.ru:81"));
        assertFalse(URLUtil.isSpamerDomain("https://lenta.ru:443"));
        assertFalse(URLUtil.isSpamerDomain("https://lenta.ru:444"));
    }

    @Test
    public void testGetHostName() throws MalformedURLException {
        assertEquals("http://kavkazfamily.ru", URLUtil.getHostName(new URL("http://kavkazfamily.ru/sitemap.xml"), true));
    }

    @Test
    public void testGetRelativeUrl() throws MalformedURLException {
        assertEquals("/sitemap.xml", URLUtil.getRelativeUrl(new URL("http://kavkazfamily.ru/sitemap.xml")));
    }
}
