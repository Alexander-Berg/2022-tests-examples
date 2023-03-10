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
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasValue;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

//import io.qameta.allure.Parameter;

@DisplayName("?????????????? ?????????????? ???????????? - ???????????? ????")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class BaseFiltersInputGroupsToTest {

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

    //@Parameter("???????????? ??????????????")
    @Parameterized.Parameter(2)
    public String inputGroup;

    //@Parameter("????????????????")
    @Parameterized.Parameter(3)
    public String path;

    //@Parameter("???????????????? ??????????????????")
    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameter(5)
    public String paramValueAfterInput;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "????????????", "?km_age_to=10000", "10000", "10 000 ????"},
                {CARS, USED, "????????????", "?km_age_to=10000", "10000", "10 000 ????"},

                {CARS, ALL, "????????", "do-200000/", "200000", "200 000 ???"},
                {CARS, NEW, "????????", "do-1000000/", "1000000", "1 000 000 ???"},

                {CARS, NEW, "????????????????", "?power_to=500", "500", "500 ??.??."},

                {MOTORCYCLE, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {MOTORCYCLE, NEW, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {MOTORCYCLE, USED, "????????", "?price_to=100000", "100000", "100 000 ???"},

                {SCOOTERS, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {SCOOTERS, ALL, "????????????", "?km_age_to=10000", "10000", "10 000 ????"},
                {SCOOTERS, ALL, "????????????????", "?power_to=300", "300", "300 ??.??."},

                {ATV, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {ATV, ALL, "????????????", "?km_age_to=10000", "10000", "10 000 ????"},

                {SNOWMOBILE, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {SNOWMOBILE, ALL, "????????????", "?km_age_to=10000", "10000", "10 000 ????"},

                {LCV, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {LCV, ALL, "?????????? ????????", "?seats_to=3", "3", "3"},

                {TRUCK, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},

                {ARTIC, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},

                {BUS, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},

                {TRAILER, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {TRAILER, ALL, "??????-???? ????????", "?axis_to=1", "1", "1"},

                {AGRICULTURAL, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {AGRICULTURAL, ALL, "????????????????", "?operating_hours_to=1", "1", "1 ??.??."},
                {AGRICULTURAL, ALL, "????????????????", "?power_to=300", "300", "300 ??.??."},

                {CONSTRUCTION, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {CONSTRUCTION, ALL, "????????????????", "?operating_hours_to=1", "1", "1 ??.??."},

                {AUTOLOADER, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {AUTOLOADER, ALL, "????????????????", "?operating_hours_to=1", "1", "1 ??.??."},
                {AUTOLOADER, ALL, "????????????", "?load_height_to=1", "1", "1 ??"},

                {CRANE, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {CRANE, ALL, "????????????????", "?operating_hours_to=1", "1", "1 ??.??."},
                {CRANE, ALL, "????????????", "?km_age_to=10000", "10000", "10 000 ????"},

                {DREDGE, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {DREDGE, ALL, "????????????????", "?operating_hours_to=1", "1", "1 ??.??."},
                {DREDGE, ALL, "????????????????", "?power_to=300", "300", "300 ??.??."},

                {BULLDOZERS, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {BULLDOZERS, ALL, "????????????????", "?operating_hours_to=1", "1", "1 ??.??."},
                {BULLDOZERS, ALL, "????????????????", "?power_to=300", "300", "300 ??.??."},

                {MUNICIPAL, ALL, "????????", "?price_to=100000", "100000", "100 000 ???"},
                {MUNICIPAL, ALL, "????????????????", "?power_to=300", "300", "300 ??.??."}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("???????????????? ????????")
    public void shouldSeeToParamInUrl() {
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("????").sendKeys(paramValue);
        urlSteps.fromUri(format("%s/%s/%s/%s/%s", urlSteps.getConfig().getTestingURI(), MOSKVA.replaceAll("/", ""),
                        category.replaceAll("/", ""), section.replaceAll("/", ""), path))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("????")
                .should(hasValue(paramValueAfterInput));
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}