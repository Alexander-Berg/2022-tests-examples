package ru.yandex.wmtools.common.sita;

import java.net.URI;
import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.wmtools.common.servantlet.AbstractServantlet;

/**
 * @author aherman
 */
public class SitaUrlFetchRequestBuilderTest {
    public static final String URL_ORIG = "http://example.com/проверка.html";
    public static final String URL_ENCODED = "http://example.com/%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D1%80%D0%BA%D0%B0.html";

    public static final String URL_FRAGMENT = "http://www.annazhuk.ru/#fotosessija-beremennyh-fotosessija-detej-semejnaja-semka-love-story";
    public static final String URL_NO_FRAGMENT = "http://www.annazhuk.ru/";

    @Test
    public void testUrlConversion1() throws Exception {
        URL uIn = AbstractServantlet.prepareUrl(URL_ORIG, false);
        URI uOut = new URI(URL_ENCODED);
        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(uIn).createSitaUrlFetchRequest();

        Assert.assertEquals(uOut, sitaUrlFetchRequest.getUri());
    }

    @Test
    public void testUrlConversion2() throws Exception {
        URL uIn = AbstractServantlet.prepareUrl(URL_ENCODED, false);
        URI uOut = new URI(URL_ENCODED);
        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(uIn).createSitaUrlFetchRequest();

        Assert.assertEquals(uOut, sitaUrlFetchRequest.getUri());
    }

    @Test
    public void testPlusInURL() throws Exception {
        URL uIn = AbstractServantlet.prepareUrl("http://deaction.com/lmages/index.php?p=chloe+lesbian+porna64", false);
        String expected = "http://deaction.com/lmages/index.php?p=chloe%20lesbian%20porna64";

        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(uIn).createSitaUrlFetchRequest();
        Assert.assertEquals(expected, sitaUrlFetchRequest.getUri().toString());
    }

    @Test
    public void testBraceInURL() throws Exception {
        URL uIn = AbstractServantlet.prepareUrl("http://arte-life.ru/sitemap[202531_6].xml", false);
        String expected = "http://arte-life.ru/sitemap%5B202531_6%5D.xml";
        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(uIn).createSitaUrlFetchRequest();

        Assert.assertEquals(expected, sitaUrlFetchRequest.getUri().toString());
    }

    @Test
    public void testSemicolonInURL() throws Exception {
        URL uIn = AbstractServantlet.prepareUrl("http://base.consultant.ru/cons/cgi/online.cgi?req=doc;base=AMS;n=188076", false);
        String expected = "http://base.consultant.ru/cons/cgi/online.cgi?req=doc;base%3DAMS;n%3D188076";
        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(uIn).createSitaUrlFetchRequest();

        Assert.assertEquals(expected, sitaUrlFetchRequest.getUri().toString());
    }

    @Test
    public void testFragmentInURL() throws Exception {
        URL uIn = AbstractServantlet.prepareUrl(URL_FRAGMENT, false);
        URI uOut = new URI(URL_NO_FRAGMENT);
        SitaUrlFetchRequest sitaUrlFetchRequest = new SitaUrlFetchRequestBuilder(uIn).createSitaUrlFetchRequest();

        Assert.assertEquals(uOut, sitaUrlFetchRequest.getUri());
    }

    @Test
    public void testTimeout() throws Exception {
        SitaUrlFetchRequest request = new SitaUrlFetchRequestBuilder(new URI("http://example.com"))
                .setRequestTimeout(SitaRequestTimeout._15_SECONDS)
                .createSitaUrlFetchRequest();

        Assert.assertTrue(request.getTimeout() >= SitaRequestTimeout._15_SECONDS.getTimeout());
    }
}
