package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Link;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.consts.Filters;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static java.lang.String.format;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.DOM_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;
import static ru.yandex.realty.mobile.page.SaleAdsPage.PARAMETERS;
import static ru.yandex.realty.step.UrlSteps.APARTMENT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.MOSCOW_RGID;
import static ru.yandex.realty.step.UrlSteps.NEW_FLAT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SELL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.SPB_RGID;
import static ru.yandex.realty.step.UrlSteps.STREET_ID_PARAM;
import static ru.yandex.realty.step.UrlSteps.STREET_NAME_PARAM;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.YES_VALUE;

@Link("https://st.yandex-team.ru/VERTISTEST-2072")
@DisplayName("Базовые фильтры. Смена региона в фильтрах")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseLocationStreetRegionChangeFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем, что фильтр по улице не сбрасывается")
    public void shouldSeeStreetAfterApplyFilter() {
        final String testStreet = "улица Цюрупы";
        final String testStreetId = "55034";
        final String testStreetPath = format("/st-ulica-cyurupy-%s/", testStreetId);
        final String testStreetNameParam = "ulica-cyurupy";

        urlSteps.testing().path(MOSKVA_I_MO).open();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(testStreet);
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(testStreet));
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(testStreetPath).shouldNotDiffWithWebDriverUrl();

        basePageSteps.onMobileSaleAdsPage().showFiltersButton().click();
        urlSteps.testing().path(Filters.FILTERS).queryParam(STREET_ID_PARAM, testStreetId)
                .queryParam(CATEGORY_URL_PARAM, APARTMENT_URL_PARAM)
                .queryParam(RGID, MOSCOW_RGID)
                .queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .queryParam(STREET_NAME_PARAM, testStreetNameParam).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Issue("https://st.yandex-team.ru/VTF-1682")
    @Issue("https://st.yandex-team.ru/REALTYFRONT-13784")
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем, что регион поиска изменился и применился фильтр по улице")
    public void shouldSeeStreetAfterApplyFilterAndChangeRegion() {
        final String testStreet = "Краснопресненская набережная";
        final String testStreetPath = "/st-krasnopresnenskaya-naberezhnaya-185280/";

        urlSteps.testing().path(SANKT_PETERBURG).open();
        basePageSteps.onMobileMainPage().searchFilters().newFlat().button("Новостройки").click();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(testStreet);
        basePageSteps.moveCursorAndClick(basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(testStreet));
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        basePageSteps.onMobileMainPage().modal().button("Да").click();
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(NOVOSTROJKA).path(testStreetPath)
                .queryParam(NEW_FLAT_URL_PARAM, YES_VALUE).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KANTEMIROV)
    @DisplayName("Проверяем, что регион поиска НЕ изменился и применился фильтр по улице")
    public void shouldSeeStreetAfterApplyFilterAndNotChangeRegion() {
        final String testStreet = "Краснопресненская набережная";

        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().showFiltersButton().click();
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(testStreet);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(testStreet).click();
        basePageSteps.onMobileMainPage().modal().button("Нет").click();
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(KVARTIRA).shouldNotDiffWithWebDriverUrl();

        basePageSteps.onMobileSaleAdsPage().showFiltersButton().click();
        urlSteps.testing().path(Filters.FILTERS)
                .queryParam(CATEGORY_URL_PARAM, APARTMENT_URL_PARAM)
                .queryParam(RGID, SPB_RGID)
                .queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .shouldNotDiffWithWebDriverUrl();
    }
}
