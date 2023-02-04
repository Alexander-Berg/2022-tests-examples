package ru.auto.tests.mobile.filters;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Параметры - инпуты")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsInputsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String paramName;

    @Parameterized.Parameter(2)
    public String paramQueryName;

    @Parameterized.Parameters(name = "name = {index}: {0} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "Расход до, л", "fuel_rate_to"},
                {CARS, "Клиренс от, мм", "clearance_from"},
                {CARS, "Багажник от, л", "trunk_volume_from"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().param(paramName).hover().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Одиночные инпуты")
    public void shouldSeeInputParamInUrl() {
        String paramValue = valueOf(getRandomShortInt());
        basePageSteps.onListingPage().paramsPopup().input(paramName).waitUntil(isDisplayed()).sendKeys(paramValue);
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam(paramQueryName, paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
