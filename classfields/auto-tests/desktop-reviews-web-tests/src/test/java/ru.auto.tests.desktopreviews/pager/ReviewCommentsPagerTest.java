package ru.auto.tests.desktopreviews.pager;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.element.desktopreviews.Comments.COMMENTS_PER_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Страница отзыва - пагинация комментариев")
@Feature(PAGER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewCommentsPagerTest {

    private static final String TEXT = "попробуй шевроле ланос посмотреть, надежная машина с неубиваемым двигателем, " +
            "минус - сильно гниют... за 110 - 120 т.р. можно взять более менее с живым кузовом. У родственника такой, " +
            "2007 года с кучей хозяев и скрученным  пробегом Бог ведает сколько сотен тысяч - год проездил (20 ткм) и " +
            "ни рубля не вложил.. В общем - рекомендую глянуть...";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Before
    public void before() {
        mockRule.newMock().with("reviews/SearchCarsBreadcrumbsUazPatriot",
                "desktop/SessionAuthUser",
                "reviews/ReviewsAutoCars",
                "reviews/ReviewsAutoReviewComments",
                "reviews/ReviewsAutoReviewCommentsPage2").post();

        urlSteps.testing().path(REVIEW).path(CARS).path("/uaz/patriot/2309645/4014660/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на страницу через пагинатор")
    public void shouldClickPage() {
        basePageSteps.onReviewPage().pager().page("2").click();
        urlSteps.addParam("comments_page", "2").shouldNotSeeDiff();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE));
        basePageSteps.onReviewPage().comments().getComment(0).text().waitUntil(hasText(TEXT));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «...»")
    public void shouldClickSkipButton() {
        mockRule.with("reviews/ReviewsAutoReviewCommentsPage5",
                "reviews/ReviewsAutoReviewCommentsPage6").update();

        basePageSteps.onReviewPage().pager().threeDotsFirst().click();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE));
        basePageSteps.onReviewPage().pager().currentPage().waitUntil(hasText("5"));
        urlSteps.addParam("comments_page", "5").shouldNotSeeDiff();
        basePageSteps.onReviewPage().pager().threeDotsLast().click();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE));
        basePageSteps.onReviewPage().pager().currentPage().waitUntil(hasText("6"));
        urlSteps.replaceParam("comments_page", "6").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickMoreButton() {
        basePageSteps.onReviewPage().pager().button("Показать ещё").click();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE * 2));
    }
}