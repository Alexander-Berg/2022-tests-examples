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
import static ru.auto.tests.desktop.consts.Pages.AGRICULTURAL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.ARTIC;
import static ru.auto.tests.desktop.consts.Pages.ATV;
import static ru.auto.tests.desktop.consts.Pages.AUTOLOADER;
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
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.SCOOTERS;
import static ru.auto.tests.desktop.consts.Pages.SNOWMOBILE;
import static ru.auto.tests.desktop.consts.Pages.TRAILER;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("?????????????? ?????????????? ???????????? - ???????????? ???????????????????? ????/????")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersSelectorGroupsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    //@Parameter("????????????")
    @Parameterized.Parameter(1)
    public String section;

    //@Parameter("????????????")
    @Parameterized.Parameter(2)
    public String selectName;

    //@Parameter("?????????? ?? ??????????????")
    @Parameterized.Parameter(3)
    public String selectItem;

    //@Parameter("????????????????")
    @Parameterized.Parameter(4)
    public String param;

    //@Parameter("???????????????? ??????????????????")
    @Parameterized.Parameter(5)
    public String paramValue;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "??????????", "2.5 ??", "displacement", "2500"},
                {CARS, NEW, "??????????", "2.0 ??", "displacement", "2000"},

                {CARS, ALL, "??????", "2015", "year", "2015"},
                {CARS, NEW, "??????", "2021", "year", "2021"},

                {LCV, ALL, "??????", "2015", "year", "2015"},
                {LCV, NEW, "??????", "2021", "year", "2021"},
                {LCV, USED, "??????", "2015", "year", "2015"},

                {TRUCK, ALL, "??????", "2015", "year", "2015"},

                {ARTIC, ALL, "??????", "2015", "year", "2015"},

                {BUS, ALL, "??????", "2015", "year", "2015"},

                {TRAILER, ALL, "??????", "2015", "year", "2015"},

                {AGRICULTURAL, ALL, "??????", "2015", "year", "2015"},
                {AGRICULTURAL, ALL, "??????????", "1.0 ??", "displacement", "1000"},

                {CONSTRUCTION, ALL, "??????", "2015", "year", "2015"},

                {AUTOLOADER, ALL, "??????", "2015", "year", "2015"},

                {AUTOLOADER, ALL, "??????", "2015", "year", "2015"},

                {CRANE, ALL, "??????", "2015", "year", "2015"},
                {CRANE, ALL, "??????????", "1.0 ??", "displacement", "1000"},

                {DREDGE, ALL, "??????", "2015", "year", "2015"},
                {DREDGE, ALL, "??????????", "1.0 ??", "displacement", "1000"},

                {BULLDOZERS, ALL, "??????", "2015", "year", "2015"},
                {BULLDOZERS, ALL, "??????????", "1.0 ??", "displacement", "1000"},

                {MUNICIPAL, ALL, "??????", "2015", "year", "2015"},

                {MOTORCYCLE, ALL, "??????????", "50 ??????", "displacement", "50"},
                {MOTORCYCLE, ALL, "??????", "2015", "year", "2015"},
                {MOTORCYCLE, NEW, "??????????", "50 ??????", "displacement", "50"},
                {MOTORCYCLE, NEW, "??????", "2021", "year", "2021"},
                {MOTORCYCLE, USED, "??????????", "50 ??????", "displacement", "50"},
                {MOTORCYCLE, USED, "??????", "2015", "year", "2015"},

                {SCOOTERS, ALL, "??????", "2015", "year", "2015"},
                {SCOOTERS, ALL, "??????????", "50 ??????", "displacement", "50"},

                {ATV, ALL, "??????", "2015", "year", "2015"},

                {SNOWMOBILE, ALL, "??????", "2015", "year", "2015"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("???????????????? ????")
    public void shouldSelectFrom() {
        basePageSteps.onListingPage().filter().selectGroupItem(selectName, "????", selectItem);
        urlSteps.addParam(format("%s_from", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().selectGroup(selectItem).selectButton(selectItem).should(isDisplayed());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("???????????????? ????")
    public void shouldSelectTo() {
        basePageSteps.onListingPage().filter().selectGroupItem(selectName, "????", selectItem);
        urlSteps.addParam(format("%s_to", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().selectGroup(selectItem).selectButton(selectItem).should(isDisplayed());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}
