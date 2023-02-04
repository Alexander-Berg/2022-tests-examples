package ru.yandex.realty.filters.commercial;

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

import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SANKT_PETERBURG;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersFurnitureTest {

    private static final String FURNITURE = "Мебель";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(SANKT_PETERBURG).path(KUPIT).path(COMMERCIAL).path(OFIS).open();
        user.onCommercialPage().openExtFilter();
        user.scrollToElement(user.onCommercialPage().extendFilters().byName(FURNITURE));
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Параметр «мебель» «Есть»")
    public void shouldSeeWithFurnitureInUrl() {
        user.onCommercialPage().extendFilters().byName(FURNITURE).checkButton("Есть");
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/s-mebeliu/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Параметр «мебель» «Нет»")
    public void shouldSeeWithoutFurnitureInUrl() {
        user.onCommercialPage().extendFilters().byName(FURNITURE).checkButton("Нет");
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("hasFurniture", "NO").shouldNotDiffWithWebDriverUrl();
    }

}
