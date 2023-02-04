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
import ru.auto.tests.commons.util.Utils;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;


@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedBuildingYearFiltersTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(KVARTIRA).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Год от»")
    public void shouldSeeYearMinInUrl() {
        String yearFrom = "197" + Utils.getRandomShortInt();
        user.onOffersSearchPage().extendFilters().byName("Год постройки").input("c").sendKeys(yearFrom);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        user.loaderWait();
        urlSteps.queryParam("builtYearMin", yearFrom).shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Год до»")
    public void shouldSeeYearMaxInUrl() {
        String yearTo = "197" + Utils.getRandomShortInt();
        user.onOffersSearchPage().extendFilters().byName("Год постройки").input("по").sendKeys(yearTo);
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("builtYearMax", yearTo).shouldNotDiffWithWebDriverUrl();
    }
}
