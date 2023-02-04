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
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;

@DisplayName("Параметры - тэги от/до")
@Feature(AutoruFeatures.MAIN)
@Story(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ParamsMinimizedTagsFromToTest {

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
    public String tag;

    @Parameterized.Parameter(2)
    public String paramName;

    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Год выпуска", "2016", "year", "2016"},
                {"Объём двигателя, л", "0.2 л", "displacement", "200"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).open();
        basePageSteps.onMainPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().param(filterName).hover().click();
    }

    @Test
    @DisplayName("Теги от")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTagFrom() {
        basePageSteps.onMainPage().paramsPopup().tagsFrom(filterName).button(tag).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(format("%s_from", paramName), paramValue).shouldNotSeeDiff();
    }

    @Test
    @DisplayName("Теги до")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTagTo() {
        basePageSteps.onMainPage().paramsPopup().tagsTo(filterName).button(tag).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onMainPage().paramsPopup().applyFiltersButton().click();

        urlSteps.testing().path(MOSKVA).path(CARS).path(ALL)
                .addParam(format("%s_to", paramName), paramValue).shouldNotSeeDiff();
    }
}
