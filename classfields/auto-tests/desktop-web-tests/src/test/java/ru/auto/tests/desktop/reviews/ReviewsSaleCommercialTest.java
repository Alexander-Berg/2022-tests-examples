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
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.TRUCK;
import static ru.auto.tests.desktop.consts.Pages.TRUCKS;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Карточка объявления мото/коммерческих - блок «Отзывы и рейтинг модели»")
@Feature(AutoruFeatures.REVIEWS)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReviewsSaleCommercialTest {

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
        mockRule.newMock().with("desktop/OfferTrucksUsedUser",
                "desktop/ReviewsAutoTrucksCounter",
                "desktop/ReviewsAutoListingTrucks",
                "desktop/ReviewsAutoTrucksRating",
                "desktop/ReviewsAutoFeaturesTrucks").post();

        urlSteps.testing().path(TRUCKS).path(USED).path(SALE).path(SALE_ID).open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение отзывов")
    public void shouldSeeReviews() {
        basePageSteps.onCardPage().reviews().should(hasText("Отзывы и рейтинг модели\n3,5\nОбщая оценка\nпо 8 отзывам\n" +
                "Плюсы и минусы\n3 / 3 / 1\nПлюсы\nМинусы\nСпорное\nРасход топлива\nСтоимость обслуживания\n" +
                "Управляемость\n" +
                "Рабочая машина для хозяина-водителя\nЗИЛ 5301 \"Бычок\"\n4,0\nРаботает и не подведёт, " +
                "если не превышать скоростные (70-80 км/ч) нормативы и своевременно проводить ТО используя качественный " +
                "материал. Не допускать перегрева двигателя, продолжительного буксования н\nЧитать далее\n" +
                "Зил Бычок\nЗИЛ 5301 \"Бычок\"\n4,2\nЗа такие деньги не найти машину для грузоперевозок. Не смотря на " +
                "то что это 3 тонник, 5 тонн таскает спокойно и при этом запаса прочности хватает. " +
                "Единственное не хватает мощности мотора и не очень уд\nЧитать далее\n" +
                "Отличная рабочая машина!\nЗИЛ 5301 \"Бычок\"\n4,2\nзил бычок, в отличии от многих других,достаточно " +
                "маневренный для своих внушительных размеров. Наличие двух рядов —6-7 посадочных мест — отлично для " +
                "самостоятельной бригады, да ещё и с наличием огромно\nЧитать далее\n" +
                "Мерседес 609 с кабиной ЗИЛ\nЗИЛ 5301 \"Бычок\"\n3,6\nНадёжная машина с \"вечным двигателем\". " + "" +
                "Заводится в любой мороз За все время ни разу серьезно не подводила, своевременно обслуживал, " + "" +
                "модернизировал под себя по мере необходимости. Есть спальник раскл\nЧитать далее\nВсе отзывы"));
        basePageSteps.onCardPage().reviews().reviewsList().forEach(item -> item.should(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Все отзывы»")
    public void shouldClickAllReviewsUrl() {
        basePageSteps.onCardPage().reviews().button("Все отзывы")
                .should(hasAttribute("href",
                        format("https://%s/reviews/trucks/all/?mark=zil&model=5301&from=card",
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
        urlSteps.testing().path(REVIEW).path(TRUCKS).path(TRUCK)
                .path("/zil/5301/7852399048512030044/").addParam("from", "card").shouldNotSeeDiff();
    }
}