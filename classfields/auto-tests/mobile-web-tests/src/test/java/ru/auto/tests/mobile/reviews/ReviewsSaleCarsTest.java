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
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.REVIEWS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Блок «Рейтинг и отзывы» на карточке объявления")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class ReviewsSaleCarsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
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
        mockRule.newMock().with("desktop/OfferCarsUsedUser",
                "desktop/ReviewsAutoCarsCounter",
                "mobile/ReviewsAutoListingCars",
                "desktop/ReviewsAutoCarsRating",
                "desktop/ReviewsAutoFeaturesCars",
                "desktop/ReviewsAutoFeaturesCarsSnippet").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().button("Пожаловаться на объявление").hover();
        waitSomething(1, TimeUnit.SECONDS);
        basePageSteps.onCardPage().reviews().hover();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение блока отзывов")
    public void shouldSeeReviewsBlock() {
        basePageSteps.onCardPage().reviews().should(hasText("Отзывы о Land Rover Discovery III\nНа основе 96 отзывов\n" +
                "4,6\nВсё с ним было хорошо\nLand Rover Discovery III\n4,8\nРебята, всем привет! Отзыв будет кратким, " +
                "тк писать особо нечего. Владел машиной 2.5 года, купил на пробеге 125тыс, продал на 185. Ездил и " +
                "наслаждался. Делал плановые ТО и работы. Были ещё какие-то ме\nЧитать далее\nАдвокат дья.. Land " +
                "Rover'а\nLand Rover Discovery III\n4,8\nДобрый день! Пролистывая отзывы владельцев Диско 3, я поймал " +
                "себя на мысли, что не могу согласиться с каким-либо из них хотя бы на половину. Поскольку машина " +
                "популярная и интерес к ней высок, решил\nЧитать далее\nОтзыв владельца про Land Rover Discovery 2007\n" +
                "Land Rover Discovery III\n4,2\nВ мае 2017 года стал обладателем Дискавери 3 поколения с дизелем и " +
                "АКПП в комплектации SE (любят англичане создавать достаточно странные комплектации. Мне досталась с " +
                "пневмоподвеской, полным внедорожн\nЧитать далее\nКупил. Муки выбора.\nLand Rover Discovery III\n5,0\n" +
                "Хотел иметь именно эту модель, именно такого дизайна. Плюсы, минусы читал и в отзывах здесь, " +
                "наслушался и от друзей. Но, как говорится, охота пуще неволи. Предыдущую машину использовал как " +
                "машину в\nЧитать далее\nВсе отзывы\nПлюсы и минусы\n14 / 13 / 3\nПлюсы\nМинусы\nСпорное\nКомфорт\n" +
                "39 / 3\nШумоизоляция\n25 / 0\nРасход топлива\n24 / 3\nНадежность\n23 / 1\nДизайн\n21 / 1\nЕщё 9"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onCardPage().reviews().button("Все отзывы").should(hasAttribute
                ("href", urlSteps.testing().path(REVIEWS).path(CARS).path(MARK).path(MODEL).path(GEN).path("/")
                        .addParam("from", "card").toString()));
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviews().button("Все отзывы"));
        urlSteps.testing().path(REVIEWS).addParam("from", "card").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву")
    public void shouldClickReview() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviews().getReview(0));
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/7156746515365802633/")
                .addParam("from", "card").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение поп-апа плюсов и минусов")
    public void shouldSeePopup() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0));
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(5, basePageSteps.onCardPage().reviewsPlusMinusPopup());

        urlSteps.onCurrentUrl().setProduction().open();
        basePageSteps.onCardPage().button("Пожаловаться на объявление").hover();
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0));
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(5, basePageSteps.onCardPage().reviewsPlusMinusPopup());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по плюсу в поп-апе")
    public void shouldClickPlusInPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0));
        basePageSteps.onCardPage().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(greaterThan(0)));
        basePageSteps.onCardPage().reviewsPlusMinusPopup().getPlus(0).click();
        basePageSteps.onCardPage().reviewsPlusMinusPopup().plusReviewsList().waitUntil(hasSize(0));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по отзыву в поп-апе")
    public void shouldClickReviewInPopup() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0));
        basePageSteps.onCardPage().reviewsPlusMinusPopup().getPlusReview(0).click();
        basePageSteps.switchToNextTab();
        urlSteps.testing().path(REVIEW).path(CARS).path(MARK).path(MODEL).path(GEN).path("/6855/")
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Закрыть»")
    public void shouldClickCloseButton() {
        basePageSteps.scrollAndClick(basePageSteps.onCardPage().reviewsPlusMinus().getPlusMinus(0));
        basePageSteps.onCardPage().reviewsPlusMinusPopup().closeButton().waitUntil(isDisplayed()).click();
        basePageSteps.onCardPage().reviewsPlusMinusPopup().waitUntil(not(isDisplayed()));
    }
}
