package ru.auto.tests.desktop.promo;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.auto.tests.commons.util.Utils.getRandomEmail;
import static ru.auto.tests.commons.util.Utils.getRandomPhone;
import static ru.auto.tests.commons.util.Utils.getRandomString;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.PROMO;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Owners.NATAGOLOVKINA;
import static ru.auto.tests.desktop.consts.Pages.DEALER;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasAttribute;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@DisplayName("Промо - дилеры")
@Feature(PROMO)
@RunWith(GuiceTestRunner.class)
@GuiceModules(DesktopTestsModule.class)
public class DealerUnregTest {

    private long pageOffset;

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
        mockRule.newMock().with("desktop/SessionUnauth").post();

        urlSteps.testing().path(DEALER).open();
        pageOffset = basePageSteps.getPageYOffset();
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение блока «Стать клиентом»")
    public void shouldSeeBecomeClientBlock() {
        basePageSteps.onPromoDealerPage().becomeClientBlock().should(hasText("1. Введите электронную почту " +
                "для регистрации\nЭлектронная почта\nПродолжить\nАвторизуясь на сайте, " +
                "я принимаю условия пользовательского соглашения и даю согласие на обработку персональных данных " +
                "в соответствии с законодательством России и пользовательским соглашением."));
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Стать клиентом»")
    public void shouldClickPresentationRegButton() {
        basePageSteps.onPromoDealerPage().button("Стать клиентом").waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        waitSomething(3, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Презентация pdf»")
    public void shouldSeePresentationButton() {
        basePageSteps.onPromoDealerPage().button("Презентация pdf")
                .should(hasAttribute("href", "https://auto-export.s3.yandex.net/dealers/autoru_sales_kit.pdf"));
    }

    @Test
    @Owner(NATAGOLOVKINA)
    @Category({Regression.class, Testing.class})
    @DisplayName("Кнопка «Подключить в личном кабинете»")
    public void shouldClickSwitchInLkButton() {
        basePageSteps.onPromoDealerPage().button("Подключить в личном кабинете").waitUntil(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        waitSomething(3, TimeUnit.SECONDS);
        assertThat("Не произошел скролл к блоку", basePageSteps.getPageYOffset() > pageOffset);
    }

    @Test
    @Owner(DSVICHIHIN)
    @Category({Regression.class, Testing.class})
    @DisplayName("Отображение поп-апа обратного звонка")
    public void shouldSeeCallbackPopup() {
        basePageSteps.onPromoDealerPage().callbackButton().should(isDisplayed()).click();
        basePageSteps.onPromoDealerPage().callbackPopup().waitUntil(isDisplayed())
                .should(hasText("Заказ обратного звонка\nКак к вам обращаться?\nТелефон\nЭлектронная почта\n" +
                        "Комментарий\nПерезвоните мне"));
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
        basePageSteps.onPromoDealerPage().callbackPopup().requestResult().waitUntil(hasText("Заявка отправлена\n" +
                "Мы получили вашу заявку и свяжемся с вами в ближайшее время, чтобы уточнить детали"));
    }
}