package ru.auto.tests.mobile.pager;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER;
import static ru.auto.tests.desktop.mobile.page.ListingPage.SALES_PER_PAGE;
import static ru.auto.tests.desktop.mobile.page.ListingPage.SALES_PER_PAGE_OLD;

@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
@DisplayName("Карточка дилера - пагинация")
@Feature(PAGER)
public class PagerDealerCardTest {

    private static final String PAGE_PARAM = "page";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DILER).path(CARS).path(ALL).path("/rolf_ugo_vostok_avtomobili_s_probegom_moskva/")
                .open();
        basePageSteps.onDealerCardPage().salesList().should(hasSize(SALES_PER_PAGE_OLD));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка следующей страницы")
    @Category({Regression.class})
    public void shouldSeeNextPage() {
        basePageSteps.onDealerCardPage().getSale(SALES_PER_PAGE_OLD - 1).hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.scrollDown(2000);
        basePageSteps.onDealerCardPage().salesList().waitUntil(hasSize(SALES_PER_PAGE_OLD * 2));
        urlSteps.addParam(PAGE_PARAM, "2").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Подгрузка предыдущей страницы")
    @Category({Regression.class})
    public void shouldSeePreviousPage() {
        urlSteps.onCurrentUrl().addParam(PAGE_PARAM, "2").open();
        basePageSteps.onListingPage().salesList().should(hasSize(SALES_PER_PAGE_OLD));
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().prevPageButton());
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(SALES_PER_PAGE_OLD * 2));
        urlSteps.replaceParam(PAGE_PARAM, "2").shouldNotSeeDiff();
    }
}
