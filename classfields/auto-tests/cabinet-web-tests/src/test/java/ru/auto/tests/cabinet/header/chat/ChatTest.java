package ru.auto.tests.cabinet.header.chat;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.LoginSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.TestData.CLIENT_2_PROVIDER;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка. Персональное меню. Чат")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class ChatTest {

    private static final int TIMEOUT = 10;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private LoginSteps loginSteps;

    @Before
    public void before() throws IOException {
        mockRule.newMock().with("cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/ChatMessageUnread",
                "cabinet/ChatMessagePost",
                "desktop/ProxyPublicApi").post();

        loginSteps.loginAs(CLIENT_2_PROVIDER.get());

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
        waitSomething(2, TimeUnit.SECONDS);
    }

    @Test
    @Ignore
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Отправка сообщения")
    public void shouldSeeSentMessage() {
        String chatId = "ab63f77a89a3c71652848a8ac10698ab";
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItemById(chatId).click();

        steps.onCabinetDashboardPage().chat().newMessage().click();
        String msg = "[Autotest] Тест";
        int size = steps.onCabinetDashboardPage().chat().chatMessages().size();
        steps.onCabinetDashboardPage().chat().newMessage().sendKeys(msg);
        steps.onCabinetDashboardPage().chat().sendMessage().click();
        steps.onCabinetDashboardPage().chat().chatMessage(size).text().should(hasText(msg));
    }

    @Test
    @Ignore
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Удаление чата")
    public void shouldSeeAlertWhenRemovingChat() {
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItem(1).click();

        steps.onCabinetDashboardPage().chat().ellipsis().click();
        steps.onCabinetDashboardPage().chat().chatMenu().chatItem("Удалить диалог").click();

        new WebDriverWait(steps.driver(), Duration.of(TIMEOUT, ChronoUnit.SECONDS))
                .until(ExpectedConditions.alertIsPresent());
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Закрытие чата")
    public void shouldCloseChat() {
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().should(isDisplayed());

        steps.onCabinetDashboardPage().chat().close().click();
        steps.onCabinetDashboardPage().chat().should(not(isDisplayed()));
    }

    @Test
    @Ignore
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Блокировка/разблокировка собеседника")
    public void shouldSeeBlockedInterlocutor() {
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").click();
        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").lastMessage().should(isDisplayed());
        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").blocked().should(not(isDisplayed()));

        steps.onCabinetDashboardPage().chat().ellipsis().click();
        steps.onCabinetDashboardPage().chat().chatMenu().chatItem("Заблокировать собеседника").click();

        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").lastMessage().should(not(isDisplayed()));
        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").blocked().should(isDisplayed());

        steps.onCabinetDashboardPage().chat().ellipsis().click();
        steps.onCabinetDashboardPage().chat().chatMenu().chatItem("Разблокировать собеседника").click();

        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").lastMessage().should(isDisplayed());
        steps.onCabinetDashboardPage().chat().chatItem("Покупатель").blocked().should(not(isDisplayed()));
    }

    @Test
    @Ignore
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Кнопка «Разблокировать»")
    public void shouldSeeUnlockedButton() {
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItem(1).lastMessage().should(isDisplayed());
        steps.onCabinetDashboardPage().chat().chatItem(1).blocked().should(not(isDisplayed()));
        steps.onCabinetDashboardPage().chat().chatItem(1).click();

        steps.onCabinetDashboardPage().chat().ellipsis().click();
        steps.onCabinetDashboardPage().chat().chatMenu().chatItem("Заблокировать собеседника").click();
        steps.onCabinetDashboardPage().chat().chatItem(1).blocked().waitUntil(isDisplayed());
        steps.onCabinetDashboardPage().chat().chatItem(1).lastMessage().waitUntil(not(isDisplayed()));

        steps.onCabinetDashboardPage().chat().unlocked().click();

        steps.onCabinetDashboardPage().chat().chatItem(1).blocked().waitUntil(not(isDisplayed()));
        steps.onCabinetDashboardPage().chat().chatItem(1).lastMessage().waitUntil(isDisplayed());
    }

    @Test
    @Ignore
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Отключение/включение уведомлений")
    public void shouldSeeDisabledNotification() {
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItem("holeden").click();
        steps.onCabinetDashboardPage().chat().chatItem("holeden").muted().should(not(isDisplayed()));

        steps.onCabinetDashboardPage().chat().ellipsis().click();
        steps.onCabinetDashboardPage().chat().chatMenu().chatItem("Отключить уведомления").click();

        steps.onCabinetDashboardPage().chat().chatItem("holeden").muted().should(isDisplayed());

        steps.onCabinetDashboardPage().chat().ellipsis().click();
        steps.onCabinetDashboardPage().chat().chatMenu().chatItem("Включить уведомления").click();

        steps.onCabinetDashboardPage().chat().chatItem("holeden").muted().should(not(isDisplayed()));
    }

    @Test
    @Ignore
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Чат с открытыми сообщениями")
    public void shouldSeeChatWithMessage() {
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItem(1).click();
        steps.onCabinetDashboardPage().chat().newMessage().waitUntil(isDisplayed());

        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().chat());

        urlSteps.onCurrentUrl().setProduction().open();
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetDashboardPage().header().chat().click();
        steps.onCabinetDashboardPage().chat().chatItem(1).click();
        steps.onCabinetDashboardPage().chat().newMessage().waitUntil(isDisplayed());

        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().chat());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Чат")
    public void shouldSeeChat() {
        steps.onCabinetDashboardPage().header().chat().click();

        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetDashboardPage().chat().waitUntil(isDisplayed()));

        urlSteps.onCurrentUrl().setProduction().open();
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetDashboardPage().header().chat().click();

        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetDashboardPage().chat().waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
