package ru.auto.tests.amp.reviews;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.Owners.TIMONDL;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.AMP;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.PROFILE;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.VIDEO;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.AMP)
@DisplayName("Страница отзыва. Проверка ссылок")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class ReviewLinksTest {

    private static final String REVIEW_PATH = "/uaz/patriot/2309645/4014660/";
    private static final String MARK_PATH = "/uaz/";
    private static final String MODEL_PATH = "/patriot/";
    private static final String GENERATION_PATH = "/2309645/";
    private static final String SORT_PARAMETER_VALUE = "relevance-exp1-desc";
    private static final String GL = "_gl";

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "reviews/ReviewsAutoCars",
                "reviews/ReviewAutoListingCars",
                "desktop/ApiRelatedUazPatriot",
                "reviews/ReviewsAutoCarsRatingUazPatriot",
                "reviews/ReviewsAutoCarsCounterUazPatriot2309645",
                "amp/SearchUazPatriot",
                "amp/SearchCarsUazPatriot",
                "amp/VideoSearchCarsUazPatriot").post();

        urlSteps.testing().path(AMP).path(REVIEW).path(CARS).path(REVIEW_PATH).open();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Скролл до комментариев")
    public void shouldScrollToComments() {
        String urlWithComments = urlSteps.fromUri(urlSteps.getCurrentUrl()).fragment("comments").toString();

        basePageSteps.onReviewPage().button("157 комментариев")
                .should(hasAttribute("href", urlWithComments)).click();
        assertThat("Не произошел скролл к комментариям", basePageSteps.getPageYOffset() > 0);
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Переход по ссылке на комментарии")
    public void shouldClick() {
        basePageSteps.onReviewPage().button("полной версии сайта").should(isDisplayed()).click();
        urlSteps.testing().path(REVIEW).path(CARS).path(REVIEW_PATH).ignoreParam(GL).fragment("reviewComments")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик в ссылку на автора")
    public void shouldClickAuthorProfile() {
        basePageSteps.onReviewPage().authorProfileLink().should(isDisplayed()).click();
        urlSteps.testing().path(PROFILE).path("/3562413/").ignoreParam(GL).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик на все отзывы в общем рейтинге")
    public void shouldClickAllReviewsInCommonRating() {
        basePageSteps.onReviewPage().button("на основании 158 отзывов").should(isDisplayed()).click();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK_PATH).path(MODEL_PATH).path(GENERATION_PATH)
                .addParam("sort", SORT_PARAMETER_VALUE).ignoreParam(GL).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок с объявлениями")
    public void shouldClickSales() {
        basePageSteps.onReviewPage().offersBlock().should(isDisplayed()).hover();
        basePageSteps.onReviewPage().offersBlock().items().should(hasSize(2));
        basePageSteps.onReviewPage().offersBlock().button("Смотреть все").should(isDisplayed()).click();

        urlSteps.testing().path(MOSKVA).path(AMP).path(CARS).path(MARK_PATH).path(MODEL_PATH).path(GENERATION_PATH)
                .path(ALL).ignoreParam(GL).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блок с видео")
    public void shouldClickVideo() {
        basePageSteps.onReviewPage().videoBlock().should(isDisplayed()).hover();
        basePageSteps.onReviewPage().videoBlock().items().should(hasSize(6));
        basePageSteps.onReviewPage().videoBlock().button("Все видео").should(isDisplayed()).click();

        urlSteps.testing().path(VIDEO).path(CARS).path(MARK_PATH).path(MODEL_PATH).path(GENERATION_PATH)
                .addParam("reviewId", "4014660").addParam("catalog_filter", "")
                .addParam("sort", SORT_PARAMETER_VALUE).ignoreParam(GL).shouldNotSeeDiff();
    }

    @Test
    @Owner(TIMONDL)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик на все отзывы внизу страницы")
    public void shouldClickAllReviewsInBottom() {
        basePageSteps.onReviewPage().button("Все отзывы УАЗ Patriot I").should(isDisplayed()).click();

        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK_PATH).path(MODEL_PATH).path(GENERATION_PATH)
                .addParam("sort", SORT_PARAMETER_VALUE).ignoreParam(GL).shouldNotSeeDiff();
    }
}
