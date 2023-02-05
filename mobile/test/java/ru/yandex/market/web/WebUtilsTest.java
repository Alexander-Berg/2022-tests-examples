package ru.yandex.market.web;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.BaseTest;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.web.WebUtils.SEPARATOR;
import static ru.yandex.market.web.WebUtils.createWebUrl;
import static ru.yandex.market.web.WebUtils.isSamePaths;
import static ru.yandex.market.web.WebUtils.isTrustedWebViewUrl;
import static ru.yandex.market.web.WebUtils.isWebUrl;

public class WebUtilsTest extends BaseTest{

    @Test
    public void testNullWebUrl() {
        assertFalse(isWebUrl(null));
        assertFalse(isWebUrl(""));
    }

    @Test
    public void testIsWeb() {
        assertTrue(isWebUrl("https://yandex.ru/asdasd"));
        assertTrue(isWebUrl("http://1234-market.yandex.ru/asdasd"));
        assertFalse(isWebUrl("ftp://1234-market.yandex.ru/asdasd"));
        assertFalse(isWebUrl("/asdasd/asd"));
        assertFalse(isWebUrl("xxxhttps://yandex.ru/asdasd"));
    }

    @Test
    public void testCreateWebUrlWithNullPath() {
        String host = "https://yandex.ru";
        assertThat(createWebUrl(host, null), equalTo(host));
        assertThat(createWebUrl(host, ""), equalTo(host));
    }

    @Test
    public void testCreateWebUrl() {
        String host = "https://yandex.ru";
        String path = "path/to/resource";
        String expected = host + SEPARATOR + path;

        assertThat(createWebUrl(host, path), equalTo(expected));
        assertThat(createWebUrl(host + SEPARATOR, path), equalTo(expected));
        assertThat(createWebUrl(host, SEPARATOR + path), equalTo(expected));
        assertThat(createWebUrl(host + SEPARATOR, SEPARATOR + path), equalTo(expected));
    }

    @Test
    public void testIsTrustedUrl() {
        assertTrue(isTrustedWebViewUrl("http://yandex.ru"));
        assertTrue(isTrustedWebViewUrl("https://yandex.ru/home"));
        assertTrue(isTrustedWebViewUrl("http://m.yandex.ru/catalog"));
        assertTrue(isTrustedWebViewUrl("https://m.yandex.ru/"));
        assertTrue(isTrustedWebViewUrl("https://full-touch.market-exp-touch.pepelac01ht.yandex.ru"));
        assertTrue(isTrustedWebViewUrl("http://beru.ru"));
        assertTrue(isTrustedWebViewUrl("https://beru.ru/home"));
        assertTrue(isTrustedWebViewUrl("http://m.beru.ru/catalog"));
        assertTrue(isTrustedWebViewUrl("https://m.beru.ru/"));
        assertTrue(isTrustedWebViewUrl("https://pass.beru.ru/"));

        Assert.assertFalse(isTrustedWebViewUrl("https://yandex.spb.ru/"));
        Assert.assertFalse(isTrustedWebViewUrl("https://yandex.club/"));
        Assert.assertFalse(
                isTrustedWebViewUrl("https://full-touch.market-exp-touch.pepelac01ht.ru/yandex"));
        Assert.assertFalse(isTrustedWebViewUrl("https://home.ru?yandex=1"));
        Assert.assertFalse(isTrustedWebViewUrl("http://home.ru?auth=false&yandex=1"));
        Assert.assertFalse(isTrustedWebViewUrl("http://yandex.home.ru"));
        Assert.assertFalse(isTrustedWebViewUrl("http://am.yandex.home.ru"));
        Assert.assertFalse(isTrustedWebViewUrl("https://full-touch.market-exp-touch.pepelac01ht.ru/beru"));
        Assert.assertFalse(isTrustedWebViewUrl("https://home.ru?beru=1"));
        Assert.assertFalse(isTrustedWebViewUrl("http://home.ru?auth=false&beru=1"));
        Assert.assertFalse(isTrustedWebViewUrl("http://beru.home.ru"));
        Assert.assertFalse(isTrustedWebViewUrl("http://am.beru.home.ru"));
        Assert.assertFalse(isTrustedWebViewUrl("http://myandex.ru"));
    }

    @Test
    public void testSamePaths() {
        assertTrue(isSamePaths("", "/"));
        assertTrue(isSamePaths("/", ""));
        assertTrue(isSamePaths("/", "/"));
        assertTrue(isSamePaths("asd", "/asd/"));
        assertFalse(isSamePaths("asda", "/asd"));
    }
}