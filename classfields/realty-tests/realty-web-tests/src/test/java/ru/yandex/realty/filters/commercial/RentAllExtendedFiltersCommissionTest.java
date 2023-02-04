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
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KURAU;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;

@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RentAllExtendedFiltersCommissionTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;


    @Parameterized.Parameter
    public String label;

    @Parameterized.Parameter(1)
    public String param;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Без комиссии", "commissionMax", "0"},
                {"С комиссией", "commissionMin", "1"},
        });
    }

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(COMMERCIAL).open();
        user.onCommercialPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Smoke.class, Production.class})
    @Owner(KURAU)
    @DisplayName("Параметр комиссии")
    public void shouldSeeCommissionMaxInUrl() {
        user.scrollToElement(user.onCommercialPage().extendFilters().button(label));
        user.onCommercialPage().extendFilters().checkButton(label);
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(param, expected).shouldNotDiffWithWebDriverUrl();
    }

}
