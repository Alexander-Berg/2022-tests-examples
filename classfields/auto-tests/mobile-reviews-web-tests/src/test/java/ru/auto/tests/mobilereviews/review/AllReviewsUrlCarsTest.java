package ru.auto.tests.mobilereviews.review;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.page.mobilereviews.ReviewsListingPage;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Страница отзыва в легковых - ссылка «Все отзывы о ...»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class AllReviewsUrlCarsTest {

    private static final String MARK = "/uaz/";
    private static final String MODEL = "/patriot/";
    private static final String GENERATION = "2309645";
    private static final String REVIEW_ID = "4014660";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "reviews/ReviewsAutoCars").post();

        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GENERATION)
                .path(REVIEW_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Все отзывы о ...»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onReviewPage().allReviewsUrl().click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/")
                .addParam("sort", ReviewsListingPage.SortBy.RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
    }
}
