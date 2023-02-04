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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.VIDEO)
@DisplayName("Страница отзыва - видео")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class VideosCarsTest {

    private static final int VIDEOS_COUNT = 3;
    private static final String MARK = "uaz";
    private static final String MODEL = "patriot";
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
        mockRule.newMock().with("reviews/SearchCarsBreadcrumbsUazPatriot",
                        "reviews/ReviewsAutoCars",
                        "reviews/CommentsReview",
                        "reviews/VideoSearchCarsPageSize")
                .post();

        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GENERATION).path(REVIEW_ID).open();
        basePageSteps.onReviewPage().footer().hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onReviewPage().videos().waitUntil(isDisplayed()).hover();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Видео в блоке «Популярные видео»")
    public void shouldSeePopularVideos() {
        basePageSteps.onReviewPage().videos().videosList().subList(0, VIDEOS_COUNT).forEach(item ->
                item.should(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по ссылке «Все видео»")
    public void shouldClickAllVideosUrl() {
        basePageSteps.onReviewPage().videos().button("Все видео").should(isDisplayed()).click();
        urlSteps.testing().path(VIDEO).path(CARS).path(MARK).path(MODEL).path(GENERATION).path("/")
                .addParam("from", "review-card").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по видео")
    public void shouldClickVideo() {
        basePageSteps.onReviewPage().videos().getVideo(0).waitUntil(isDisplayed()).click();
        basePageSteps.onReviewPage().videoFrame().waitUntil(isDisplayed());
    }
}
