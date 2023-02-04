package ru.yandex.realty.filters.offers;

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
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Filters.UCHASTOK;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersBuyLotTest {

    public static final String LOT_TYPE = "lotType";
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(UCHASTOK).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «садоводство»")
    public void shouldSeeGardenInUrl() {
        user.onOffersSearchPage().extendFilters().checkButton("В садоводстве");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(LOT_TYPE, "GARDEN").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «ИЖС»")
    public void shouldSeeIGSFiltersInUrl() {
        user.onOffersSearchPage().extendFilters().selectCheckBox("ИЖС");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/izhs/").shouldNotDiffWithWebDriverUrl();
    }
}
