package ru.auto.tests.auth;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.CookieSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.auto.tests.desktop.mobile.page.AuthPage.SMS_CODE;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Адаптированный тест. Авторизация по номеру телефона зарегистрированного пользователя")
@Feature(AUTH)
@Story("Mobile auth")
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileTestsModule.class)
public class MobilePhoneAuthorizationTest {

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

    @Inject
    private CookieSteps cookieSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionUnauth",
                "mobile/AuthLoginOrRegisterRedirectToMain",
                "desktop/UserConfirm",
                "desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(urlSteps.getConfig().getMobileURI())).open();

        cookieSteps.setCookie("promo-app-banner-shown", "5", format(".%s",
                urlSteps.getConfig().getBaseDomain()));
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Авторизация по номеру телефона")
    public void shouldAuthorizeByPhone() {
        basePageSteps.onAuthPage().phoneInput().sendKeys("9111111111");

        waitSomething(3, TimeUnit.SECONDS);
        mockRule.overwriteStub(0, "desktop/SessionAuthUser");

        basePageSteps.onAuthPage().input(SMS_CODE, "1234");

        urlSteps.mobileURI().path(SLASH).shouldNotSeeDiff();
        basePageSteps.onMainPage().header().sidebarButton().click();
        basePageSteps.onMainPage().sidebar().username().waitUntil(hasText("sosediuser1"));
    }
}
