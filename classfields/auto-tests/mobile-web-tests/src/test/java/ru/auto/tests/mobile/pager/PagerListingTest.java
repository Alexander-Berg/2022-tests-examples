package ru.auto.tests.mobile.pager;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LCV;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.mobile.page.ListingPage.SALES_PER_PAGE;
import static ru.auto.tests.desktop.mobile.page.ListingPage.SALES_PER_PAGE_OLD;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@RunWith(Parameterized.class)
@GuiceModules(MobileTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
@DisplayName("Листинг - пагинация")
@Feature(PAGER)
public class PagerListingTest {

    private static final int NEEDED_SCROLL = 15000;
    private static final String PAGE_PARAM = "page";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "name = {index}: {0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {MOTORCYCLE},
                {LCV}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(MOSKVA).path(category).path(ALL).open();
        basePageSteps.onListingPage().salesList().should(hasSize(anyOf(equalTo(SALES_PER_PAGE),
                equalTo(SALES_PER_PAGE + 1))));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка следующей страницы")
    @Category({Regression.class})
    public void shouldSeeNextPage() {
        basePageSteps.scrollDown(NEEDED_SCROLL);

        basePageSteps.onListingPage().salesList().should(anyOf(hasSize(SALES_PER_PAGE * 2),
                hasSize(SALES_PER_PAGE * 2 - 1), //-1 склейка
                hasSize(SALES_PER_PAGE * 2 - 2), //-2 склейка
                hasSize(SALES_PER_PAGE * 2 - 3), //-3 склейка
                hasSize(SALES_PER_PAGE * 2 - 4))); //-4 склейка
        basePageSteps.scrollDown(300);
        urlSteps.addParam(PAGE_PARAM, "2").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка предыдущей страницы")
    @Category({Regression.class})
    public void shouldSeePreviousPage() {
        int salesOnPrevPage = SALES_PER_PAGE + SALES_PER_PAGE_OLD;

        urlSteps.onCurrentUrl().addParam(PAGE_PARAM, "2").open();
        basePageSteps.onListingPage().salesList().should(anyOf(hasSize(SALES_PER_PAGE), hasSize(SALES_PER_PAGE + 1)));
        basePageSteps.onListingPage().prevPageButton().should(isDisplayed()).click();

        basePageSteps.onListingPage().salesList().waitUntil(anyOf(hasSize(salesOnPrevPage),
                hasSize(salesOnPrevPage + 1), //+1 проданное
                hasSize(salesOnPrevPage + 2), //+2 проданных
                hasSize(salesOnPrevPage - 1), //-1 склейка
                hasSize(salesOnPrevPage - 2))); //-2 склейка
        basePageSteps.scrollUp(NEEDED_SCROLL);

        urlSteps.replaceParam(PAGE_PARAM, "2").shouldNotSeeDiff();
    }

}
