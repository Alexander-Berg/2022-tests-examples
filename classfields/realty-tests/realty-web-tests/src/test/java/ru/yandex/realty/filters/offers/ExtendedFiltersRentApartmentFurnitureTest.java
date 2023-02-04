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

import static ru.yandex.realty.consts.Filters.KVARTIRA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Filters.SPB_I_LO;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Расширенные фильтры поиска по объявлениям.")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersRentApartmentFurnitureTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openSaleAdsPage() {
        urlSteps.testing().path(SPB_I_LO).path(SNYAT).path(KVARTIRA).open();
        user.onOffersSearchPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Мебель есть»")
    public void shouldSeeFurnitureTypeInUrl() {
        user.scrollElementToCenter(user.onOffersSearchPage().extendFilters().button("Есть"));
        user.onOffersSearchPage().extendFilters().checkButton("Есть");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/s-mebeliu/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «Мебель нет»")
    public void shouldSeeWithoutFurnitureTypeInUrl() {
        user.scrollElementToCenter(user.onOffersSearchPage().extendFilters().button("Нет"));
        user.onOffersSearchPage().extendFilters().checkButton("Нет");
        user.onOffersSearchPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("hasFurniture", "NO").shouldNotDiffWithWebDriverUrl();
    }
}
