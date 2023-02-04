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

import static java.lang.String.format;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.LOGIN;
import static ru.auto.tests.desktop.consts.Pages.MOSKVA;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AUTH;
import static ru.lanwen.diff.uri.core.util.URLCoder.encode;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Выход из своего аккаунта")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class LogOutTest {

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
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/SearchCarsBreadcrumbsEmpty").post();

        urlSteps.testing().path(MOSKVA).open();
        mockRule.delete();
        mockRule.deleteCookies();
    }

    @Test
    @Owner(KRISKOLU)
    @Category({Regression.class, Testing.class})
    @DisplayName("Выход из аккаунта и переход в авторизацию")
    public void shouldLogOutAndClickLogin() {
        basePageSteps.onMainPage().header().avatar().hover();
        basePageSteps.onMainPage().header().authDropdown().button("Выйти").click();
        basePageSteps.onMainPage().header().button("Войти").should(isDisplayed()).click();

        urlSteps.subdomain(SUBDOMAIN_AUTH).path(LOGIN)
                .addParam("r", encode(format("%s%s", urlSteps.getConfig().getTestingURI().toString(), MOSKVA)))
                .shouldNotSeeDiff();
        basePageSteps.onAuthPage().phoneInput().should(isDisplayed());
    }
}
