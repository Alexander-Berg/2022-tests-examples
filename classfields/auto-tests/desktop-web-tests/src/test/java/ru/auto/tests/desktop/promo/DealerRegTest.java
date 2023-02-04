package ru.auto.tests.desktop.promo;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CLIENTS;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_OFFICE7;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Промо - Дилеры - Регистрация клиента под частником")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerRegTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    public BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Регистрация клиента под незарегом")
    public void shouldRegisterClientUnreg() {
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(DEALER).open();

        mockRule.delete();
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/AuthLoginOrRegisterEmail",
                "desktop/UserConfirmEmail",
                "desktop/User",
                "desktop/GeoSuggest",
                "desktop/CommonCustomerGet",
                "desktop/DesktopClientPost").post();

        basePageSteps.onPromoDealerPage().form().input("Электронная почта", "sosediuser1@mail.ru");
        basePageSteps.onPromoDealerPage().form().button("Продолжить").should(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().form().input("Введите код из письма").waitUntil(isDisplayed());
        basePageSteps.onPromoDealerPage().form().input("Введите код из письма", "1234");
        basePageSteps.onPromoDealerPage().form().input("Телефон").sendKeys(getRandomPhone());
        basePageSteps.onPromoDealerPage().form().input("Как к вам обращаться?", "Иван Иванов");
        basePageSteps.onPromoDealerPage().form().input("Название автосалона", "Тестовый салон");
        basePageSteps.onPromoDealerPage().form().input("Город").click();
        basePageSteps.onPromoDealerPage().form().input("Город").clear();
        basePageSteps.onPromoDealerPage().form().input("Город", "Химки");
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(isDisplayed());
        basePageSteps.onPromoDealerPage().geoSuggest().region("Химки").waitUntil(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(not(isDisplayed()));
        basePageSteps.onPromoDealerPage().form().button("Начать работу").waitUntil(isEnabled()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Регистрация клиента под частником")
    public void shouldRegisterByUser() {
        mockRule.newMock().with("desktop/SessionAuthUser",
                "desktop/User",
                "desktop/GeoSuggest",
                "desktop/CommonCustomerGet",
                "desktop/DesktopClientPost").post();

        urlSteps.testing().path(DEALER).open();
        basePageSteps.onPromoDealerPage().form().input("Телефон", "89000000000");
        basePageSteps.onPromoDealerPage().form().input("Как к вам обращаться?", "Иван");
        basePageSteps.onPromoDealerPage().form().input("Название автосалона", "Тест");
        basePageSteps.onPromoDealerPage().form().input("Город").click();
        basePageSteps.onPromoDealerPage().form().input("Город").clear();
        basePageSteps.onPromoDealerPage().form().input("Город", "Химки");
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(isDisplayed());
        basePageSteps.onPromoDealerPage().geoSuggest().region("Химки").waitUntil(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(not(isDisplayed()));
        basePageSteps.onPromoDealerPage().form().button("Начать работу").waitUntil(isEnabled()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path("/").shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Регистрация клиента под менеджером")
    public void shouldRegisterByManager() {
        mockRule.newMock().with("desktop/SessionAuthManager",
                "desktop/UserManager",
                "desktop/CommonCustomerGetManager",
                "desktop/GeoSuggest",
                "desktop/DesktopClientPostManager").post();

        urlSteps.testing().path(DEALER).open();
        basePageSteps.onPromoDealerPage().form().input("Телефон", "79111111111");
        basePageSteps.onPromoDealerPage().form().input("Как к вам обращаться?", "Ivan Ivanov");
        basePageSteps.onPromoDealerPage().form().input("Название автосалона", "Test Salon");
        basePageSteps.onPromoDealerPage().form().input("Город").click();
        basePageSteps.onPromoDealerPage().form().input("Город").clear();
        basePageSteps.onPromoDealerPage().form().input("Город", "Химки");
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(isDisplayed());
        basePageSteps.mouseClick(basePageSteps.onPromoDealerPage().geoSuggest().region("Химки").waitUntil(isDisplayed()));
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(not(isDisplayed()));
        basePageSteps.onPromoDealerPage().form().button("Начать работу").waitUntil(isEnabled()).click();
        urlSteps.shouldUrl(startsWith(urlSteps.subdomain(SUBDOMAIN_OFFICE7).path(CLIENTS).toString()), 20);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Вход под уже зарегистрированным клиентом")
    public void shouldAuthClient() {
        mockRule.newMock().with("desktop/AuthLoginOrRegisterEmailDealer403").post();

        urlSteps.testing().path(DEALER).open();
        basePageSteps.onPromoDealerPage().form().input("Электронная почта", "demo@auto.ru");
        basePageSteps.onPromoDealerPage().form().button("Продолжить").should(isDisplayed()).click();

        mockRule.newMock().with("desktop/AuthLoginDemo",
                "desktop/SessionAuthDealer",
                "desktop/UserDealer").post();

        basePageSteps.onPromoDealerPage().form().input("Введите пароль").waitUntil(isDisplayed());
        basePageSteps.onPromoDealerPage().form().input("Введите пароль", "autoru");
        basePageSteps.onPromoDealerPage().form().button("Войти").waitUntil(isEnabled()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path("/").shouldNotSeeDiff();
    }

}