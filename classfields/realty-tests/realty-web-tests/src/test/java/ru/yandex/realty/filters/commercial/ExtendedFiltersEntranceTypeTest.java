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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(RealtyWebModule.class)
public class ExtendedFiltersEntranceTypeTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(OFIS).open();
        user.onCommercialPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «тип входа» - «Отдельный»")
    public void shouldSeeEntranceTypeSeparateInUrl() {
        user.onCommercialPage().extendFilters().checkButton("Отдельный");
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.path("/otdelniy-vhod/").shouldNotDiffWithWebDriverUrl();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «тип входа» - «Общий»")
    public void shouldSeeEntranceTypeCommonInUrl() {
        user.onCommercialPage().extendFilters().checkButton("Общий");
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("entranceType", "COMMON").shouldNotDiffWithWebDriverUrl();
    }
}
