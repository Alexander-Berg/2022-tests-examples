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

@DisplayName("Базовые фильтры поиска - инпуты до")
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

    //@Parameter("Секция")
    @Parameterized.Parameter(1)
    public String section;

    //@Parameter("Группа инпутов")
    @Parameterized.Parameter(2)
    public String inputGroup;

    //@Parameter("Параметр")
    @Parameterized.Parameter(3)
    public String path;

    //@Parameter("Значение параметра")
    @Parameterized.Parameter(4)
    public String paramValue;

    @Parameterized.Parameter(5)
    public String paramValueAfterInput;

    @Parameterized.Parameters(name = "{0} {1} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, "Пробег", "?km_age_to=10000", "10000", "10 000 км"},
                {CARS, USED, "Пробег", "?km_age_to=10000", "10000", "10 000 км"},

                {CARS, ALL, "Цена", "do-200000/", "200000", "200 000 ₽"},
                {CARS, NEW, "Цена", "do-1000000/", "1000000", "1 000 000 ₽"},

                {CARS, NEW, "Мощность", "?power_to=500", "500", "500 л.с."},

                {MOTORCYCLE, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {MOTORCYCLE, NEW, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {MOTORCYCLE, USED, "Цена", "?price_to=100000", "100000", "100 000 ₽"},

                {SCOOTERS, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {SCOOTERS, ALL, "Пробег", "?km_age_to=10000", "10000", "10 000 км"},
                {SCOOTERS, ALL, "Мощность", "?power_to=300", "300", "300 л.с."},

                {ATV, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {ATV, ALL, "Пробег", "?km_age_to=10000", "10000", "10 000 км"},

                {SNOWMOBILE, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {SNOWMOBILE, ALL, "Пробег", "?km_age_to=10000", "10000", "10 000 км"},

                {LCV, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {LCV, ALL, "Число мест", "?seats_to=3", "3", "3"},

                {TRUCK, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},

                {ARTIC, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},

                {BUS, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},

                {TRAILER, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {TRAILER, ALL, "Кол-во осей", "?axis_to=1", "1", "1"},

                {AGRICULTURAL, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {AGRICULTURAL, ALL, "Моточасы", "?operating_hours_to=1", "1", "1 м.ч."},
                {AGRICULTURAL, ALL, "Мощность", "?power_to=300", "300", "300 л.с."},

                {CONSTRUCTION, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {CONSTRUCTION, ALL, "Моточасы", "?operating_hours_to=1", "1", "1 м.ч."},

                {AUTOLOADER, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {AUTOLOADER, ALL, "Моточасы", "?operating_hours_to=1", "1", "1 м.ч."},
                {AUTOLOADER, ALL, "Подъем", "?load_height_to=1", "1", "1 м"},

                {CRANE, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {CRANE, ALL, "Моточасы", "?operating_hours_to=1", "1", "1 м.ч."},
                {CRANE, ALL, "Пробег", "?km_age_to=10000", "10000", "10 000 км"},

                {DREDGE, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {DREDGE, ALL, "Моточасы", "?operating_hours_to=1", "1", "1 м.ч."},
                {DREDGE, ALL, "Мощность", "?power_to=300", "300", "300 л.с."},

                {BULLDOZERS, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {BULLDOZERS, ALL, "Моточасы", "?operating_hours_to=1", "1", "1 м.ч."},
                {BULLDOZERS, ALL, "Мощность", "?power_to=300", "300", "300 л.с."},

                {MUNICIPAL, ALL, "Цена", "?price_to=100000", "100000", "100 000 ₽"},
                {MUNICIPAL, ALL, "Мощность", "?power_to=300", "300", "300 л.с."}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Параметр «До»")
    public void shouldSeeToParamInUrl() {
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("до").sendKeys(paramValue);
        urlSteps.fromUri(format("%s/%s/%s/%s/%s", urlSteps.getConfig().getTestingURI(), MOSKVA.replaceAll("/", ""),
                        category.replaceAll("/", ""), section.replaceAll("/", ""), path))
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().resultsButton().waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        basePageSteps.onListingPage().filter().inputGroup(inputGroup).input("до")
                .should(hasValue(paramValueAfterInput));
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}