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

import static java.lang.String.format;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.element.main.FilterPopup.RAYON;

@DisplayName("Базовые фильтры. Район")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseLocationRayonFiltersTest {

    private static final String SUB_LOCALITY = "subLocality";
    private static final String ADMIRALTEISKY_ID = "417975";
    private static final String KRASNOGVARDEYSKY_ID = "417964";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SPB_I_LO).open();
        user.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        user.onMobileMainPage().searchFilters().metroAndStreet().click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем один район из списка")
    @Category({Regression.class, Mobile.class})
    public void shouldChooseOneSubLocality() {
        user.onMobileMainPage().searchFilters().filterPopup().button(RAYON).click();
        user.onMobileMainPage().searchFilters().filterPopup().rayon("Адмиралтейский").click();
        user.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA).path(format("/dist-admiraltejskij-rajon-%s/", ADMIRALTEISKY_ID))
                .shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем два района из списка")
    @Category({Regression.class, Mobile.class})
    public void shouldChooseTwoSubLocalities() {
        user.onMobileMainPage().searchFilters().filterPopup().button(RAYON).click();
        user.onMobileMainPage().searchFilters().filterPopup().rayon("Красногвардейский").click();
        user.onMobileMainPage().searchFilters().filterPopup().rayon("Адмиралтейский").click();
        user.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KVARTIRA)
                .queryParam(SUB_LOCALITY, ADMIRALTEISKY_ID)
                .queryParam(SUB_LOCALITY, KRASNOGVARDEYSKY_ID)
                .shouldNotDiffWithWebDriverUrl();
    }
}
