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

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.FILTERS;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.CRANE;
import static ru.auto.tests.desktop.consts.Pages.DREDGE;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Расширенные фильтры поиска - группы инпутов")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class AdvancedFiltersInputGroupsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public String inputGroup;

    @Parameterized.Parameter(3)
    public String param;

    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameter(5)
    public String paramValueAfterInput;

    @Parameterized.Parameters(name = "name = {index}: {0} {1} {3}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Мощность", "power", "100", "100 л.с."},
                {CARS, USED, "Разгон", "acceleration", "10", "10 с"},
                {CARS, NEW, "Разгон", "acceleration", "10", "10 с"},

                {LCV, ALL, "Пробег", "km_age", "10000", "10 000 км"},
                {LCV, ALL, "Мощность", "power", "300", "300 л.с."},

                {TRUCK, ALL, "Пробег", "km_age", "10000", "10 000 км"},
                {TRUCK, ALL, "Мощность", "power", "300", "300 л.с."},

                {ARTIC, ALL, "Пробег", "km_age", "10000", "10 000 км"},
                {ARTIC, ALL, "Мощность", "power", "300", "300 л.с."},

                {CRANE, ALL, "Подъем", "load_height", "5", "5 м"},
                {CRANE, ALL, "Стрела", "crane_radius", "5", "5 м"},

                {DREDGE, ALL, "Ковш", "bucket_volume", "3", "3 м³"},

                {MOTORCYCLE, ALL, "Пробег", "km_age", "10000", "10 000 км"},
                {MOTORCYCLE, ALL, "Мощность", "power", "200", "200 л.с."},

                {ATV, ALL, "Мощность", "power", "150", "150 л.с."},

                {SNOWMOBILE, ALL, "Мощность", "power", "50", "50 л.с."}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.focusElementByScrollingOffset(basePageSteps.onListingPage().getSale(0), 0, 0);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр «От»")
    public void shouldSeeFromParamInUrl() {
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("от").sendKeys(paramValue);
        urlSteps.addParam(format("%s_from", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("от")
                .should(hasValue(paramValueAfterInput));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр «До»")
    public void shouldSeeToParamInUrl() {
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("до").sendKeys(paramValue);
        urlSteps.addParam(format("%s_to", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().filter().showAdvancedFilters();
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("до")
                .should(hasValue(paramValueAfterInput));
    }
}
