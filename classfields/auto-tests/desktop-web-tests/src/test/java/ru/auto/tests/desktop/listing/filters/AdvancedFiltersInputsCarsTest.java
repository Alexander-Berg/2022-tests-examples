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
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Легковые - базовые фильтры поиска - инпуты")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersInputsCarsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;


    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    //@Parameter("Секция")
    @Parameterized.Parameter
    public String section;

    //@Parameter("Название инпута")
    @Parameterized.Parameter(1)
    public String input;

    //@Parameter("Параметр")
    @Parameterized.Parameter(2)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(3)
    public String paramValue;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {ALL, "Расход", "fuel_rate_to", "1"},
                {NEW, "Расход", "fuel_rate_to", "10"},

                {ALL, "Клиренс", "clearance_from", "1"},
                {NEW, "Клиренс", "clearance_from", "100"},

                {USED, "Багажник", "trunk_volume_from", "1000"},
                {NEW, "Багажник", "trunk_volume_from", "5000"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(CARS).path(section).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().getSale(0), 0, 0);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр")
    public void shouldSeeParamInUrl() {
        basePageSteps.onListingPage().filter().input(input).sendKeys(paramValue);
        urlSteps.addParam(param, paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
    }
}