package ru.yandex.wmconsole.service;

import java.io.StringReader;
import java.net.URL;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * User: azakharov
 * Date: 03.05.12
 * Time: 17:45
 */
public class DelUrlServiceTest {

    public static final String HTML_WITH_NONE_META_TAG =
            "<html> " +
            "    <head> " +
            "        <meta name=\"robots\" content=\"none\"/> " +
            "    </head> " +
            "    <body> " +
            "    This text is not for search engine robot! " +
            "    </body> " +
            "</html>";

    public static final String HTML_WITH_NOINDEX_META_TAG =
            "<html> " +
                    "    <head> " +
                    "        <meta name=\"robots\" content=\"noindex\"/> " +
                    "    </head> " +
                    "    <body> " +
                    "    This text is not for search engine robot! " +
                    "    </body> " +
                    "</html>";

    public static final String HTML_WITH_NONE_AND_NOINDEX_META_TAG =
            "<html> " +
                    "    <head> " +
                    "        <meta name=\"robots\" content=\"none\"/> " +
                    "        <meta name=\"robots\" content=\"noindex\"/> " +
                    "    </head> " +
                    "    <body> " +
                    "    This text is not for search engine robot! " +
                    "    </body> " +
                    "</html>";

    public static final String HTML_WITHOUT_META_TAG =
            "<html> " +
                    "    <head> " +
                    "    </head> " +
                    "    <body> " +
                    "    This text is not for search engine robot! " +
                    "    </body> " +
                    "</html>";
    @Test
    public void testDelUrlWithNoneMetaTag() throws Exception {
        DelUrlService delUrlService = new DelUrlService();
        URL url = new URL("http://test.ru/test.html");
        assertTrue(delUrlService.checkMetaTag(new StringReader(HTML_WITH_NONE_META_TAG)));
    }

    @Test
    public void testDelUrlWithNoindexMetaTag() throws Exception {
        DelUrlService delUrlService = new DelUrlService();
        URL url = new URL("http://test.ru/test.html");
        assertTrue(delUrlService.checkMetaTag(new StringReader(HTML_WITH_NOINDEX_META_TAG)));
    }

    @Test
    public void testDelUrlWithNoneAndNoindexMetaTag() throws Exception {
        DelUrlService delUrlService = new DelUrlService();
        URL url = new URL("http://test.ru/test.html");
        assertTrue(delUrlService.checkMetaTag(new StringReader(HTML_WITH_NONE_AND_NOINDEX_META_TAG)));
    }

    @Test
    public void testDelUrlWithOutMetaTag() throws Exception {
        DelUrlService delUrlService = new DelUrlService();
        URL url = new URL("http://test.ru/test.html");
        assertFalse(delUrlService.checkMetaTag(new StringReader(HTML_WITHOUT_META_TAG)));
    }
}
