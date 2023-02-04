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
import ru.yandex.realty.module.RealtyWebModule;
import ru.yandex.realty.step.BasePageSteps;
import ru.yandex.realty.step.UrlSteps;

import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.yandex.realty.consts.Filters.COMMERCIAL;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.OFIS;
import static ru.yandex.realty.consts.Filters.SNYAT;
import static ru.yandex.realty.consts.Owners.KOPITSA;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;


@DisplayName("Фильтры поиска по коммерческой недвижимости")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class RentOfficeExtendedFiltersTest {

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
    public String comfort;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Клининг", "hasCleaningIncluded"},
                {"КУ", "hasUtilitiesIncluded"},
                {"Электроэнергия", "hasElectricityIncluded"},
                {"Кондиционер", "hasAircondition"},
        });
    }

    @Before
    public void openCommercialPage() {
        urlSteps.testing().path(MOSKVA).path(SNYAT).path(COMMERCIAL).path(OFIS).open();
        user.onCommercialPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(KOPITSA)
    @DisplayName("Параметр «удобства»")
    public void shouldSeeComfortsInUrl() {
        user.scrollToElement(user.onCommercialPage().extendFilters().button(label));
        user.onCommercialPage().extendFilters().checkButton(label);
        user.onCommercialPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam(comfort, "YES").shouldNotDiffWithWebDriverUrl();
    }
}
