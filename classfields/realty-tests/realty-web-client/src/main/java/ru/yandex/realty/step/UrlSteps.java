package ru.yandex.realty.step;

import com.google.inject.Inject;
import io.qameta.allure.Step;
import lombok.Getter;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import ru.auto.tests.commons.extension.context.LocatorStorage;
import ru.auto.tests.commons.extension.context.StepContext;
import ru.auto.tests.commons.webdriver.WebDriverSteps;
import ru.lanwen.diff.uri.core.filters.UriDiffFilter;
import ru.yandex.realty.config.RealtyWebConfig;
import ru.yandex.realty.rules.MockRuleConfigurable;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static ru.lanwen.diff.uri.core.filters.AnyParamValueFilter.param;
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Pages.ZASTROYSCHIK;
import static ru.yandex.realty.matchers.UrlMatcher.hasNoDiffWithUrl;
import static ru.yandex.realty.matchers.WaitForMatcherDecorator.withWaitFor;

/**
 * @author kurau (Yuri Kalinin)
 */
public class UrlSteps extends WebDriverSteps {

    public static final String REAL_PRODUCTION = "https://realty.yandex.ru";
    public static final String COOKIE_DOMAIN = ".yandex.";
    public static final String COOKIE_TEST_DOMAIN = ".realty.test.vertis.yandex";
    public static final String RGID = "rgid";
    public static final String FROM_FORM_VALUE = "from-form";
    public static final String ACTIVATION_VALUE = "activation";
    public static final String TRAP_VALUE = "trap";
    public static final String TRUE_VALUE = "true";
    public static final String EDIT_VALUE = "edit";
    public static final String FALSE_VALUE = "false";
    public static final String PAYMENT_ID_VALUE = "paymentId";
    public static final String CATEGORY_URL_PARAM = "category";
    public static final String HOUSE_URL_PARAM = "HOUSE";
    public static final String OBJECT_TYPE_URL_PARAM = "objectType";
    public static final String VILLAGE_URL_PARAM = "VILLAGE";
    public static final String COMMERCIAL_URL_PARAM = "COMMERCIAL";
    public static final String APARTMENT_URL_PARAM = "APARTMENT";
    public static final String TYPE_URL_PARAM = "type";
    public static final String SELL_URL_PARAM = "SELL";
    public static final String RENT_URL_PARAM = "RENT";
    public static final String SORT_URL_PARAM = "sort";
    public static final String RELEVANCE_SORT_VALUE = "RELEVANCE";
    public static final String PRICE_SORT_VALUE = "PRICE";
    public static final String COMMISSIONING_DATE_SORT_VALUE = "COMMISSIONING_DATE";
    public static final String COMMERCIAL_TYPE_URL_PARAM = "commercialType";
    public static final String OFFICE_URL_PARAM = "OFFICE";
    public static final String RETAIL_URL_PARAM = "RETAIL";
    public static final String PAGE_URL_PARAM = "page";
    public static final String PAGE_SIZE_URL_PARAM = "pageSize";
    public static final String PRICE_MIN_URL_PARAM = "priceMin";
    public static final String PRICE_MAX_URL_PARAM = "priceMax";
    public static final String AREA_MIN_URL_PARAM = "areaMin";
    public static final String AREA_MAX_URL_PARAM = "areaMax";
    public static final String ROOMS_TOTAL_URL_PARAM = "roomsTotal";
    public static final String SHOW_SIMILAR_URL_PARAM = "showSimilar";
    public static final String IS_EXACT_URL_PARAM = "isExact";
    public static final String NEW_FLAT_URL_PARAM = "newFlat";
    public static final String LAYER_URL_PARAM = "layer";
    public static final String YES_VALUE = "YES";
    public static final String NO_VALUE = "NO";
    public static final String CATEGORY_CODE_URL_PARAM = "categoryCode";
    public static final String KVARTIRA_VALUE = "kvartira";
    public static final String TAB_URL_PARAM = "tab";
    public static final String SEARCH_TAB_VALUE = "search";
    public static final String NEWBUILDING_TAB_VALUE = "newbuilding";
    public static final String PRICE_TAB_VALUE = "price";
    public static final String DEVELOPER_ID_URL_PARAM = "developerId";
    public static final String SITE_NAME = "siteName";
    public static final String SITE_ID = "siteId";
    public static final String AMP_PARAMETER = "_gl";
    public static final String ID_VALUE = "id";
    public static final String FROM_PARAM = "from";
    public static final String MAIN_MENU_VALUE = "main_menu";
    public static final String FAVORITE_TYPE_PARAM = "favoriteType";
    public static final String SITE_URL_VALUE = "SITE";
    public static final String VILLAGE_URL_VALUE = "VILLAGE";
    public static final String NEWBUILDING_DEV_OBJECTS_VALUE = "/kupit/novostrojka/";
    public static final String FROM_SPECIAL = "from-special";
    public static final String SAMOLET_VALUE = "samolet";
    public static final String NB_WITH_FLATS_PATH = "/sputnik-386079/";
    public static final String NB_MOCK_PATH = format("/valerinskij-gorod-%s/", MockRuleConfigurable.NB_ID);
    public static final String MOSCOW_RGID = "587795";
    public static final String MO_RGID = "741964";
    public static final String OMSK_RGID = "176912";
    public static final String SPB_I_LO_RGID = "741965";
    public static final String SPB_RGID = "417899";
    public static final String ID_SITE = "1705565";
    public static final String REDIRECT_FROM_RGID = "redirect_from_rgid";
    public static final String STREET_ID_PARAM = "streetId";
    public static final String STREET_NAME_PARAM = "streetName";
    public static final String SOURCE = "source";
    public static final String PINNED_OFFER_ID_PARAM = "pinnedOfferId";

    @Inject
    @Getter
    private RealtyWebConfig config;

    @Inject
    private LocatorStorage locatorStorage;

    private UriBuilder uriBuilder;

    private List<UriDiffFilter> ignoringParams = newArrayList();


    public String session() {
        return ((RemoteWebDriver) getDriver()).getSessionId().toString();
    }

    public UrlSteps fromUri(String uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        return this;
    }

    public UrlSteps testing() {
        uriBuilder = UriBuilder.fromUri(config.getTestingURI());
        return this;
    }

    public UrlSteps production() {
        uriBuilder = UriBuilder.fromUri(config.getProductionURI());
        return this;
    }

    public String getMobileTesting() {
        return uriBuilderWithMobileHost(config.getTestingURI()).toString();
    }

    public void setSpbCookie() {
        setCookie(RGID, SPB_RGID, COOKIE_DOMAIN + "ru");
    }

    public void setSpbCookieTest() {
        setCookie(RGID, SPB_RGID, COOKIE_TEST_DOMAIN);
    }

    public void setMoscowCookie() {
        setCookie(RGID, MOSCOW_RGID, COOKIE_DOMAIN + "ru");
    }

    public void setMoscowCookieTest() {
        setCookie(RGID, MOSCOW_RGID, COOKIE_TEST_DOMAIN);
    }

    public void setRegionCookie() {
        setCookie(RGID, OMSK_RGID, COOKIE_DOMAIN + "ru");
    }

    public void setRegionCookieTest() {
        setCookie(RGID, OMSK_RGID, COOKIE_TEST_DOMAIN + "ru");
    }

    public UrlSteps login() {
        uriBuilder = UriBuilder.fromUri("http://aqua.yandex-team.ru/auth.html");
        return this;
    }

    public UrlSteps logout() {
        uriBuilder = UriBuilder.fromUri("https://passport.yandex.ru/passport");
        return this;
    }

    public void ignoreAppBannerOnStart() {
        setCookie("splash_banner_closed", "1", ".yandex.ru");
    }


    public String getMobileProduction() {
        return uriBuilderWithMobileHost(config.getProductionURI()).toString();
    }

    public UrlSteps path(String path) {
        uriBuilder.path(path);
        return this;
    }

    public UrlSteps replacePath(String path) {
        uriBuilder.replacePath(path);
        return this;
    }

    public UrlSteps uri(String resource) {
        uriBuilder.uri(resource);
        return this;
    }

    public UrlSteps setProductionHost() {
        uriBuilder.host(config.getProductionURI().getHost());
        return this;
    }

    public UrlSteps setMobileProductionHost() {
        uriBuilder.host(config.getProductionURI().getHost().replace("realty.", "m.realty."));
        return this;
    }

    @Step("Добавляем параметр {name} = {values} к билдеру")
    public UrlSteps queryParam(String name, Object... values) {
        uriBuilder.queryParam(name, values);
        return this;
    }

    @Step("Меняем параметр {name} = {value}")
    public UrlSteps replaceQueryParam(String name, String value) {
        uriBuilder.replaceQueryParam(name, value);
        return this;
    }

    @Step("Добавляем фрагмент {name} к билдеру")
    public UrlSteps fragment(String name) {
        uriBuilder.fragment(name);
        return this;
    }

    @Step("Добавляем параметр {name} к списку игнорируемых")
    public UrlSteps ignoreParam(String name) {
        ignoringParams.add(param(name));
        return this;
    }

    @Step("Добавляем параметр {name} к списку игнорируемых")
    public UrlSteps ignoreParams(String... names) {
        for (String name : names) {
            ignoringParams.add(param(name));
        }
        return this;
    }

    @Step("Добавляем параметры координат карты к списку игнорируемых")
    public UrlSteps ignoreMapCoordinate() {
        ignoringParams.add(param("leftLongitude"));
        ignoringParams.add(param("rightLongitude"));
        ignoringParams.add(param("bottomLatitude"));
        ignoringParams.add(param("topLatitude"));
        return this;
    }

    public UrlSteps open() {
        open(uriBuilder.build().toString());
        return this;
    }

    @Step("Open url «{url}»")
    public void open(String url) {
        try {
            locatorStorage.getStepsList()
                    .add(new StepContext().setDescription("Переходим на " + url)
                            .setAction("openUrl"));
        } catch (Exception e) {
            //
        }
        getDriver().manage().timeouts().pageLoadTimeout(45, TimeUnit.SECONDS);
        try {
            getDriver().get(url);
        } catch (org.openqa.selenium.TimeoutException e) {
            stopLoadingPage(url);
        }
    }

    @Step("СТРАНИЦА «{url}» НЕ ЗАГРУЗИЛАСЬ ДО КОНЦА. ОСТАНАВЛИВАЕМ КРЕСТИКОМ В БРАУЗЕРЕ")
    private void stopLoadingPage(String url) {
        ((JavascriptExecutor) getDriver()).executeScript("return window.stop");
    }

    @Step("Должны быть на странице «{url}»")
    public UrlSteps shouldNotDiffWith(String url) {
        try {
            locatorStorage.getStepsList()
                    .add(new StepContext().setAction("urlMatcher").setDescription(hasNoDiffWithUrl(url, ignoringParams).toString()));
        } catch (Exception e) {
            //
        }
        assertThat(getDriver(), withWaitFor(hasNoDiffWithUrl(url, ignoringParams), SECONDS.toMillis(getUrlTimeout())));
        return this;
    }

    public UrlSteps shouldNotDiffWithWebDriverUrl() {
        shouldNotDiffWith(uriBuilder.build().toString());
        return this;
    }

    public void shouldCurrentUrlContains(String expected) {
        assertThat(getCurrentUrl(), withWaitFor(containsString(expected)));
    }

    public void shouldCurrentUrlContains(String expected, int timeout) {
        assertThat(getCurrentUrl(), withWaitFor(containsString(expected), SECONDS.toMillis(timeout)));
    }

    public UrlSteps newbuildingSite() {
        return path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path(NB_WITH_FLATS_PATH);
    }

    public UrlSteps newbuildingSiteMock() {
        return path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(format("/valerinskij-gorod-%s/", MockRuleConfigurable.NB_ID));
    }

    public UrlSteps newbuildingSiteMockWithCallback() {
        return path(SPB_I_LO).path(KUPIT).path(NOVOSTROJKA).path(format("/valo-395435/"));
    }

    public UrlSteps newbuildingSiteWithChats() {
        return path(SANKT_PETERBURG).path(KUPIT).path(NOVOSTROJKA).path("/capital-towers-872687/");
    }

    public UrlSteps villageSite() {
        return path("/angelovo/").path(KUPIT).path(KOTTEDZHNYE_POSELKI).path("/angelovo/").queryParam("id", "1927508");
    }

    public UrlSteps newbuildingSiteMobile() {
        return path(MOSKVA_I_MO).path(KUPIT).path(NOVOSTROJKA).path("headliner-401524/");
    }

    public UrlSteps villageCardMobile() {
        return path("/moskva/").path(KUPIT).path(KOTTEDZHNYE_POSELKI).path("/poltevo-forest/")
                .queryParam("id", "1774146");
    }

    public UrlSteps villageFiltersMobile() {
        return queryParam(CATEGORY_URL_PARAM, HOUSE_URL_PARAM).queryParam(OBJECT_TYPE_URL_PARAM, VILLAGE_URL_PARAM)
                .queryParam(TYPE_URL_PARAM, SELL_URL_PARAM);
    }

    public UrlSteps developerPath(String name, String id) {
        return path(ZASTROYSCHIK).path(format("%s-%s/", name, id));
    }

    @Step("Получаем текущий url")
    public String getCurrentUrl() {
        return getDriver().getCurrentUrl();
    }

    public String toString() {
        return uriBuilder.build().toString();
    }

    private UriBuilder uriBuilderWithMobileHost(URI uri) {
        uriBuilder = UriBuilder.fromUri(uri);
        return uriBuilder.host(uri.getHost().replace("realty.", "realty."));
    }

    public int getUrlTimeout() {
        return config.getUrlTimeout();
    }
}
