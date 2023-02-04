package ru.yandex.realty.specproject.filters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.Pages.CATALOG;
import static ru.yandex.realty.consts.Pages.SAMOLET;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.step.CommonSteps.FIRST;
import static ru.yandex.realty.step.UrlSteps.AREA_MIN_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.PRICE_MAX_URL_PARAM;
import static ru.yandex.realty.utils.UtilsWeb.getNormalArea;

@DisplayName("Спецпроект. Фильтры")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class SpecProjectFiltersTest {

    private static final String MAP = "/map/";
    private static final String SEE_ON_MAP = "На карте";
    private static final String OFFER_PATH = "/lyubercy-1839196/";
    private static final String DELIVERY_DATE = "deliveryDate";
    private static final String DVUHKOMNATNAYA = "/dvuhkomnatnaya/";
    private static final String PRICE_VALUE = "20000000";
    private static final String AREA_VALUE = "40";
    private static final String DELIVERY_DATE_VALUE = "4_2023";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры открываются")
    public void shouldSeeFilters() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        basePageSteps.onSpecProjectPage().filters().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Фильтры закрываются")
    public void shouldNotSeeFilters() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        basePageSteps.onSpecProjectPage().filters().closeButton().click();
        basePageSteps.onSpecProjectPage().filters().waitUntil(not(isDisplayed()));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Санкт-Петербург и ЛО»")
    public void shouldSeeSpbRegion() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        basePageSteps.onSpecProjectPage().filters().button("Санкт-Петербург и ЛО").click();
        basePageSteps.onSpecProjectPage().filters().submitButton().click();
        urlSteps.testing().path(SPB_I_LO).path(SAMOLET).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onSpecProjectPage().showFiltersButton().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Кнопка «Москва и МО»")
    public void shouldSeeMoscowRegion() {
        urlSteps.testing().path(SPB_I_LO).path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        basePageSteps.onSpecProjectPage().filters().button("Москва и МО").click();
        basePageSteps.onSpecProjectPage().filters().submitButton().click();
        urlSteps.testing().path(SAMOLET).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onSpecProjectPage().showFiltersButton().should(isDisplayed());
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Если фильтр площади «от» больше чем «до» то применяется только последний")
    public void shouldSeeLastAreaFilter() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        int areaInt = getNormalArea();
        String area = String.valueOf(areaInt + 1);
        String areaLast = String.valueOf(areaInt);
        basePageSteps.onSpecProjectPage().filters().areaFrom().sendKeys(area);
        basePageSteps.onSpecProjectPage().filters().areaFrom().waitUntil(hasValue(area));
        basePageSteps.onSpecProjectPage().filters().areaTo().click();
        basePageSteps.onSpecProjectPage().filters().areaTo().sendKeys(areaLast);
        basePageSteps.onSpecProjectPage().filters().areaTo().waitUntil(hasValue(areaLast));
        basePageSteps.onSpecProjectPage().filters().areaFrom().waitUntil(hasValue(""));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Если фильтр цены «до» меньше чем «от» то применяется только последний")
    public void shouldSeeLastPriceFilter() {
        urlSteps.testing().path(SAMOLET).open();
        basePageSteps.onSpecProjectPage().showFiltersButton().click();
        int priceInt = getNormalArea();
        String price = String.valueOf(priceInt - 1);
        String priceLast = String.valueOf(priceInt);
        basePageSteps.onSpecProjectPage().filters().priceTo().sendKeys(price);
        basePageSteps.onSpecProjectPage().filters().priceTo().waitUntil(hasValue(price));
        basePageSteps.onSpecProjectPage().filters().priceFrom().click();
        basePageSteps.onSpecProjectPage().filters().priceFrom().sendKeys(priceLast);
        basePageSteps.onSpecProjectPage().filters().priceFrom().waitUntil(hasValue(priceLast));
        basePageSteps.onSpecProjectPage().filters().priceTo().waitUntil(hasValue(""));
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Смотреть на карте»")
    public void shouldSeeMapInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        basePageSteps.onSpecProjectPage().button(SEE_ON_MAP).click();
        urlSteps.path(MAP).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Показать списком»")
    public void shouldNotSeeMapInUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).open();
        basePageSteps.onSpecProjectPage().onList().click();
        urlSteps.testing().path(SAMOLET).path(CATALOG).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик «Смотреть на карте» фильтры сохраняются")
    public void shouldSeeFiltersOnMapUrl() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(DVUHKOMNATNAYA)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE_VALUE).queryParam(AREA_MIN_URL_PARAM, AREA_VALUE)
                .queryParam(DELIVERY_DATE, DELIVERY_DATE_VALUE)
                .open();
        basePageSteps.onSpecProjectPage().button(SEE_ON_MAP).click();
        urlSteps.testing().path(SAMOLET).path(CATALOG).path(MAP).path(DVUHKOMNATNAYA)
                .queryParam(PRICE_MAX_URL_PARAM, PRICE_VALUE).queryParam(AREA_MIN_URL_PARAM, AREA_VALUE)
                .queryParam(DELIVERY_DATE, DELIVERY_DATE_VALUE)
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Клик на оффер")
    @Issue("https://st.yandex-team.ru/REALTYFRONT-13311")
    public void shouldSeeOfferClick() {
        urlSteps.testing().path(SAMOLET).path(CATALOG).open();
        basePageSteps.onSpecProjectPage().offer(FIRST).offerLink().click();
        basePageSteps.waitUntilSeeTabsCount(2);
        basePageSteps.switchToNextTab();
        urlSteps.testing().path("/moskva_i_moskovskaya_oblast/").path(KUPIT).path(NOVOSTROJKA).path(OFFER_PATH)
                .queryParam(UrlSteps.FROM_SPECIAL, UrlSteps.SAMOLET_VALUE)
                .ignoreParam(UrlSteps.SOURCE)
                .shouldNotDiffWithWebDriverUrl();
    }
}
