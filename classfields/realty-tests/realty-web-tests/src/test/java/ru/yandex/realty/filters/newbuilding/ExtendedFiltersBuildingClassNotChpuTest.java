package ru.yandex.realty.filters.newbuilding;

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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.realty.consts.Filters.KUPIT;
import static ru.yandex.realty.consts.Filters.MOSKVA;
import static ru.yandex.realty.consts.Filters.NOVOSTROJKA;
import static ru.yandex.realty.consts.Owners.VICDEV;
import static ru.yandex.realty.consts.RealtyFeatures.FILTERS;


@DisplayName("Расширенные фильтры поиска по новостройкам. Класс здания")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(RealtyWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ExtendedFiltersBuildingClassNotChpuTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps user;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String title;

    @Parameterized.Parameter(1)
    public String label;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters(name = "{index} -{0}")
    public static Collection<Object[]> testParameters() {
        return asList(new Object[][]{
                {"Комфорт", "Комфорт", "COMFORT"},
                {"Комфорт плюс", "Комфорт +", "COMFORT_PLUS"},
        });
    }

    @Before
    public void openNewBuildingPage() {
        urlSteps.testing().path(MOSKVA).path(KUPIT).path(NOVOSTROJKA).open();
        user.onNewBuildingPage().openExtFilter();
    }

    @Test
    @Category({Regression.class, Production.class})
    @Owner(VICDEV)
    @DisplayName("Параметр «класс здания»")
    public void shouldSeeClassesBuildingInUrl() {
        user.onNewBuildingPage().extendFilters().select("Класс жилья", label);
        user.onNewBuildingPage().extendFilters().button(label).should(isDisplayed()).click();
        user.onNewBuildingPage().extendFilters().applyFiltersButton().click();
        urlSteps.queryParam("buildingClass", expected).shouldNotDiffWithWebDriverUrl();
    }
}
