package ru.yandex.market.web;

import org.junit.Test;

import ru.yandex.market.BaseTest;
import ru.yandex.market.data.deeplinks.DeeplinkUtils;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;

public class MarketWebUtilsTest extends BaseTest {

    private static final String WHITE_TOUCH = MarketWebUtils.WHITE_TOUCH_URL + "/product/1234/";

    @Test
    public void webUrlToMarketWebUrl() {
        String url = "https://market.yandex.ru/product/1234";
        assertEquals(url, MarketWebUtils.toMarketWebUrl(url));

        url = WHITE_TOUCH;
        assertEquals(url, MarketWebUtils.toMarketWebUrl(url));

        url = DeeplinkUtils.SCHEME_BERU + "://product/1234/";
        assertEquals(WHITE_TOUCH, MarketWebUtils.toMarketWebUrl(url));

        url = "//product/1234/";
        assertEquals(WHITE_TOUCH, MarketWebUtils.toMarketWebUrl(url));

        url = "product/1234/";
        assertEquals(WHITE_TOUCH, MarketWebUtils.toMarketWebUrl(url));
    }

    @Test
    public void nonMarketUrlToMarketWebUrl() {
        String url = "https://trololo.com/test/url";
        assertNull(MarketWebUtils.toMarketWebUrl(url));

        url = "makemehappy://trololo.com/test/url";
        assertNull(MarketWebUtils.toMarketWebUrl(url));
    }
}