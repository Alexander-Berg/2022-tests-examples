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

import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.USER_4_PROVIDER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(AutoruFeatures.REVIEWS)
@DisplayName("Страница отзыва - ответ на комментарий под зарегом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class CommentsReplyAuthTest {

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
        loginSteps.loginAs(USER_4_PROVIDER.get());

        urlSteps.testing().path(REVIEW).path(CARS).path("renault/duster/20417950/5117081178102382672/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class})
    @DisplayName("Добавление ответа на комментарий")
    public void shouldAddReply() {
        steps.hideElement(steps.onBasePage().discountTimer());
        int repliesCount = steps.onReviewPage().comments().getNonDeletedComment(0).repliesList().size();
        String replyText = getRandomString();

        steps.onReviewPage().comments().getNonDeletedComment(0).button("Ответить").click();
        steps.onReviewPage().comments().getNonDeletedComment(0).replyText().waitUntil(isDisplayed())
                .sendKeys(replyText);
        steps.onReviewPage().comments().getNonDeletedComment(0).sendReplyButton().waitUntil(isDisplayed()).click();
        waitSomething(1, TimeUnit.SECONDS);
        steps.onReviewPage().comments().getNonDeletedComment(0).repliesList().waitUntil(hasSize(repliesCount + 1));
        steps.onReviewPage().comments().getNonDeletedComment(0).getReply(repliesCount).text()
                .waitUntil(hasText(replyText));
    }
}
