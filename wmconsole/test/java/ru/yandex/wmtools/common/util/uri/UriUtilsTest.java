package ru.yandex.wmtools.common.util.uri;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author aherman
 */
public class UriUtilsTest {
    @Test
    public void testUrlEncode() throws Exception {
        final String URL_ORIG = "http://example.com/проверка.html";
        final String URL_ENCODED = "http://example.com/%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D1%80%D0%BA%D0%B0.html";

        Assert.assertEquals(URL_ENCODED, UriUtils.toUri(URL_ORIG).toUriString());
    }

    @Test
    public void testNoReencode() throws Exception {
        final String URL_ENCODED = "http://example.com/%D0%BF%D1%80%D0%BE%D0%B2%D0%B5%D1%80%D0%BA%D0%B0.html";
        Assert.assertEquals(URL_ENCODED, UriUtils.toUri(URL_ENCODED).toUriString());
    }

    @Test
    public void testBrace() throws Exception {
        final String URL_ORIG = "http://arte-life.ru/sitemap[202531_6].xml";
        final String URL_ENCODED = "http://arte-life.ru/sitemap%5B202531_6%5D.xml";

        Assert.assertEquals(URL_ENCODED, UriUtils.toUri(URL_ORIG).toUriString());
    }

    @Test
    public void testFragmentClean() throws Exception {
        final String URL_ORIG = "http://www.annazhuk.ru/#fotosessija-beremennyh-fotosessija-detej-semejnaja-semka-love-story";
        final String URL_ENCODED = "http://www.annazhuk.ru/";

        Assert.assertEquals(URL_ENCODED,
                UriUtils.toUri(URL_ORIG, UriUtils.UriFeature.CLEAN_FRAGMENT, UriUtils.UriFeature.CLEAN_AUTHORITY)
                        .toUriString());
    }

    @Test
    public void testUnderscore() throws Exception {
        final String URL_ORIG = "http://www.example_site.com";
        final String HOST = "www.example_site.com";

        Assert.assertEquals(HOST, UriUtils.toUri(URL_ORIG).getHost());
    }

    @Test
    public void testPunycode() throws Exception {
        final String URL_ORIG = "http://ввв.пример.рф";
        final String HOST = "http://xn--b1aaa.xn--e1afmkfd.xn--p1ai";

        Assert.assertEquals(HOST,
                UriUtils.toUri(URL_ORIG, UriUtils.UriFeature.USE_PUNYCODED_HOSTNAME).toUriString());
    }

    @Test
    public void testException1() throws Exception {
        final String URL_ORIG = "http://www.stvcc.ru/forum/index.php?action=profile;u=85288";
        final String URL_ENCODED = "http://www.stvcc.ru/forum/index.php?action=profile;u%3D85288";

        Assert.assertEquals(URL_ENCODED, UriUtils.toUri(URL_ORIG).toUriString());
    }

    @Test
    public void testUriResolve() throws Exception {
        final String BASE_URI = "http://a/b/c/d;p?q";

        // Tests from cpp-netlib

        Assert.assertEquals("http://a/g", resolve("http://a/", "g"));
        Assert.assertEquals("http://a/g/x/y?q#s", resolve("http://a/", "g/x/y?q#s"));
//        Assert.assertEquals("http://a/b/c/d;p?q", resolve(BASE_URI, ""));

        // Normal tests from rfc3986 http://tools.ietf.org/html/rfc3986#section-5.4.1

        Assert.assertEquals("g:h", resolve(BASE_URI, "g:h"));
        Assert.assertEquals("http://a/b/c/g", resolve(BASE_URI, "g"));
        Assert.assertEquals("http://a/b/c/g", resolve(BASE_URI, "./g"));
        Assert.assertEquals("http://a/b/c/g/", resolve(BASE_URI, "g/"));
        Assert.assertEquals("http://a/g", resolve(BASE_URI, "/g"));
        Assert.assertEquals("http://g", resolve(BASE_URI, "//g"));
        Assert.assertEquals("http://a/b/c/d;p?y", resolve(BASE_URI, "?y"));
        Assert.assertEquals("http://a/b/c/g?y", resolve(BASE_URI, "g?y"));
        Assert.assertEquals("http://a/b/c/d;p?q#s", resolve(BASE_URI, "#s"));
        Assert.assertEquals("http://a/b/c/g#s", resolve(BASE_URI, "g#s"));
        Assert.assertEquals("http://a/b/c/g?y#s", resolve(BASE_URI, "g?y#s"));
        Assert.assertEquals("http://a/b/c/;x", resolve(BASE_URI, ";x"));
        Assert.assertEquals("http://a/b/c/g;x", resolve(BASE_URI, "g;x"));
        Assert.assertEquals("http://a/b/c/g;x?y#s", resolve(BASE_URI, "g;x?y#s"));
//        Assert.assertEquals("http://a/b/c/", resolve(BASE_URI, "."));
        Assert.assertEquals("http://a/b/c/", resolve(BASE_URI, "./"));
//        Assert.assertEquals("http://a/b/", resolve(BASE_URI, ".."));
        Assert.assertEquals("http://a/b/", resolve(BASE_URI, "../"));
        Assert.assertEquals("http://a/b/g", resolve(BASE_URI, "../g"));
        Assert.assertEquals("http://a/", resolve(BASE_URI, "../.."));
        Assert.assertEquals("http://a/", resolve(BASE_URI, "../../"));
        Assert.assertEquals("http://a/g", resolve(BASE_URI, "../../g"));

        // Abnormal tests from rfc3986 http://tools.ietf.org/html/rfc3986#section-5.4.2

//        Assert.assertEquals("http://a/g", resolve(BASE_URI, "../../../g"));
//        Assert.assertEquals("http://a/g", resolve(BASE_URI, "../../../../g"));

//        Assert.assertEquals("http://a/g", resolve(BASE_URI, "/./g"));
//        Assert.assertEquals("http://a/g", resolve(BASE_URI, "/../g"));
//        Assert.assertEquals("http://a/b/c/g.", resolve(BASE_URI, "g."));
//        Assert.assertEquals("http://a/b/c/.g", resolve(BASE_URI, ".g"));
//        Assert.assertEquals("http://a/b/c/g..", resolve(BASE_URI, "g.."));
//        Assert.assertEquals("http://a/b/c/..g", resolve(BASE_URI, "..g"));

//        Assert.assertEquals("http://a/b/g", resolve(BASE_URI, "./../g"));
//        Assert.assertEquals("http://a/b/c/g/", resolve(BASE_URI, "./g/."));
//        Assert.assertEquals("http://a/b/c/g/h", resolve(BASE_URI, "g/./h"));
//        Assert.assertEquals("http://a/b/c/h", resolve(BASE_URI, "g/../h"));
//        Assert.assertEquals("http://a/b/c/g;x=1/y", resolve(BASE_URI, "g;x=1/./y"));
//        Assert.assertEquals("http://a/b/c/y", resolve(BASE_URI, "g;x=1/../y"));

//        Assert.assertEquals("http://a/b/c/g?y/./x", resolve(BASE_URI, "g?y/./x"));
//        Assert.assertEquals("http://a/b/c/g?y/../x", resolve(BASE_URI, "g?y/../x"));
//        Assert.assertEquals("http://a/b/c/g#s/./x", resolve(BASE_URI, "g#s/./x"));
//        Assert.assertEquals("http://a/b/c/g#s/../x", resolve(BASE_URI, "g#s/../x"));
    }

    @Test
    public void testJavaResolveBug() throws Exception {
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4666701
        Assert.assertEquals("http://vsdaria.com/dar/home.php", resolve("http://vsdaria.com", "dar/home.php"));
    }

    @Test
    public void testUberUrlParsing() throws Exception {
        String UBER_URL = "http://example.com/:@-._~!$&'()*+,=;:@-._~!$&'()*+,=:@-._~!$&'()*+,==?/?:@-._~!$'()*+,;=/?:@-._~!$'()*+,;==#/?:@-._~!$&'()*+,;=";
        URI2 uri2 = UriUtils.toUri(UBER_URL);
        Assert.assertEquals("http", uri2.getScheme());
        Assert.assertEquals("example.com", uri2.getHost());
        Assert.assertEquals("/:@-._~!$&'()*+,=;:@-._~!$&'()*+,=:@-._~!$&'()*+,==", uri2.getPath());
        Assert.assertEquals("/?:@-._~!$'()*+,;=/?:@-._~!$'()*+,;==", uri2.getQuery());
        Assert.assertEquals("/?:@-._~!$&'()*+,;=", uri2.getFragment());
    }

    @Test
    public void testsPseudoEncoded() {
        String test1 = "https://lenta.ru/привет%2Fпривет";
        URI2 res1 = UriUtils.toUri(test1);
        Assert.assertEquals("https://lenta.ru/%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82%252F%D0%BF%D1%80%D0%B8%D0%B2%D0%B5%D1%82", res1.toUriString());

        String test2 = "https://lenta.ru/hello%Сkitty";
        URI2 res2 = UriUtils.toUri(test2);
        Assert.assertEquals("https://lenta.ru/hello%25%D0%A1kitty", res2.toUriString());
    }

    private static String resolve(String base, String reference) {
        return UriUtils
                .resolveUri(UriUtils.toUri(base), UriUtils.toUri(reference, UriUtils.UriFeature.DO_NOT_NORMALIZE))
                .toUriString();
    }
}
