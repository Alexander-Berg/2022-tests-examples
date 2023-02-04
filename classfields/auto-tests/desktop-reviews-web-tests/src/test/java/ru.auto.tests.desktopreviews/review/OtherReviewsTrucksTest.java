package ru.auto.tests.desktopreviews.review;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Story;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.page.desktopreviews.ReviewsListingPage.SortBy.RELEVANCE_DESC;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - другие отзывы в комтрансе")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class OtherReviewsTrucksTest {

    private static final String MARK = "foton";
    private static final String MODEL = "aumark_10xx";
    private static final String REVIEW_ID = "/4033391/";
    private static final String OTHER_REVIEW_ID = "/4029032/";

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
                "reviews/SearchTrucksBreadcrumbsFotonAumark10xx",
                "reviews/ReviewAutoListingTrucks").post();

        urlSteps.testing().path(REVIEW).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).path(REVIEW_ID).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    @Category({Regression.class})
    public void shouldClickReview() {
        basePageSteps.onReviewPage().getReview(0).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).path(OTHER_REVIEW_ID)
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Смотреть все»")
    @Category({Regression.class})
    public void shouldClickShowAllButton() {
        basePageSteps.onReviewPage().showAllButton().click();
        urlSteps.testing().path(REVIEWS).path(TRUCKS).path(TRUCK).path(MARK).path(MODEL).path("/")
                .addParam("sort", RELEVANCE_DESC.getAlias()).shouldNotSeeDiff();
    }
}