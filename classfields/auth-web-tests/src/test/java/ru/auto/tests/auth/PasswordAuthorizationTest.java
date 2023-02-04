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

@DisplayName("Авторизация пользователя с паролем")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)

public class PasswordAuthorizationTest {

    private static final String EMAIL = "sosediuser1@mail.ru";
    private static final String USERNAME = "id 11604617";

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
                "desktop/AuthLogin",
                "desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty").post();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Авторизация по паролю")
    public void shouldAuthorizeByPassword() {
        basePageSteps.onAuthPage().input(PHONE_EMAIL_INPUT, EMAIL);
        basePageSteps.onAuthPage().button(CONTINUE_BUTTON).click();
        basePageSteps.onAuthPage().input("Пароль", "autoru");
        basePageSteps.onAuthPage().button("Войти").click();

        urlSteps.testing().path(SLASH).shouldNotSeeDiff();
        basePageSteps.onMainPage().header().avatar().hover();
        basePageSteps.onMainPage().header().authDropdown().userId().waitUntil(hasText(USERNAME));
    }
}
