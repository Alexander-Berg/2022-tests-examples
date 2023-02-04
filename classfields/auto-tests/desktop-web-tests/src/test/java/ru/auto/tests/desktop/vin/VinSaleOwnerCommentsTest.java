package ru.auto.tests.desktop.vin;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import com.google.inject.Inject;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import static java.lang.String.format;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.VIN;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Комментарии в отчёте")
@Feature(VIN)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class VinSaleOwnerCommentsTest {

    private static final String SALE_ID = "/1076842087-f1e84/";
    private static final String VIN_COMMENTS_POPUP_COOKIE = "hide_promo_about_comments";
    private static final String VIN_PROMO_POPUP_COOKIE = "promo_popup_history_seller_closed";
    private static final String comment1 = "Comment 1";
    private static final String comment2 = "Comment 2";

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private CookieSteps cookieSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/OfferCarsUsedUserOwner"),
                stub("desktop/SessionAuthUser"),
                stub("desktop/CarfaxOfferCarsRawNotPaidError"),
                stub("desktop/CarfaxUserComment"),
                stub("desktop/CarfaxUserComment2"),
                stub("desktop/CarfaxUserCommentDelete")
        ).create();

        cookieSteps.setCookieForBaseDomain(VIN_PROMO_POPUP_COOKIE, "true");
        cookieSteps.deleteCookie(VIN_COMMENTS_POPUP_COOKIE);
        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_ID).open();
        basePageSteps.onCardPage().vinReport().waitUntil(isDisplayed());
        basePageSteps.onCardPage().vinReport().getPointFreeHistory(0).click();
        addComment(comment1);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Добавление комментария")
    public void shouldAddComment() {
        basePageSteps.onCardPage().vinReport().getComment(0).waitUntil(hasText("Комментарий владельца\n" +
                format("Редактировать\nУдалить\n%s", comment1)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Редактирование комментария")
    public void shouldEditComment() {
        basePageSteps.onCardPage().vinReport().getComment(0).button("Редактировать").waitUntil(isDisplayed())
                .click();
        basePageSteps.onCardPage().vinReport().commentInput().clear();
        basePageSteps.onCardPage().vinReport().commentInput()
                .sendKeys(Keys.HOME, Keys.chord(Keys.SHIFT, Keys.END, Keys.DELETE));
        addComment(comment2);
        basePageSteps.onCardPage().vinReport().getComment(0).waitUntil(hasText("Комментарий владельца\n" +
                format("Редактировать\nУдалить\n%s", comment2)));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Удаление комментария")
    public void shouldDeleteComment() {
        basePageSteps.onCardPage().vinReport().getComment(0).button("Удалить").waitUntil(isDisplayed())
                .click();
        basePageSteps.onCardPage().vinReport().commentsList().waitUntil(hasSize(0));
    }

    @Step("Добавляем комментарий")
    public void addComment(String comment) {
        basePageSteps.onCardPage().vinReport().commentInput().sendKeys(comment);
        basePageSteps.onCardPage().vinReport().sendCommentButton().waitUntil(isEnabled()).click();
    }
}
