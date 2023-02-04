package ru.yandex.realty.listing;

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

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.ALEKS_IVANOV;
import static ru.yandex.realty.consts.Pages.FILTERS;
import static ru.yandex.realty.consts.Pages.KARTA;
import static ru.yandex.realty.consts.RealtyFeatures.LISTING;
import static ru.yandex.realty.mobile.page.SaleAdsPage.MAP;
import static ru.yandex.realty.mobile.page.SaleAdsPage.PARAMETERS;
import static ru.yandex.realty.mobile.page.SaleAdsPage.PIXELS_TO_FLOAT_NAV_BAR;
import static ru.yandex.realty.step.UrlSteps.APARTMENT_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.CATEGORY_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.MOSCOW_RGID;
import static ru.yandex.realty.step.UrlSteps.RGID;
import static ru.yandex.realty.step.UrlSteps.SELL_URL_PARAM;
import static ru.yandex.realty.step.UrlSteps.TYPE_URL_PARAM;

@Issue("VERTISTEST-1352")
@Feature(LISTING)
@DisplayName("Навбар")
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class NavBarTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход «Назад»")
    public void shouldGoBack() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        basePageSteps.onBasePage().navButtonBack().click();

        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onMobileMainPage().searchFilters().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Параметры»")
    public void shouldGoToParameters() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().link(PARAMETERS).click();

        urlSteps.testing().path(FILTERS).queryParam(RGID, MOSCOW_RGID).queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .queryParam(CATEGORY_URL_PARAM, APARTMENT_URL_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Карта»")
    public void shouldGoToMap() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.onMobileSaleAdsPage().link(MAP).click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход «Назад» с залипшего навбара")
    public void shouldGoBackFloatNavbar() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.onMobileMainPage().searchFilters().applyFiltersButton().click();
        basePageSteps.scroll(PIXELS_TO_FLOAT_NAV_BAR);
        basePageSteps.onBasePage().navButtonBack().click();

        urlSteps.testing().path(MOSKVA).shouldNotDiffWithWebDriverUrl();
        basePageSteps.onMobileMainPage().searchFilters().should(isDisplayed());
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Параметры» с залипшего навбара")
    public void shouldGoToParametersFloatNavbar() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.scroll(PIXELS_TO_FLOAT_NAV_BAR);
        basePageSteps.onMobileSaleAdsPage().link(PARAMETERS).click();

        urlSteps.testing().path(FILTERS).queryParam(RGID, MOSCOW_RGID).queryParam(TYPE_URL_PARAM, SELL_URL_PARAM)
                .queryParam(CATEGORY_URL_PARAM, APARTMENT_URL_PARAM).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(ALEKS_IVANOV)
    @DisplayName("Переход на «Карта» с залипшего навбара")
    public void shouldGoToMapFloatNavbar() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        basePageSteps.scroll(PIXELS_TO_FLOAT_NAV_BAR);
        basePageSteps.onMobileSaleAdsPage().link(MAP).click();

        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).path(KARTA).shouldNotDiffWithWebDriverUrl();
    }

}
