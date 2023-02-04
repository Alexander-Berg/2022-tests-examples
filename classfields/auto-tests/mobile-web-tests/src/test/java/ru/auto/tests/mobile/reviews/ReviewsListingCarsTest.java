package ru.auto.tests.mobile.reviews;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.ALL;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок отзывов в листинге")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with(
                "desktop/SearchCarsBreadcrumbsEmpty",
                "mobile/SearchCarsAllLandRoverDiscovery",
                "desktop/ReviewsAutoCarsCounterLandRoverDiscovery",
                "mobile/ReviewsAutoListingCarsLandRoverDiscovery",
                "desktop/ReviewsAutoCarsRatingLandRoverDiscovery",
                "desktop/ReviewsAutoFeaturesCarsLandRoverDiscovery",
                "desktop/ReviewsAutoFeaturesCarsSnippetLandRoverDiscovery").post();

        urlSteps.testing().path(CARS).path(MARK).path(MODEL).path(ALL).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение блока отзывов")
    public void shouldSeeReviewsBlock() {
        basePageSteps.onListingPage().reviews().should(hasText("Рейтинг и отзывы\nНа основе 96 отзывов\n4,6\nВсё с ним " +
                "было хорошо\nLand Rover Discovery III\n4,8\nРебята, всем привет! Отзыв будет кратким, тк писать особо" +
                " нечего. Владел машиной 2.5 года, купил на пробеге 125тыс, продал на 185. Ездил и наслаждался. " +
                "Делал плановые ТО и работы. Были ещё какие-то ме\nЧитать далее\nАдвокат дья.. Land Rover'а\nLand Rover " +
                "Discovery III\n4,8\nДобрый день! Пролистывая отзывы владельцев Диско 3, я поймал себя на мысли, что не " +
                "могу согласиться с каким-либо из них хотя бы на половину. Поскольку машина популярная и интерес к ней " +
                "высок, решил\nЧитать далее\nОтзыв владельца про Land Rover Discovery 2007\nLand Rover Discovery III\n" +
                "4,2\nВ мае 2017 года стал обладателем Дискавери 3 поколения с дизелем и АКПП в комплектации SE (любят " +
                "англичане создавать достаточно странные комплектации. Мне досталась с пневмоподвеской, полным " +
                "внедорожн\nЧитать далее\nКупил. Муки выбора.\nLand Rover Discovery III\n5,0\nХотел иметь именно эту " +
                "модель, именно такого дизайна. Плюсы, минусы читал и в отзывах здесь, наслушался и от друзей. Но, как " +
                "говорится, охота пуще неволи. Предыдущую машину использовал как машину в\nЧитать далее\nВсе отзывы"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onListingPage().reviews().button("Все отзывы").should(hasAttribute
                ("href", urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(SLASH)
                        .path(SLASH).addParam("from", "listing").toString()));
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviews().button("Все отзывы"));
        urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(SLASH)
                .addParam("from", "listing").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviews().getReview(0));
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/7156746515365802633/")
                .addParam("from", "listing").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение поп-апа плюсов и минусов")
    public void shouldSeePopup() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviewsPlusMinus().getPlusMinus(0));
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(5, basePageSteps.onListingPage().reviewsPlusMinusPopup());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onListingPage().button("Пожаловаться на объявление").hover();
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviewsPlusMinus().getPlusMinus(0));
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(5, basePageSteps.onListingPage().reviewsPlusMinusPopup());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по плюсу в поп-апе")
    public void shouldClickPlusInPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviewsPlusMinus().getPlusMinus(0));
        basePageSteps.onListingPage().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onListingPage().reviewsPlusMinusPopup().getPlus(0).click();
        basePageSteps.onListingPage().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(0));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по отзыву в поп-апе")
    public void shouldClickReviewInPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviewsPlusMinus().getPlusMinus(0));
        basePageSteps.onListingPage().reviewsPlusMinusPopup().getPlusReview(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/6855/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по кнопке «Закрыть»")
    public void shouldClickCloseButton() {
        basePageSteps.scrollAndClick(basePageSteps.onListingPage().reviewsPlusMinus().getPlusMinus(0));
        basePageSteps.onListingPage().reviewsPlusMinusPopup().closeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onListingPage().reviewsPlusMinusPopup().waitUntil(not(isDisplayed()));
    }
}
