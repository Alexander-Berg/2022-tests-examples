package ru.yandex.realty.newfilters;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.yandex.realty.categories.Mobile;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.mobile.step.BasePageSteps;
import ru.yandex.realty.module.RealtyWebMobileModule;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FilterPopup.METRO;

@DisplayName("Базовые фильтры. Метро")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseLocationMetroFiltersTest {

    private static final String METRO_GEO_ID = "metroGeoId";
    private static final String TEST_METRO_ID = "114766";
    private static final String SECOND_TEST_METRO_ID = "20302";
    private static final String TEST_METRO = "Автово";
    private static final String SECOND_TEST_METRO = "Адмиралтейская";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SPB_I_LO).open();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем одну станцию из списка")
    @Category({Regression.class, Mobile.class})
    public void shouldChooseOneMetroStation() {
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().button(METRO).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().metro(TEST_METRO).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).path("metro-avtovo/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем две станции из списка")
    @Category({Regression.class, Mobile.class})
    public void shouldChooseTwoMetroStations() {
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().button(METRO).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().metro(TEST_METRO).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().metro(SECOND_TEST_METRO).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA)
                .queryParam(METRO_GEO_ID, TEST_METRO_ID)
                .queryParam(METRO_GEO_ID, SECOND_TEST_METRO_ID)
                .shouldNotDiffWithWebDriverUrl();
    }
}
