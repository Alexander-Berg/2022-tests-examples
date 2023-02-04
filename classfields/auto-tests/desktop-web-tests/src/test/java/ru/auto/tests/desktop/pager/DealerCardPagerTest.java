package ru.auto.tests.desktop.pager;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.DEALERS;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER;
import static ru.auto.tests.desktop.consts.Dealers.CARS_OFFICIAL_DEALER_MARK;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.DILER_OFICIALNIY;
import static ru.auto.tests.desktop.page.ListingPage.SALES_PER_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Карточка дилера - пагинация")
@Feature(DEALERS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerCardPagerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsMercedes",
                "desktop/Salon",
                "desktop/SearchCarsAllDealerIdMarkPage2",
                "desktop/SearchCarsAllDealerIdMarkPage10",
                "desktop/SearchCarsAllDealerIdMark",
                "desktop/SearchCarsCountDealerId",
                "desktop/UserFavoritesAllSubscriptionsEmpty",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(DILER_OFICIALNIY).path(CARS).path(ALL)
                .path(CARS_OFFICIAL_DEALER).path(CARS_OFFICIAL_DEALER_MARK).path("/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNextButton() {
        basePageSteps.onDealerCardPage().pager().next().click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(SALES_PER_PAGE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на предыдущую страницу по кнопке «Предыдущая»")
    public void shouldClickPrevButton() {
        urlSteps.addParam("page", "2").open();
        basePageSteps.onDealerCardPage().pager().prev().click();
        urlSteps.replaceQuery("").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(SALES_PER_PAGE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход на страницу через пагинатор")
    public void shouldClickPageInPager() {
        basePageSteps.onDealerCardPage().pager().page("2").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(SALES_PER_PAGE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickMoreButton() {
        basePageSteps.onDealerCardPage().pager().button("Показать ещё").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().waitUntil(hasSize(SALES_PER_PAGE * 2));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «...»")
    public void shouldClickSkipButton() {
        basePageSteps.onDealerCardPage().pager().threeDotsFirst().click();
        basePageSteps.onListingPage().pager().currentPage().waitUntil(hasText("10"));
        urlSteps.addParam("page", "10").shouldNotSeeDiff();
        basePageSteps.onListingPage().salesList().should(hasSize(SALES_PER_PAGE));
    }
}