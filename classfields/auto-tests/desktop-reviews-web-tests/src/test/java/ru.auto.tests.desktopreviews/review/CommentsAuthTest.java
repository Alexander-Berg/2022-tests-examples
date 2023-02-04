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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.USER_2_PROVIDER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - комментарии под зарегом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CommentsAuthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private LoginSteps loginSteps;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() throws IOException {
        loginSteps.loginAs(USER_2_PROVIDER.get());

        urlSteps.testing().path(REVIEW).path(CARS).path("/hyundai/tucson/2306975/4502999281016794678/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Добавление и удаление комментария")
    @Category({Regression.class})
    public void shouldAddAndDeleteComment() {
        String comment1Text = getRandomString();
        String comment2Text = steps.onReviewPage().comments().getComment(0).text().getText();
        steps.onReviewPage().comments().commentText().click();
        steps.onReviewPage().comments().helper().waitUntil(hasText("Для отправки нажмите Ctrl+Enter"));
        steps.onReviewPage().comments().commentText().sendKeys(comment1Text);
        steps.onReviewPage().comments().sendCommentButton().waitUntil(isDisplayed()).click();
        waitSomething(3, TimeUnit.SECONDS);
        steps.onReviewPage().comments().getComment(0).text().waitUntil(hasText(comment1Text));

        steps.onReviewPage().comments().getComment(0).button("Удалить").waitUntil(isDisplayed()).click();
        waitSomething(3, TimeUnit.SECONDS);
        steps.onReviewPage().comments().getComment(0).text().waitUntil(hasText(comment2Text));
    }
}