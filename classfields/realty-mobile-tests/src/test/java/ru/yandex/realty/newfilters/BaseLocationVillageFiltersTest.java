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
import static ru.yandex.realty.consts.Filters.KOTTEDZHNYE_POSELKI;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA_I_MO;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;
import static ru.yandex.realty.mobile.page.MainPage.DOM_OPTION;
import static ru.yandex.realty.mobile.page.MainPage.KVARTIRU_OPTION;

@DisplayName("Базовые фильтры. КП")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebMobileModule.class)
public class BaseLocationVillageFiltersTest {

    private static final String TEST_KP = "Кратово";
    private static final String VILLAGE_ID = "villageId";
    private static final String TEST_KP_VALUE = "1772004";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA_I_MO).open();
        basePageSteps.selectOption(basePageSteps.onMobileMainPage().searchFilters().selector(KVARTIRU_OPTION),
                DOM_OPTION);
        basePageSteps.onMobileMainPage().searchFilters().button("Коттеджный пос.").click();
        basePageSteps.onMobileMainPage().searchFilters().waitUntil(isDisplayed());
        basePageSteps.onMobileMainPage().searchFilters().metroAndStreet().click();
    }

    @Test
    @Owner(KURAU)
    @DisplayName("Выбираем «Посёлок»")
    @Category({Regression.class, Mobile.class})
    public void shouldChooseOneVillage() {
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().input().sendKeys(TEST_KP);
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().item(TEST_KP).click();
        basePageSteps.onMobileMainPage().searchFilters().filterPopup().showButton().click();

        urlSteps.path(KUPIT).path(KOTTEDZHNYE_POSELKI).queryParam(VILLAGE_ID, TEST_KP_VALUE)
                .queryParam("category", "HOUSE").queryParam("objectType", "VILLAGE")
                .shouldNotDiffWithWebDriverUrl();
    }
}
