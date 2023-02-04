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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок отзывов в листинге")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsListingCarsTest {

    private static final String MARK = "land_rover";
    private static final String MODEL = "discovery";
    private static final String GEN = "2307388";

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
        mockRule.newMock().with("desktop/SearchCarsBreadcrumbsEmpty",
                "desktop/SearchCarsAllLandRoverDiscovery",
                "desktop/ReviewsAutoCarsCounterLandRoverDiscovery",
                "desktop/ReviewsAutoListingCarsLandRoverDiscovery",
                "desktop/ReviewsAutoCarsRatingLandRoverDiscovery",
                "desktop/ReviewsAutoFeaturesCarsLandRoverDiscovery",
                "desktop/ReviewsAutoFeaturesCarsSnippetLandRoverDiscovery").post();

        urlSteps.testing().path(CARS).path(MARK).path(MODEL).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение отзывов")
    public void shouldSeeReviews() {
        basePageSteps.onListingPage().reviews().header().should(hasText("Рейтинг и отзывы"));
        basePageSteps.onListingPage().reviews().reviewsList().forEach(item -> item.should(isDisplayed()));
        basePageSteps.onListingPage().reviews().getReview(0).should(hasText("Всё с ним было хорошо\n" +
                "Land Rover Discovery III\n4,8\nРебята, всем привет! Отзыв будет кратким, тк писать особо нечего. " +
                "Владел машиной 2.5 года, купил на пробеге 125тыс, продал на 185. Ездил и наслаждался. " +
                "Делал плановые ТО и работы. Были ещё какие-то ме\nЧитать далее"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onListingPage().reviews().button("Все отзывы").should(hasAttribute
                ("href", urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL)
                        .path("/").addParam("from", "listing").toString()));
        basePageSteps.onListingPage().reviews().button("Все отзывы").click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path("/").addParam("from", "listing").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.onListingPage().reviews().getReview(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/7156746515365802633/")
                .addParam("from", "listing").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по плюсу в поп-апе")
    public void shouldClickPlusInPopup() {
        basePageSteps.onListingPage().button("Все 14 плюсов").click();
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().plusReviewsList()
                .waitUntil(hasSize(0));
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().getPlus(0).click();
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(42));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по отзыву в поп-апе")
    public void shouldClickReviewInPopup() {
        basePageSteps.onListingPage().button("Все 14 плюсов").click();
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().getPlus(0).click();
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().getPlusReview(0)
                .waitUntil(hasText("«Комфорт»")).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/6855/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по кнопке «Закрыть»")
    public void shouldClickCloseButton() {
        basePageSteps.onListingPage().button("Все 14 плюсов").click();
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().closeButton().waitUntil(isDisplayed())
                .click();
        basePageSteps.onListingPage().reviews().reviewsPlusMinusPopup().waitUntil(not(isDisplayed()));
    }
}