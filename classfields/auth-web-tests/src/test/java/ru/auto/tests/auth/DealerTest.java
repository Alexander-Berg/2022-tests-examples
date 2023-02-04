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

import static ru.auto.tests.desktop.TestData.CLIENT_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AUTH;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Авторизация на промке дилеров")
@Feature(AUTH)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        urlSteps.testing().path(DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вход под уже зарегистрированным клиентом")
    public void shouldAuthClient() {
        basePageSteps.onPromoDealerPage().form().input("Электронная почта", CLIENT_PROVIDER.get().getLogin());
        basePageSteps.onPromoDealerPage().form().button("Продолжить").should(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().form().input("Введите пароль").waitUntil(isDisplayed());
        basePageSteps.onPromoDealerPage().form().input("Введите пароль", CLIENT_PROVIDER.get().getPassword());
        basePageSteps.onPromoDealerPage().form().button("Войти").waitUntil(isEnabled()).click();

        urlSteps.subdomain(SUBDOMAIN_CABINET).shouldNotSeeDiff();
    }
}
