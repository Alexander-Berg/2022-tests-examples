package ru.auto.tests.mobilereviews.pager;

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
import ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewsListingPage;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewsListingPage.REVIEWS_LISTING_SIZE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Листинг отзывов - пагинация")
@Feature(PAGER)
@RunWith(Parameterized.class)
@GuiceModules(MobileEmulationTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class ReviewsListingPagerTest {

    private static final int NEEDED_SCROLL = 30000;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Parameterized.Parameter
    public String category;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {CARS},
                {MOTO},
                {TRUCKS}
        });
    }

    @Before
    public void before() {
        urlSteps.testing().path(REVIEWS).path(category).path(ALL).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Подгрузка следующей страницы")
    public void shouldSeeNextPage() {
        basePageSteps.scrollDown(NEEDED_SCROLL);
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE * 2));
        urlSteps.addParam("page", "2").addParam("sort", ReviewsListingPage.SortBy.RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Подгрузка предыдущей страницы")
    public void shouldSeePreviousPage() {
        urlSteps.onCurrentUrl().addParam("page", "2").open();
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE));
        basePageSteps.onReviewsListingPage().prevPageButton().should(isDisplayed()).click();
        basePageSteps.onReviewsListingPage().prevPageButton().waitUntil(not(isDisplayed()));
        basePageSteps.onReviewsListingPage().reviewsList().waitUntil(hasSize(REVIEWS_LISTING_SIZE * 2));
        basePageSteps.scrollUp(NEEDED_SCROLL * 3);
        urlSteps.replaceParam("page", "1").addParam("sort", ReviewsListingPage.SortBy.RELEVANCE_DESC.getAlias())
                .shouldNotSeeDiff();
    }
}
