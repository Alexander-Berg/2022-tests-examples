package ru.auto.tests.mag.desktop;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.USER_2_PROVIDER;
import static ru.auto.tests.desktop.TestData.USER_4_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.MAG;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.ARTICLE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MAG;
import static ru.auto.tests.desktop.mobile.page.MagPage.TEST_ARTICLE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Журнал - статья - комментарии")
@Feature(MAG)
@Ignore
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class MagArticleCommentsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    public LoginSteps loginSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление/удаление комментария первого уровня")
    public void shouldAddAndDeleteComment() throws IOException {
        loginSteps.loginAs(USER_2_PROVIDER.get());
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE).path(TEST_ARTICLE)
                .addParam("withDraftModel", "true").open();
        basePageSteps.onMagPage().comments().hover();
        String commentText = getRandomString();
        int commentsCount = basePageSteps.onMagPage().comments().commentsListFirstLevel().size();
        basePageSteps.onMagPage().comments().commentText().sendKeys(commentText);
        basePageSteps.onMagPage().comments().sendCommentButton().click();
        waitSomething(10, TimeUnit.SECONDS);
        basePageSteps.onMagPage().comments().getCommentFirstLevel(0).text().waitUntil(hasText(commentText));
        basePageSteps.onMagPage().comments().getCommentFirstLevel(0).hover();
        basePageSteps.onMagPage().comments().getCommentFirstLevel(0).button("Удалить").waitUntil(isDisplayed()).click();
        basePageSteps.onMagPage().comments().commentsListFirstLevel().waitUntil(hasSize(commentsCount));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление ответа на комментарий")
    public void shouldAddCommentReply() throws IOException {
        loginSteps.loginAs(USER_4_PROVIDER.get());
        urlSteps.subdomain(SUBDOMAIN_MAG).path(ARTICLE)
                .path(TEST_ARTICLE).addParam("withDraftModel", "true").open();
        basePageSteps.onMagPage().comments().hover();
        String replyText = getRandomString();
        int commentsCount = basePageSteps.onMagPage().comments().commentsListSecondLevel().size();
        basePageSteps.onMagPage().comments().getCommentFirstLevel(0).hover();
        basePageSteps.onMagPage().comments().getCommentFirstLevel(0).button("Ответить").waitUntil(isDisplayed()).click();
        basePageSteps.onMagPage().comments().replyInput().hover().click();
        basePageSteps.onMagPage().comments().replyInput().sendKeys(replyText);
        basePageSteps.onMagPage().comments().sendReplyButton().click();
        waitSomething(10, TimeUnit.SECONDS);
        basePageSteps.onMagPage().comments().getCommentSecondLevel(commentsCount).text().waitUntil(hasText(replyText));
        basePageSteps.onMagPage().comments().getCommentSecondLevel(commentsCount).button("Удалить").waitUntil(isDisplayed()).click();
        basePageSteps.onMagPage().comments().commentsListSecondLevel().waitUntil(hasSize(commentsCount));
    }
}