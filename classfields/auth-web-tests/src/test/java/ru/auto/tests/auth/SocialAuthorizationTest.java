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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@DisplayName("Соцавторизация")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class SocialAuthorizationTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(urlSteps.getConfig().getTestingURI().toString())).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Авторизация через Яндекс")
    public void shouldAuthorizeByYandex() {
        basePageSteps.onAuthPage().button("Войти с Яндекс ID").click();
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.onAuthPage().yandexAuthPopup().loginInput().sendKeys("yandex-team-09397.92349");
        basePageSteps.onAuthPage().yandexAuthPopup().submitButton().click();
        basePageSteps.onAuthPage().yandexAuthPopup().passwordInput().sendKeys("2FBl.DKn3");
        basePageSteps.onAuthPage().yandexAuthPopup().submitButton().click();
        waitSomething(5, TimeUnit.SECONDS);
        urlSteps.shouldSeeCertainNumberOfTabs(1);
        basePageSteps.switchToTab(0);

        urlSteps.testing().path(SLASH).shouldNotSeeDiff();
        basePageSteps.onMainPage().header().avatar().hover();
        basePageSteps.onMainPage().header().authDropdown().userId().waitUntil(hasText("id 68899600"));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Авторизация через Mail.ru")
    public void shouldAuthorizeByMailRu() {
        basePageSteps.onAuthPage().social().mailruButton().click();
        waitSomething(5, TimeUnit.SECONDS);
        basePageSteps.switchToNextTab();
        basePageSteps.onAuthPage().mailruAuthPopup().loginInput().sendKeys("test.autoru@mail.ru");
        basePageSteps.onAuthPage().mailruAuthPopup().passwordInput().sendKeys("EtSamoe!1");
        basePageSteps.onAuthPage().mailruAuthPopup().submitButton().click();
        waitSomething(5, TimeUnit.SECONDS);
        urlSteps.shouldSeeCertainNumberOfTabs(1);
        basePageSteps.switchToTab(0);
        urlSteps.testing().path(SLASH).shouldNotSeeDiff();
        basePageSteps.onMainPage().header().avatar().hover();
        basePageSteps.onMainPage().header().authDropdown().userName().waitUntil(hasText("Autoru Test"));
    }
}
