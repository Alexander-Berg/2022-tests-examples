package ru.auto.tests.mobile.catalog;

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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Каталог - расширенные фильтры, селекторы от/до")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersSelectorsFromToTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String filterName;

    @Parameterized.Parameter(1)
    public String option;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {2} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Объем", "0.2 л", "displacement", "200"},
                {"Год", "2017", "year", "2017"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).open();
        basePageSteps.onCatalogMainPage().filter().allParamsButton().should(isDisplayed()).click();
    }

    @Test
    @DisplayName("Селекторы от")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeSelectorsFromInUrl() {
        basePageSteps.selectOption(basePageSteps.onFiltersPage().selectorFrom(filterName), option);
        waitSomething(500, TimeUnit.MILLISECONDS);
        basePageSteps.onFiltersPage().applyFiltersButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(String.format("%s_from", paramName), paramValue)
                .shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Селекторы до")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeSelectorsToInUrl() {
        basePageSteps.selectOption(basePageSteps.onFiltersPage().selectorTo(filterName), option);
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onFiltersPage().applyFiltersButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(String.format("%s_to", paramName), paramValue)
                .shouldNotSeeDiff();
    }
}
