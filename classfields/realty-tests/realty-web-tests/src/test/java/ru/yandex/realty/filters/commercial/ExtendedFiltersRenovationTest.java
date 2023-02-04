package ru.yandex.realty.filters.commercial;

import com.carlosbecker.guice.GuiceModules;
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
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.yandex.realty.categories.Production;
import ru.yandex.realty.categories.Regression;
import ru.yandex.realty.categories.Smoke;
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.TORGOVOE_POMESHCHENIE;
import static ru.yandex.realty.consts.Owners.KANTEMIROV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersRenovationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Требует ремонта", "NEEDS_RENOVATION"}
        });
    }

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(COMMERCIAL).path(TORGOVOE_POMESHCHENIE).open();
        basePageSteps.onCommercialPage().openExtFilter();
        basePageSteps.scrollToElement(basePageSteps.onCommercialPage().extendFilters().byName("Ремонт"));
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KANTEMIROV)
    @DisplayName("Параметр Ремонт")
    public void shouldSeeRenovationInUrl() {
        basePageSteps.onCommercialPage().extendFilters().checkButton(label);
        basePageSteps.onCommercialPage().closeExtFilter();
        urlSteps.queryParam("renovation", expected).shouldNotDiffWithWebDriverUrl();
    }

}
