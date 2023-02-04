package ru.auto.tests.mobile.my;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Epic;
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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.LK;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.ADD;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.EDIT;
import static ru.auto.tests.desktop.consts.Pages.MY;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы")
@Epic(LK)
@Feature(AutoruFeatures.REVIEWS)
@Story("Список отзывов")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class LkReviewsTest {

    private final static String ACTIVE_REVIEW_ID = "3867271072784994963";
    private final static String DRAFT_REVIEW_ID = "566989686761442140";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private BasePageSteps basePageSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "mobile/UserReviews",
                "mobile/ReviewsAuto",
                "mobile/ReviewsAutoDelete").post();

        urlSteps.testing().path(MY).path(REVIEWS).open();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение отзыва")
    public void shouldSeeReview() {
        basePageSteps.onLkReviewsPage().getReview(0).should(hasText("Опубликованный отзыв\nЗАЗ 1103 " +
                "«Славута» 4760888 1.2 MT (62 л.с.)\n22 января 2019\n10\n0 \n1509\n5,0\nРедактировать\nУдалить"));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение заблокированного отзыва")
    public void shouldSeeBlockedReview() {
        basePageSteps.onLkReviewsPage().getReview(4).should(hasText("Без заголовка\nChevrolet Cruze I " +
                "Рестайлинг 1.8 AT (141 л.с.)\nЗаблокирован\nНенормативная лексика\nРедактировать\nУдалить"));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение отзыва без заголовка")
    public void shouldSeeReviewWithoutTitle() {
        basePageSteps.onLkReviewsPage().getReview(2).title().should(hasText("Без заголовка"));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по заголовку отзыва")
    public void shouldClickReviewTitle() {
        basePageSteps.onLkReviewsPage().getReview(0).title().click();
        urlSteps.testing().path(REVIEW).path(CARS)
                .path("/zaz/slavuta/4760888/").path(ACTIVE_REVIEW_ID)
                .path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Редактировать»")
    public void shouldClickEditButton() {
        basePageSteps.onLkReviewsPage().getReview(0).button("Редактировать").should(isDisplayed()).click();
        urlSteps.desktopURI().path(CARS).path(REVIEWS).path(EDIT).path(ACTIVE_REVIEW_ID).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Опубликовать»")
    public void shouldClickPublishButton() {
        basePageSteps.onLkReviewsPage().getReview(2).button("Опубликовать").should(isDisplayed()).click();
        urlSteps.desktopURI().path(CARS).path(REVIEWS).path(EDIT).path(DRAFT_REVIEW_ID).path("/").fragment("validate")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Клик по кнопке «Добавить отзыв»")
    public void shouldClickAddReviewButton() {
        basePageSteps.onLkReviewsPage().addReviewButton().should(isDisplayed()).click();
        urlSteps.desktopURI().path(CARS).path(REVIEWS).path(ADD).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление отзыва")
    public void shouldDeleteReview() {
        int size = basePageSteps.onLkReviewsPage().reviewsList().size();
        String reviewTitle = basePageSteps.onLkReviewsPage().getReview(0).title().getText();
        basePageSteps.onLkReviewsPage().getReview(0).button("Удалить").click();
        basePageSteps.onLkReviewsPage().reviewsList().waitUntil(hasSize(size - 1));
        basePageSteps.onLkReviewsPage().getReview(0).title().waitUntil(not(hasText(reviewTitle)));
        urlSteps.shouldNotSeeDiff();
    }
}
