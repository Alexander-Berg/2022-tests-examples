package com.yandex.launcher.search;

import com.yandex.launcher.common.util.TextUtils;
import com.yandex.launcher.BaseRobolectricTest;
import com.yandex.launcher.search.providers.YandexSearchProvider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class YandexSearchProviderTest extends BaseRobolectricTest {

    private Map<String, String> redirectCountriesMainDomain;
    private Map<String, String> redirectCountriesClckDomain;

    private static final String APP_ID = YandexSearchProvider.PARAM_APP_ID;

    public YandexSearchProviderTest() throws NoSuchFieldException, IllegalAccessException {
    }

    //region -------------------------------- Self test

    @Test
    public void testSelf() throws UnsupportedEncodingException {

        Assert.assertEquals(
                "http://yandex.com.tr/search/touch?text=vasia&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "tr", null, null)
        );

        Assert.assertEquals(
                "http://yandex.ru/search/touch?text=vasia&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "ru", null, null)
        );

        Assert.assertEquals(
                "http://yandex.ru/search/touch?text=vasia&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "666", null, null)
        );

        Assert.assertEquals(
                "http://yandex.ru/search/touch?text=vasia&app_id=" + APP_ID,
                getExpectedUrlString("vasia", null, null, null)
        );

        Assert.assertEquals(
                "http://yandex.ru/search/touch?text=vasia-ololoev&clid=asd12&app_id=" + APP_ID,
                getExpectedUrlString("vasia-ololoev", "ru", "asd12", null)
        );

        Assert.assertEquals(
                "http://yandex.com.tr/search/touch?text=ololo123&clid=qwe&app_id=" + APP_ID,
                getExpectedUrlString("ololo123", "tr", "qwe", null)
        );

        Assert.assertEquals(
                "http://yandex.com.tr/search/touch?text=ololo123&app_id=" + APP_ID,
                getExpectedUrlString("ololo123", "tr", "", null)
        );

        Assert.assertEquals(
                "http://clck.yandex.ru/redir/uuid=rty/*http://yandex.ru/search/touch?text=vasia&clid=11a&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "ru", "11a", "rty")
        );

        Assert.assertEquals(
                "http://clck.yandex.com.tr/redir/uuid=rty/*http://yandex.com.tr/search/touch?text=vasia&clid=11a&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "tr", "11a", "rty")
        );

        Assert.assertEquals(
                "http://clck.yandex.com.tr/redir/uuid=rty/*http://yandex.com.tr/search/touch?text=vasia&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "tr", "", "rty")
        );

        Assert.assertEquals(
                "http://yandex.com.tr/search/touch?text=vasia&app_id=" + APP_ID,
                getExpectedUrlString("vasia", "tr", "", "")
        );

    }

    //endregion

    //region -------------------------------- Actual tests

    @Test
    public void testSimpleSearchUris() throws UnsupportedEncodingException {

        Assert.assertEquals(
                getExpectedUrlString("vasia", "en", null, null),
                YandexSearchProvider.getSearchUri("vasia", "en", null, null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "de", null, null),
                YandexSearchProvider.getSearchUri("vasia", "de", null, null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "tr", null, null),
                YandexSearchProvider.getSearchUri("vasia", "tr", null, null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "ru", null, null),
                YandexSearchProvider.getSearchUri("vasia", "ru", null, null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "666", null, null),
                YandexSearchProvider.getSearchUri("vasia", "666", null, null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", null, null, null),
                YandexSearchProvider.getSearchUri("vasia", null, null, null).toString()
        );

    }

    @Test
    public void testClidsSearchUris() throws UnsupportedEncodingException {

        Assert.assertEquals(
                getExpectedUrlString("vasia", "en", "qwe", null),
                YandexSearchProvider.getSearchUri("vasia", "en", "qwe", null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia_ololoev", "ru", "asd12", null),
                YandexSearchProvider.getSearchUri("vasia_ololoev", "ru", "asd12", null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "en", "qwe", null),
                YandexSearchProvider.getSearchUri("vasia", "en", "qwe", null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("ololo123", "tr", "qwe", null),
                YandexSearchProvider.getSearchUri("ololo123", "tr", "qwe", null).toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("ololo123", "tr", "", null),
                YandexSearchProvider.getSearchUri("ololo123", "tr", "", null).toString()
        );
    }

    @Test
    public void testClickRedirectSearchUris() throws UnsupportedEncodingException {

        Assert.assertEquals(
                getExpectedUrlString("vasia", "en", "qwe", "rty"),
                YandexSearchProvider.getSearchUri("vasia", "en", "qwe", "rty").toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "ru", "", "rty"),
                YandexSearchProvider.getSearchUri("vasia", "ru", "", "rty").toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "ru", "11a", "rty"),
                YandexSearchProvider.getSearchUri("vasia", "ru", "11a", "rty").toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "tr", "11a", "rty"),
                YandexSearchProvider.getSearchUri("vasia", "tr", "11a", "rty").toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "tr", "", "rty"),
                YandexSearchProvider.getSearchUri("vasia", "tr", "", "rty").toString()
        );

        Assert.assertEquals(
                getExpectedUrlString("vasia", "tr", "", ""),
                YandexSearchProvider.getSearchUri("vasia", "tr", "", "").toString()
        );

    }

    //endregion

    //region -------------------------------- Expected values methods

    @Before
    public void prepare() {
        redirectCountriesClckDomain = new HashMap<>();
        redirectCountriesClckDomain.put("ru", "clck.yandex.ru");
        redirectCountriesClckDomain.put("tr", "clck.yandex.com.tr");

        redirectCountriesMainDomain = new HashMap<>();
        redirectCountriesMainDomain.put("tr", "yandex.com.tr");
    }

    private String getExpectedUrlString(String query, String country, String clid, String uuid) throws UnsupportedEncodingException {
        String baseUrl = String.format("http://%s/search/touch?text=%s%s&app_id=" + APP_ID,
                getMainDomain(country), query,
                !TextUtils.isEmpty(clid) ? "&clid="+clid : ""
        );

        String redirectDomain = getRedirectDomain(country);
        if (redirectDomain != null && !TextUtils.isEmpty(uuid)) {
            baseUrl = String.format(
                "http://%s/redir/uuid=%s/*%s",
                getRedirectDomain(country), uuid, baseUrl
            );
        }

        return baseUrl;
    }

    private String getRedirectDomain(String country) {
        return redirectCountriesClckDomain.get(country);
    }

    private String getMainDomain(String country) {
        String domain = redirectCountriesMainDomain.get(country);
        if (domain == null) {
            domain = "yandex.ru";
        }
        return domain;
    }

    //endregion

}
