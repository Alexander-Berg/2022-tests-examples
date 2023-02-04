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
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.MOTO;
import static ru.auto.tests.desktop.consts.Pages.MOTORCYCLE;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления мото - блок «Отзывы и рейтинг модели»")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsSaleMotoTest {

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
        mockRule.newMock().with("desktop/OfferMotoUsedUser",
                "desktop/ReviewsAutoMotoCounter",
                "desktop/ReviewsAutoListingMoto",
                "desktop/ReviewsAutoMotoRating",
                "desktop/ReviewsAutoFeaturesMoto").post();

        urlSteps.testing().path(MOTORCYCLE).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отзывов")
    public void shouldSeeReviews() {
        basePageSteps.onCardPage().reviews().should(hasText("Отзывы и рейтинг модели\n4,8\nОбщая оценка\nпо 6 отзывам\n" +
                "Плюсы и минусы\n3 / 3 / 1\nПлюсы\nМинусы\nСпорное\nРасход топлива\nСтоимость обслуживания\n" +
                "Управляемость\n" +
                "Удобный городской мотоцикл\nHarley-Davidson Sportster 1200\n4,6\n" +
                "Свою легенду я купил почти случайно. Заехал в ХД Олимпийский посмотреть на новые мотоциклы и шоу-руме " +
                "увидел свой Sportster 1200 в трейд-ин. Посидел на нем, прокатился и купил. Мотоцикл хорош на каж\n" +
                "Читать далее\n" +
                "Маленький байк - большой потенциал\nHarley-Davidson Sportster 1200\n4,8\nБлагодаря большому баку " +
                "у этого по сути городского байка неплохие туристические возможности. " +
                "Запса хода на баке по трассе — 300+ км. Делал 1600 км Кисловодск — Москва за один день, почти ЖЖ. " +
                "Надежност\nЧитать далее\n" +
                "Хороший.Плохой.Злой\nHarley-Davidson V-Rod Muscle\n5,0\nНравится всем, хотят многие, " +
                "подойдёт не каждому.\nЧитать далее\n" +
                "Городской красавчик\nHarley-Davidson Street Rod\n4,2\nПриветствую всех " +
                "заинтересованных. Брал этот мотоцикл новым, за лето откатал полторы тысячи километров. " +
                "На первых порах был неприятно удивлён зубодробительной жестокостью подвесок, но через 500-600 ки\n" +
                "Читать далее\nВсе отзывы"));
        basePageSteps.onCardPage().reviews().reviewsList().forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onCardPage().reviews().button("Все отзывы")
                .should(hasAttribute("href",
                        format("https://%s/reviews/moto/all/?mark=harley_davidson&model=dyna_super_glide&from=card",
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
        urlSteps.testing().path(REVIEW).path(MOTO).path(MOTORCYCLE)
                .path("/harley_davidson/dyna_super_glide/1747147222378106200/").addParam("from", "card")
                .shouldNotSeeDiff();
    }
}