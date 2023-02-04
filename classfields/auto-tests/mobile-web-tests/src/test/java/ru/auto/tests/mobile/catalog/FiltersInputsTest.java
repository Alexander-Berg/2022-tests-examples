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

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CATALOG;
import static ru.auto.tests.desktop.consts.Pages.FILTERS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Каталог - расширенные фильтры, инпуты")
@Feature(AutoruFeatures.FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FiltersInputsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(CATALOG).open();
        basePageSteps.onCatalogMainPage().filter().allParamsButton().should(isDisplayed()).click();
    }

    @Parameterized.Parameter
    public String filterName;

    @Parameterized.Parameter(1)
    public String paramName;

    @Parameterized.Parameter(2)
    public int minParamValue;

    @Parameterized.Parameters(name = "name = {index}: {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Цена", "price", 100000},
                {"Разгон", "acceleration", 0},
                {"Мощн.", "power", 0}
        });
    }

    @Test
    @DisplayName("Инпуты от")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeInputsFromInUrl() {
        String paramValue = valueOf(minParamValue + getRandomShortInt());
        basePageSteps.onFiltersPage().inputFrom(filterName).waitUntil(isDisplayed()).sendKeys(paramValue);
        waitSomething(1, TimeUnit.SECONDS);
        urlSteps.path(CARS).path(FILTERS).addParam(format("%s_from", paramName), paramValue).shouldNotSeeDiff();
        basePageSteps.onFiltersPage().applyFiltersButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(String.format("%s_from", paramName), paramValue)
                .shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Инпуты до")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldSeeInputsToInUrl() throws InterruptedException {
        String paramValue = valueOf(minParamValue + getRandomShortInt());
        basePageSteps.onFiltersPage().inputTo(filterName).waitUntil(isEnabled()).sendKeys(paramValue);
        waitSomething(1, TimeUnit.SECONDS);
        urlSteps.path(CARS).path(FILTERS).addParam(format("%s_to", paramName), paramValue).shouldNotSeeDiff();
        basePageSteps.onFiltersPage().applyFiltersButton().waitUntil(isDisplayed()).click();
        urlSteps.testing().path(CATALOG).path(CARS).path(ALL).addParam(String.format("%s_to", paramName), paramValue)
                .shouldNotSeeDiff();
    }
}
