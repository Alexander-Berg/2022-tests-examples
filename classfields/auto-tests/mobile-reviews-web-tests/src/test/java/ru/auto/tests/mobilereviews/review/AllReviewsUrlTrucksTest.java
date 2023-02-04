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
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Страница отзыва в мото/комТС - ссылка «Все отзывы о ...»")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class AllReviewsUrlTrucksTest {

    private static final String MARK = "foton";
    private static final String MODEL = "aumark_10xx";
    private static final String REVIEW_ID = "4033391";

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
                "reviews/ReviewsAutoTrucks",
                "reviews/SearchTrucksBreadcrumbsFotonAumark10xx").post();

        urlSteps.testing().path(REVIEW).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).path(REVIEW_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Все отзывы о ...»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onReviewPage().allReviewsUrl().hover().click();
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).path("/")
                .addParam("sort", ReviewsListingPage.SortBy.RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
    }
}
