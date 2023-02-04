package ru.auto.tests.desktop.listing.filters;

import com.carlosbecker.guice.GuiceModules;
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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.RUSSIA;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - расширенный фильтр - цвет")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersColorMultiTest {

    private static final String COLOR_RED = "EE1D19";
    private static final String COLOR_SILVER = "CACECB";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {LCV},
                {MOTORCYCLE}
        });
    }

    @Before
    public void before() {
        basePageSteps.setWindowMaxHeight();
        urlSteps.testing().path(RUSSIA).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр 'Цвет' -> Мультивыбор")
    public void shouldSelectColorMultiselection() {
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().color(COLOR_RED).hover().click();
        basePageSteps.onListingPage().filter().color(COLOR_SILVER).hover().click();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();

        urlSteps.addParam("color", COLOR_RED).addParam("color", COLOR_SILVER).shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));

        basePageSteps.refresh();

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().should(isDisplayed());
    }
}
