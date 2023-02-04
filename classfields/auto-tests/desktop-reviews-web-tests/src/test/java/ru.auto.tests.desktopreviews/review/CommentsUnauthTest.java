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
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.REVIEW;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@Feature(AutoruFeatures.REVIEWS)
@Story(AutoruFeatures.REVIEW)
@DisplayName("Страница отзыва - авторизация при отправке комментария под незарегом")
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class CommentsUnauthTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    public UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "reviews/ReviewsAutoCars",
                "reviews/SearchCarsBreadcrumbsUazPatriot",
                "desktop/AuthLoginOrRegister",
                "desktop/UserConfirm",
                "desktop/ProxyPublicApi").post();

        urlSteps.testing().path(REVIEW).path(CARS).path("/uaz/patriot/2309645/4014660/").open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @DisplayName("Авторизация при добавлении комментария под незарегом")
    @Category({Regression.class})
    public void shouldAuth() {
        steps.onReviewPage().comments().input("Номер телефона").sendKeys("9111111111");

        steps.onReviewPage().confirmButton().waitUntil(isEnabled()).click();
        steps.onReviewPage().codeInput().waitUntil(isDisplayed()).sendKeys("1234");
        steps.onReviewPage().comments().commentText().waitUntil(isDisplayed());
        steps.onReviewPage().comments().input("Номер телефона").waitUntil(not(isDisplayed()));
        steps.onReviewPage().header().avatar().waitUntil(isDisplayed());
    }
}