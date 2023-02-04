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

@DisplayName("Базовые фильтры поиска - группы селекторов от/до")
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

    //@Parameter("Секция")
    @Parameterized.Parameter(1)
    public String section;

    //@Parameter("Селект")
    @Parameterized.Parameter(2)
    public String selectName;

    //@Parameter("Опция в селекте")
    @Parameterized.Parameter(3)
    public String selectItem;

    //@Parameter("Параметр")
    @Parameterized.Parameter(4)
    public String param;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(5)
    public String paramValue;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Объем", "2.5 л", "displacement", "2500"},
                {CARS, NEW, "Объем", "2.0 л", "displacement", "2000"},

                {CARS, ALL, "Год", "2015", "year", "2015"},
                {CARS, NEW, "Год", "2021", "year", "2021"},

                {LCV, ALL, "Год", "2015", "year", "2015"},
                {LCV, NEW, "Год", "2021", "year", "2021"},
                {LCV, USED, "Год", "2015", "year", "2015"},

                {TRUCK, ALL, "Год", "2015", "year", "2015"},

                {ARTIC, ALL, "Год", "2015", "year", "2015"},

                {BUS, ALL, "Год", "2015", "year", "2015"},

                {TRAILER, ALL, "Год", "2015", "year", "2015"},

                {AGRICULTURAL, ALL, "Год", "2015", "year", "2015"},
                {AGRICULTURAL, ALL, "Объем", "1.0 л", "displacement", "1000"},

                {CONSTRUCTION, ALL, "Год", "2015", "year", "2015"},

                {AUTOLOADER, ALL, "Год", "2015", "year", "2015"},

                {AUTOLOADER, ALL, "Год", "2015", "year", "2015"},

                {CRANE, ALL, "Год", "2015", "year", "2015"},
                {CRANE, ALL, "Объем", "1.0 л", "displacement", "1000"},

                {DREDGE, ALL, "Год", "2015", "year", "2015"},
                {DREDGE, ALL, "Объем", "1.0 л", "displacement", "1000"},

                {BULLDOZERS, ALL, "Год", "2015", "year", "2015"},
                {BULLDOZERS, ALL, "Объем", "1.0 л", "displacement", "1000"},

                {MUNICIPAL, ALL, "Год", "2015", "year", "2015"},

                {MOTORCYCLE, ALL, "Объем", "50 см³", "displacement", "50"},
                {MOTORCYCLE, ALL, "Год", "2015", "year", "2015"},
                {MOTORCYCLE, NEW, "Объем", "50 см³", "displacement", "50"},
                {MOTORCYCLE, NEW, "Год", "2021", "year", "2021"},
                {MOTORCYCLE, USED, "Объем", "50 см³", "displacement", "50"},
                {MOTORCYCLE, USED, "Год", "2015", "year", "2015"},

                {SCOOTERS, ALL, "Год", "2015", "year", "2015"},
                {SCOOTERS, ALL, "Объем", "50 см³", "displacement", "50"},

                {ATV, ALL, "Год", "2015", "year", "2015"},

                {SNOWMOBILE, ALL, "Год", "2015", "year", "2015"}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Селектор от")
    public void shouldSelectFrom() {
        basePageSteps.onListingPage().filter().selectGroupItem(selectName, "от", selectItem);
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
    @DisplayName("Селектор до")
    public void shouldSelectTo() {
        basePageSteps.onListingPage().filter().selectGroupItem(selectName, "до", selectItem);
        urlSteps.addParam(format("%s_to", param), paramValue).shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().waitForListingReload();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().selectGroup(selectItem).selectButton(selectItem).should(isDisplayed());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}
