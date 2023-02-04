package ru.auto.tests.mobile.promo;

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
import ru.auto.tests.desktop.mobile.step.BasePageSteps;
import ru.auto.tests.desktop.module.MobileEmulationTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.desktop.TestData.CLIENT_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.CLIENTS;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_OFFICE7;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

@DisplayName("Промо - дилеры")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(MobileEmulationTestsModule.class)
public class DealerTest {

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

    @Before
    public void before() {
        urlSteps.testing().path(DEALER).open();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока «Стать клиентом» под незарегом")
    public void shouldSeeBecomeClientBlock() {
        basePageSteps.onPromoDealerPage().becomeClientBlock().should(hasText("1. Введите электронную почту " +
                "для регистрации\nЭлектронная почта\nПродолжить\nАвторизуясь на сайте, я принимаю условия " +
                "пользовательского соглашения и даю согласие на обработку персональных данных в соответствии " +
                "с законодательством России и пользовательским соглашением."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    public void shouldClickPresentationRegButton() {
        long pageOffset = basePageSteps.getPageYOffset();
        basePageSteps.onPromoDealerPage().presentationButton("Стать клиентом").waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Презентация pdf»")
    public void shouldClickPresentationButton() {
        basePageSteps.onPromoDealerPage().button("Презентация pdf")
                .should(hasAttribute("href", "https://auto-export.s3.yandex.net/dealers/autoru_sales_kit.pdf"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Подключить в личном кабинете» под дилером")
    public void shouldClickSwitchInLkButton() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/UserDealer").post();

        urlSteps.testing().path(DEALER).open();
        basePageSteps.onPromoDealerPage().button("Подключить в личном кабинете").waitUntil(isDisplayed()).click();
        urlSteps.fromUri("https://auth.auto.ru/login/?r=https%3A%2F%2Fcabinet.auto.ru%2Fsales%2F%3Ffrom%3Dfor-dealers-vas")
                .shouldNotSeeDiff();
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Подключить в личном кабинете» под незарегом")
    public void shouldClickSwitchInLkButtonUnreg() {
        long pageOffset = basePageSteps.getPageYOffset();
        basePageSteps.onPromoDealerPage().button("Подключить в личном кабинете").waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Регистрация клиента под незарегом")
    public void shouldRegisterClientUnreg() {
        urlSteps.testing().path(DEALER).open();

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
        basePageSteps.onPromoDealerPage().form().input("Телефон", getRandomPhone());
        basePageSteps.onPromoDealerPage().form().input("Как к вам обращаться?", "Иван Иванов");
        basePageSteps.onPromoDealerPage().form().input("Название автосалона", "Тестовый салон");
        basePageSteps.onPromoDealerPage().form().input("Город").click();
        basePageSteps.onPromoDealerPage().form().input("Город").clear();
        basePageSteps.onPromoDealerPage().form().input("Город", "Химки");
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(isDisplayed());
        basePageSteps.onPromoDealerPage().geoSuggestItem("Химки").waitUntil(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(not(isDisplayed()));
        basePageSteps.onPromoDealerPage().form().button("Начать работу").waitUntil(isEnabled()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Регистрация клиента под менеджером")
    public void shouldRegisterClientManager() {
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
        basePageSteps.onPromoDealerPage().geoSuggestItem("Химки").waitUntil(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().geoSuggest().waitUntil(not(isDisplayed()));
        basePageSteps.onPromoDealerPage().form().button("Начать работу").waitUntil(isEnabled()).click();
        urlSteps.shouldUrl(startsWith(urlSteps.subdomain(SUBDOMAIN_OFFICE7).path(CLIENTS).toString()), 20);
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
        urlSteps.fromUri(format("https://cabinet.%s/", urlSteps.getConfig().getBaseDomain())).shouldNotSeeDiff();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Блока «Стать клиентом» не должно быть под дилером")
    public void shouldNotSeeBecomeClientBlock() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "desktop/UserDealer").post();

        urlSteps.testing().path(DEALER).open();
        basePageSteps.onPromoDealerPage().becomeClientBlock().should(not(isDisplayed()));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отправка заявки на обратный звонок")
    public void shouldSendCallbackRequest() {
        basePageSteps.onPromoDealerPage().callbackButton().should(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().callbackPopup().input("Как к вам обращаться?", "Иван Иванов");
        basePageSteps.onPromoDealerPage().callbackPopup().input("Телефон").sendKeys(getRandomPhone());
        basePageSteps.onPromoDealerPage().callbackPopup().input("Электронная почта").sendKeys(getRandomEmail());
        basePageSteps.onPromoDealerPage().callbackPopup().input("Комментарий", getRandomString());
        basePageSteps.onPromoDealerPage().callbackPopup().button("Перезвоните мне").click();
        basePageSteps.onPromoDealerPage().callbackPopup().waitUntil(hasText("Заявка отправлена\n" +
                "Мы получили вашу заявку и свяжемся с вами в ближайшее время, чтобы уточнить детали"));
    }
}
