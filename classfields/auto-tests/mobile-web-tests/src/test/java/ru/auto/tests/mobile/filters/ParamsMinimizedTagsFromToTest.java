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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Параметры - тэги от/до")
@Feature(AutoruFeatures.FILTERS)
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
    public String category;

    @Parameterized.Parameter(1)
    public String filterName;

    @Parameterized.Parameter(2)
    public String tag;

    @Parameterized.Parameter(3)
    public String paramName;

    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {2} {3} {4}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, "Год выпуска", "2016", "year", "2016"},
                {CARS, "Объём двигателя, л", "0.3 л", "displacement", "300"},

                {LCV, "Год выпуска", "2016", "year", "2016"},
                {LCV, "Объём двигателя, л", "0.2 л", "displacement", "200"},

                {MOTORCYCLE, "Год выпуска", "2016", "year", "2016"},
                {MOTORCYCLE, "Объём двигателя, см³", "50 см³", "displacement", "50"},
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().filters().paramsButton().click();
        basePageSteps.hideApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().param(filterName).hover().click();
    }

    @Test
    @DisplayName("Теги от")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTagFrom() {
        basePageSteps.onListingPage().paramsPopup().tagsFrom(filterName).button(tag).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(format("%s_from", paramName), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }

    @Test
    @DisplayName("Теги до")
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    public void shouldClickTagTo() {
        basePageSteps.onListingPage().paramsPopup().tagsTo(filterName).button(tag).click();
        basePageSteps.showApplyFiltersButton();
        basePageSteps.onListingPage().paramsPopup().applyFiltersButton().click();
        urlSteps.addParam(format("%s_to", paramName), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
        urlSteps.refresh();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filters().should(isDisplayed());
    }
}
