package ru.auto.tests.desktop.listing.sortbar;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
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
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.element.SortBar.SortBy;
import static ru.auto.tests.desktop.element.SortBar.SortBy.AUTORU_EXCLUSIVE_DESC;
import static ru.auto.tests.desktop.element.SortBar.SortBy.PROWEN_OWNER_DESC;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг - особенные сортировки")
@Feature(FILTERS)
@RunWith(Parameterized.class)
@GuiceModules(DesktopTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SortSpecialTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Inject
    public CookieSteps cookieSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameter(1)
    public String section;

    @Parameterized.Parameter(2)
    public SortBy sort;

    @Parameterized.Parameters(name = "name = {index}: {0} {2}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS, ALL, AUTORU_EXCLUSIVE_DESC},
                {CARS, ALL, PROWEN_OWNER_DESC},

                {CARS, USED, AUTORU_EXCLUSIVE_DESC},
                {CARS, USED, PROWEN_OWNER_DESC}
        });
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Выбор сортировки")
    public void shouldSelectSort() {
        urlSteps.testing().path(MOSKVA).path(category).path(section).open();
        basePageSteps.onListingPage().sortBar().selectItem("Сортировка", sort.getName());
        urlSteps.replaceParam("sort", sort.getAlias().toLowerCase()).shouldNotSeeDiff();
        basePageSteps.onListingPage().sortBar().select(sort.getName()).should(isDisplayed());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(greaterThan(0)));

        urlSteps.testing().path(LCV).path(section).open();
        basePageSteps.onListingPage().sortBar().select("Сортировка").should(isDisplayed());
    }
}