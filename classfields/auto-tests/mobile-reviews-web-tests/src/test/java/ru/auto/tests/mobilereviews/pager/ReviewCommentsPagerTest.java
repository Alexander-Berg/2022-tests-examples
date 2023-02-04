package ru.auto.tests.mobilereviews.pager;

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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PAGER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.mobile.element.mobilereviews.Comments.COMMENTS_PER_PAGE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Страница отзыва - пагинация комментариев")
@Feature(PAGER)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ReviewCommentsPagerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEW).path(CARS).path("/gaz/9715/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Переход на страницу через пагинатор")
    public void shouldClickPage() {
        basePageSteps.onReviewPage().pager().button("2").click();
        urlSteps.addParam("comments_page", "2").shouldNotSeeDiff();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «...»")
    public void shouldClickSkipButton() {
        basePageSteps.onReviewPage().pager().button("…").click();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE));
        basePageSteps.onReviewPage().pager().currentPage().waitUntil(hasText("4"));
        urlSteps.addParam("comments_page", "4").shouldNotSeeDiff();
        basePageSteps.onReviewPage().pager().button("…").click();
        basePageSteps.onReviewPage().comments().commentsList().waitUntil(hasSize(COMMENTS_PER_PAGE));
        basePageSteps.onReviewPage().pager().currentPage().waitUntil(hasText("3"));
        urlSteps.replaceParam("comments_page", "3").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Клик по кнопке «Показать ещё»")
    public void shouldClickMoreButton() {
        basePageSteps.onReviewPage().pager().button("Показать ещё").click();
        basePageSteps.onReviewPage().comments().commentsList()
                .waitUntil(hasSize(COMMENTS_PER_PAGE * 2));
    }
}
