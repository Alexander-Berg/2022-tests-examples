package ru.yandex.arenda;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Link;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.passport.account.Account;
import ru.yandex.arenda.module.ArendaWebModule;
import ru.yandex.arenda.steps.CompareSteps;
import ru.yandex.arenda.steps.LkSteps;
import ru.yandex.arenda.steps.PassportSteps;
import ru.yandex.arenda.steps.RetrofitApiSteps;
import ru.yandex.arenda.steps.UrlSteps;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.yandex.arenda.constants.UriPath.LK_FEEDBACK;
import static ru.yandex.arenda.constants.UriPath.LK_FEEDBACK_FORM;
import static ru.yandex.arenda.constants.UriPath.LK_FLAT;
import static ru.yandex.arenda.matcher.AttributeMatcher.hasHref;
import static ru.yandex.arenda.pages.BasePage.FEEDBACK;
import static ru.yandex.arenda.pages.LkFeedBackPage.HEADER_TEXT_FEED_BACK;
import static ru.yandex.arenda.pages.LkFeedBackPage.YOUR_FEEDBACK;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Link("https://st.yandex-team.ru/VERTISTEST-1833")
@DisplayName("[Arenda] Отзывы в ЛК Аренды")
@RunWith(GuiceTestRunner.class)
@GuiceModules(ArendaWebModule.class)
public class ReviewsTest {

    private static final String SMILES_TEXT = "☺ ☻ ☹ シ ツ ʕʘ‿ಠʔ ￣ヘ=)))";
    private static final String LINK = "https://yandex.ru/maps/org/yandeks_arenda/159725450042/reviews/" +
            "?ll=37.642474%2C55.735525&z=13";
    private static final String STAR_1 = "1";
    private static final String STAR_2 = "2";
    private static final String STAR_3 = "3";
    private static final String STAR_4 = "4";
    String uid;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private Account account;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private LkSteps lkSteps;

    @Inject
    private PassportSteps passportSteps;

    @Inject
    private RetrofitApiSteps retrofitApiSteps;

    @Inject
    private CompareSteps compareSteps;

    @Before
    public void before() {
        passportSteps.login(account);
        uid = account.getId();
        retrofitApiSteps.createUser(uid);
    }

    @Test
    @DisplayName("В меню отзывы доступны если есть созданная квартира")
    public void shouldSeeReviewWithFlat() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_FEEDBACK_FORM).open();
        lkSteps.onLkFeedBackPage().headerText(HEADER_TEXT_FEED_BACK).should(isDisplayed());
        lkSteps.onLkFeedBackPage().myCabinet().click();
        lkSteps.onLkFeedBackPage().myCabinetPopupDesktop().link(FEEDBACK).should(isDisplayed());
    }

    @Test
    @DisplayName("Отзыв с 1 баллом после рефреша показывается")
    public void shouldNotSeeReviewWithOneStar() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_FEEDBACK_FORM).open();
        lkSteps.onLkFeedBackPage().sendStarredReview(STAR_1);
        urlSteps.waitForUrl(LK_FLAT, 20);
        urlSteps.testing().path(LK_FEEDBACK).open();
        lkSteps.onLkFeedBackPage().link(YOUR_FEEDBACK).should(isDisplayed());
    }

    @Test
    @DisplayName("Оставить отзыв используя форматирование и смайлики")
    public void shouldSeeReviewWithEmoji() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_FEEDBACK_FORM).open();
        lkSteps.onLkFeedBackPage().star(STAR_4).click();
        String text = getRandomString() + SMILES_TEXT;
        lkSteps.onLkFeedBackPage().textarea().sendKeys(text);
        lkSteps.onLkFeedBackPage().sendButton().click();
        lkSteps.onLkFeedBackPage().link(YOUR_FEEDBACK).should(isDisplayed());
        lkSteps.onLkFeedBackPage().myReview().should(hasText(text));
    }

    @Test
    @DisplayName("Если ранее был отзыв и поменять его на отзыв с 1 баллом. Новый отзыв отображается после отправки")
    public void shouldSeeReviewWithNewStar() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_FEEDBACK_FORM).open();
        lkSteps.onLkFeedBackPage().sendStarredReview(STAR_3);
        urlSteps.waitForUrl(LK_FLAT, 20);
        urlSteps.testing().path(LK_FEEDBACK_FORM).open();
        lkSteps.onLkFeedBackPage().star(STAR_1).click();
        lkSteps.onLkFeedBackPage().sendButton().click();
        urlSteps.waitForUrl(LK_FLAT, 20);
        urlSteps.testing().path(LK_FEEDBACK).open();
        lkSteps.onLkFeedBackPage().link(YOUR_FEEDBACK).should(isDisplayed());
    }

    @Test
    @DisplayName("Не было ранее отзывов, оставить отзыв на 2 балла Отзыв ушел, на презентационной странице отзыв " +
            "видно и его можно изменить.")
    public void shouldSeeReviewWithEdit() {
        retrofitApiSteps.createConfirmedFlat(uid);
        urlSteps.testing().path(LK_FEEDBACK_FORM).open();
        lkSteps.onLkFeedBackPage().sendStarredReview(STAR_2);
        urlSteps.waitForUrl(LK_FLAT, 20);
        urlSteps.testing().path(LK_FEEDBACK).open();
        lkSteps.onLkFeedBackPage().link(YOUR_FEEDBACK).should(hasHref(equalTo(LINK)));
        lkSteps.onLkFeedBackPage().editLink().should(hasHref(containsString(LK_FEEDBACK_FORM)));
    }
}
