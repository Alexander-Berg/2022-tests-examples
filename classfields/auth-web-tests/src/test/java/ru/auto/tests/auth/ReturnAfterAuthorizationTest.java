package ru.auto.tests.auth;

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
import ru.auto.tests.desktop.module.DesktopTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.auto.tests.desktop.page.auth.AuthPage.CONTINUE_BUTTON;
import static ru.auto.tests.desktop.page.auth.AuthPage.SMS_CODE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Возвращение на исходную страницу после авторизации")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ReturnAfterAuthorizationTest {

    private static final String SALE_PATH = "/land_rover/discovery/1076842087-f1e84/";


    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "desktop/OfferCarsUsedUser",
                "desktop/AuthLoginOrRegisterRedirectToSale").post();

        urlSteps.testing().path(CARS).path(USED).path(SALE).path(SALE_PATH).open();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Возвращение на исходную страницу после авторизации")
    public void shouldReturnToStartPageAfterAuthorization() throws InterruptedException {
        basePageSteps.onCardPage().header().button("Войти").click();
        basePageSteps.onAuthPage().phoneInput().should(isDisplayed());
        basePageSteps.onAuthPage().phoneInput().sendKeys("89111111111");
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();

        TimeUnit.SECONDS.sleep(3);
        mockRule.delete();
        mockRule.newMock().with("desktop/UserConfirm",
                "desktop/SessionAuthUser",
                "desktop/OfferCarsUsedUser").post();

        basePageSteps.onAuthPage().input(SMS_CODE, "1234");

        urlSteps.shouldNotSeeDiff();
        basePageSteps.onMainPage().header().avatar().hover();
        basePageSteps.onMainPage().header().authDropdown().userId().waitUntil(hasText("id 11604617"));
    }
}
