package ru.auto.tests.mobile.main;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
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
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
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
    public String paramName;

    @Parameterized.Parameter(1)
    public String paramQueryName;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Расход до, л", "fuel_rate_to"},
                {"Клиренс от, мм", "clearance_from"},
                {"Багажник от, л", "trunk_volume_from"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().param(paramName).hover().click();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Одиночные инпуты")
    public void shouldSeeInputParamInUrl() {
        String paramValue = valueOf(getRandomShortInt());

        basePageSteps.onMainPage().paramsPopup().input(paramName).waitUntil(isDisplayed()).sendKeys(paramValue);
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();

        urlSteps.path(CARS).path(ALL).addParam(paramQueryName, paramValue).shouldNotSeeDiff();
    }
}
