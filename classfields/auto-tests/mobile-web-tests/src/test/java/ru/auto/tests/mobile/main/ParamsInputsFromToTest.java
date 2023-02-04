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

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static ru.auto.tests.commons.util.Utils.getRandomShortInt;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.BULLDOZERS;
import static ru.auto.tests.desktop.consts.Pages.BUS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CONSTRUCTION;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.MUNICIPAL;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Параметры - инпуты от/до")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsInputsFromToTest {

    private String paramValue = valueOf(getRandomShortInt());

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
                {"Мощность, л.с.", "power"},
                {"Пробег, км", "km_age"},
                {"Разгон до 100 км/ч, с", "acceleration"}
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
    @DisplayName("Инпуты от")
    public void shouldInputFrom() {
        basePageSteps.onMainPage().paramsPopup().inputFrom(paramName).waitUntil(isDisplayed()).sendKeys(paramValue);
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(format("%s_from", paramQueryName), paramValue).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Инпуты до")
    public void shouldInputTo() {
        basePageSteps.onMainPage().paramsPopup().inputTo(paramName).waitUntil(isDisplayed()).sendKeys(paramValue);
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(format("%s_to", paramQueryName), paramValue).shouldNotSeeDiff();
    }
}
