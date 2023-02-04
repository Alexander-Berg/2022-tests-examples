package ru.auto.tests.mobile.reviews;

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
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.GARAGE;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Отзывы в гараже")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ReviewsGarageTest {

    private static final String VIN_CARD_ID = "/1146321503/";

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
                "desktop/SearchCarsBreadcrumbs",
                "desktop/ReferenceCatalogCarsSuggest",
                "desktop/GarageUserCardsVinPost",
                "desktop/GarageUserCardVin",
                "desktop/ReviewsAutoCarsCounterVolkswagenJetta7355324",
                "desktop/ReviewsAutoCarsRatingVolkswagenJetta7355324",
                "desktop/ReviewsAutoFeaturesCarsVolkswagenJetta7355324",
                "desktop/ReviewsAutoListingVolkswagenJetta7355324",
                "desktop/ReviewsAutoFeaturesCarsSnippetVolkswagenJetta7355324").post();

        urlSteps.testing().path(GARAGE).path(VIN_CARD_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отзывов")
    public void shouldSeeReviews() {
        basePageSteps.onGarageCardPage().reviews().title().should(hasText("Рейтинг модели"));
        basePageSteps.onGarageCardPage().reviews().reviewsList().forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по счётчику отзывов»")
    public void shouldClickCounterUrl() {
        basePageSteps.onGarageCardPage().reviews().button("по\u00a0165\u00a0отзывам")
                .should(hasAttribute("href",
                        format("%s/reviews/cars/volkswagen/jetta/7355324/?from=garage",
                                urlSteps.getConfig().getTestingURI()))).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Смотреть отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onGarageCardPage().reviews().button("Смотреть отзывы")
                .should(hasAttribute("href",
                        format("%s/reviews/cars/volkswagen/jetta/7355324/?from=garage",
                                urlSteps.getConfig().getTestingURI()))).hover().click();
        urlSteps.shouldSeeCertainNumberOfTabs(2);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.onGarageCardPage().reviews().getReview(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path("/volkswagen/jetta/7355324/1120378196532205944/")
                .addParam("from", "garage")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по плюсу в поп-апе")
    public void shouldClickPlusInPopup() {
        basePageSteps.onGarageCardPage().reviews().getPlusMinus(0).click();
        basePageSteps.onGarageCardPage().reviewsPlusMinusPopup().plusReviewsList()
                .waitUntil(hasSize(36));
        basePageSteps.onGarageCardPage().reviewsPlusMinusPopup().getPlus(0).click();
        basePageSteps.onGarageCardPage().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(0));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву в поп-апе")
    public void shouldClickReviewInPopup() {
        basePageSteps.onGarageCardPage().reviews().getPlusMinus(0).click();
        basePageSteps.onGarageCardPage().reviewsPlusMinusPopup().getPlusReview(0)
                .waitUntil(hasText("«В меру жесткий, в повороты входит как по рельсам, Управляемость -на высоте»"))
                .click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path("/volkswagen/jetta/7355324/4009168/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Закрыть»")
    public void shouldClickCloseButton() {
        basePageSteps.onGarageCardPage().reviews().getPlusMinus(0).click();
        basePageSteps.onGarageCardPage().reviewsPlusMinusPopup().closeButton().waitUntil(isDisplayed())
                .click();
        basePageSteps.onGarageCardPage().reviewsPlusMinusPopup().waitUntil(not(isDisplayed()));
    }
}
