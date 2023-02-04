package ru.auto.tests.mobilereviews.review;

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
import ru.auto.tests.desktop.consts.AutoruFeatures;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
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
@DisplayName("Страница отзыва - комментарии под зарегом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
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

        urlSteps.testing().path(REVIEW).path(CARS).path("/vaz/2114/3913672/1139554491464239353/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление и удаление комментария")
    public void shouldAddAndDeleteComment() {
        String comment1Text = getRandomString();
        String comment2Text = steps.onReviewPage().comments().getComment(0).text().getText();
        steps.onReviewPage().comments().commentText().click();
        steps.onReviewPage().comments().commentText().sendKeys(comment1Text);
        steps.onReviewPage().comments().sendCommentButton().waitUntil(isDisplayed()).click();
        waitSomething(3, TimeUnit.SECONDS);
        steps.onReviewPage().comments().getComment(0).text().waitUntil(hasText(comment1Text));

        steps.onReviewPage().comments().getComment(0).button("Удалить").waitUntil(isDisplayed()).hover()
                .click();
        waitSomething(3, TimeUnit.SECONDS);
        steps.onReviewPage().comments().getComment(0).text().waitUntil(hasText(comment2Text));
    }
}
