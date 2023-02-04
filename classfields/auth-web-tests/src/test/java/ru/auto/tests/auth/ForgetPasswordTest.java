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

import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.page.auth.AuthPage.CONTINUE_BUTTON;
import static ru.auto.tests.desktop.page.auth.AuthPage.PHONE_EMAIL_INPUT;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Восстановление пароля")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class ForgetPasswordTest {

    private static final String EMAIL = "sosediuser1@mail.ru";
    private static final String USERNAME = "id 11604617";
    private static final String NEW_PASSWORD = "Brother12121988";

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
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(urlSteps.getConfig().getTestingURI().toString())).open();

        mockRule.newMock().with("desktop/AuthLoginOrRegisterEmail403",
                "auth/UserPasswordRequestReset",
                "auth/UserPasswordReset",
                "desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty").post();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Восстановление пароля по почте")
    public void shouldResetPassword() {
        basePageSteps.onAuthPage().input(PHONE_EMAIL_INPUT, EMAIL);
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();
        basePageSteps.onAuthPage().resend().waitUntil(hasText("Забыли пароль?"));
        basePageSteps.onAuthPage().resend().click();
        basePageSteps.onAuthPage().input("Код из письма", "490409");
        basePageSteps.onAuthPage().input("Новый пароль", NEW_PASSWORD);
        basePageSteps.onAuthPage().input("Повторите новый пароль", NEW_PASSWORD);
        basePageSteps.onAuthPage().button("Войти").click();

        urlSteps.testing().path(SLASH).shouldNotSeeDiff();
        basePageSteps.onMainPage().header().avatar().hover();
        basePageSteps.onMainPage().header().authDropdown().userId().waitUntil(hasText(USERNAME));
    }
}
