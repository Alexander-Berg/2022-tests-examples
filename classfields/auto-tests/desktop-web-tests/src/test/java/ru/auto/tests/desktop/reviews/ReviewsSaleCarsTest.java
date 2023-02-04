package ru.auto.tests.desktop.reviews;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления легковых - блок «Отзывы и рейтинг модели»")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsSaleCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";

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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/ReviewsAutoCarsCounter",
                "desktop/ReviewsAutoListingCars",
                "desktop/ReviewsAutoCarsRating",
                "desktop/ReviewsAutoFeaturesCars",
                "desktop/ReviewsAutoFeaturesCarsSnippet").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();

        basePageSteps.onCardPage().reviews().waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отзывов")
    public void shouldSeeReviews() {
        basePageSteps.onCardPage().reviews().header().should(hasText("Отзывы и рейтинг модели"));
        basePageSteps.onCardPage().reviews().reviewsList().forEach(item -> item.should(isDisplayed()));
        basePageSteps.onCardPage().reviews().getReview(0).should(hasText("Всё с ним было хорошо\n" +
                "Land Rover Discovery III\n4,8\nРебята, всем привет! Отзыв будет кратким, тк писать особо нечего. " +
                "Владел машиной 2.5 года, купил на пробеге 125тыс, продал на 185. Ездил и наслаждался. " +
                "Делал плановые ТО и работы. Были ещё какие-то ме\nЧитать далее"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onCardPage().reviews().button("Все отзывы")
                .should(hasAttribute("href",
                        format("https://%s/reviews/cars/land_rover/discovery/2307388/?from=card",
                                urlSteps.getConfig().getBaseDomain()))).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.onCardPage().reviews().getReview(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path("/land_rover/discovery/2307388/7156746515365802633/")
                .addParam("from", "card")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по плюсу в поп-апе")
    public void shouldClickPlusInPopup() {
        basePageSteps.onCardPage().reviews().getTabPlusMinus(0).should(hasText("Комфорт")).click();
        basePageSteps.onCardPage().reviews().reviewsPlusMinusPopup().plusReviewsList()
                .waitUntil(hasSize(42));
        basePageSteps.onCardPage().reviews().reviewsPlusMinusPopup().getPlus(0).click();
        basePageSteps.onCardPage().reviews().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(0));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву в поп-апе")
    public void shouldClickReviewInPopup() {
        basePageSteps.onCardPage().reviews().getTabPlusMinus(0).click();
        basePageSteps.onCardPage().reviews().reviewsPlusMinusPopup().getPlusReview(0)
                .waitUntil(hasText("«Комфорт»")).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path("/land_rover/discovery/2307388/6855/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Закрыть»")
    public void shouldClickCloseButton() {
        basePageSteps.onCardPage().reviews().getTabPlusMinus(0).should(hasText("Комфорт")).click();
        basePageSteps.onCardPage().reviews().reviewsPlusMinusPopup().closeButton().waitUntil(isDisplayed())
                .click();
        basePageSteps.onCardPage().reviews().reviewsPlusMinusPopup().waitUntil(not(isDisplayed()));
    }
}