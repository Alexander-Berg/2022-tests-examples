package ru.auto.tests.mobile.listing;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.SORT;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.DATE_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.FRESH_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.NAME_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.PRICE_PROFITABILITY_DESC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.RUN_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.YEAR_ASC;
import static ru.auto.tests.desktop.mobile.element.listing.SortBar.SortBy.YEAR_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - сортировки")
@Feature(SORT)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SortTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public SortBy sort;

    @Parameterized.Parameters(name = "name = {index}: {0} {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, FRESH_DESC},
                {CARS, DATE_DESC},
                {CARS, PRICE_ASC},
                {CARS, PRICE_DESC},
                {CARS, YEAR_DESC},
                {CARS, YEAR_ASC},
                {CARS, RUN_ASC},
                {CARS, NAME_ASC},
                {CARS, PRICE_PROFITABILITY_DESC},

                {LCV, FRESH_DESC},
                {LCV, DATE_DESC},
                {LCV, PRICE_ASC},
                {LCV, PRICE_DESC},
                {LCV, YEAR_ASC},
                {LCV, YEAR_DESC},
                {LCV, RUN_ASC},
                {LCV, NAME_ASC},

                {MOTORCYCLE, FRESH_DESC},
                {MOTORCYCLE, DATE_DESC},
                {MOTORCYCLE, PRICE_ASC},
                {MOTORCYCLE, PRICE_DESC},
                {MOTORCYCLE, YEAR_ASC},
                {MOTORCYCLE, YEAR_DESC},
                {MOTORCYCLE, RUN_ASC},
                {MOTORCYCLE, NAME_ASC}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Сортировка")
    public void shouldSort() {
        urlSteps.testing().path(category).path(ALL).open();
        basePageSteps.onListingPage().sortBar().sort(format("По %s", FRESH_DESC.getName())).click();
        basePageSteps.onListingPage().sortPopup().sort(sort.getName()).click();
        basePageSteps.onListingPage().sortPopup().waitUntil(not(isDisplayed()));
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).addParam("sort", sort.getAlias())
                .shouldNotSeeDiff();
        basePageSteps.onListingPage().sortBar().sort(format("По %s", sort.getName())).waitUntil(isDisplayed());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));
    }
}
