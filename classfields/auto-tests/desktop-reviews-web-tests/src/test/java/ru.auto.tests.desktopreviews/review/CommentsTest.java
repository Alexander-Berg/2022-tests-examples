package ru.auto.tests.desktopreviews.review;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;

import static java.lang.String.format;
import static ru.auto.tests.desktop.TestData.USER_4_PROVIDER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - комментарии")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CommentsTest {

    private String firstCommentId;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(REVIEW).path(CARS).path("/chrysler/300c/2306011/4025020/").open();
        firstCommentId = basePageSteps.onReviewPage().comments().getComment(1).id().getAttribute("id");
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Пожаловаться» под незарегом")
    @Category({Regression.class})
    public void shouldClickComplainButtonUnauth() {
        String currentUrl = urlSteps.getCurrentUrl();
        basePageSteps.onReviewPage().comments().getComment(1).hover();
        basePageSteps.onReviewPage().comments().getComment(1).button("Пожаловаться").waitUntil(isDisplayed())
                .click();
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s#%s", currentUrl, firstCommentId))).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Пожаловаться» под зарегом")
    @Category({Regression.class})
    public void shouldClickComplainButtonAuth() throws IOException {
        loginSteps.loginAs(USER_4_PROVIDER.get());

        basePageSteps.onReviewPage().comments().getComment(1).hover();
        basePageSteps.onReviewPage().comments().getComment(1).button("Пожаловаться").waitUntil(isDisplayed())
                .click();
        basePageSteps.onReviewPage().notifier().waitUntil(hasText("Жалоба отправлена!"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по кнопке «Ссылка»")
    @Category({Regression.class})
    public void shouldClickUrlButton() {
        basePageSteps.onReviewPage().comments().getComment(1).hover();
        basePageSteps.onReviewPage().comments().getComment(1).button("Ссылка").waitUntil(isDisplayed())
                .click();
        basePageSteps.onReviewPage().notifier().waitUntil(hasText("Ссылка скопирована"));
    }
}