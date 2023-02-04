package ru.auto.tests.cabinet.dashboard;

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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 31.01.19
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд. Пополнение баланса. Овердрафт. Кнопки оплаты")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class OverdraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccountOverdraft",
                "cabinet/ClientsGet",
                "cabinet/ApiClientInvoicePersons",
                "cabinet/ApiClientInvoicePost",
                "cabinet/InvoicePdf").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(JENKL)
    @DisplayName("Скриншот кошелька")
    public void shouldSeeWalletBalanceBlock() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetDashboardPage().dashboardWidget("Кошелёк"));

        urlSteps.setProduction().open();
        waitSomething(2, TimeUnit.SECONDS);
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetDashboardPage().dashboardWidget("Кошелёк"));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Кнопка «Новый счёт»")
    public void shouldPayButton() {
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Новый счёт").click();
        steps.onStartPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onStartPage().popupBillingBlock().choicePayer().click();
        steps.onStartPage().selectPayer("avto avto avto").click();
        steps.onStartPage().popupBillingBlock().checkBoxOferta().click();
        steps.onStartPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Файл со счётом будет загружен на ваш компьютер"));
    }

    @Test
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по кнопке «Оплата картой» в поп-апе «Новый счёт»")
    public void shouldClickNotPaidButton() {
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Новый счёт").click();
        steps.onStartPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onStartPage().popupBillingBlock().choicePayer().click();
        steps.onStartPage().selectPayer("avto avto avto").click();
        steps.onStartPage().popupBillingBlock().checkBoxOferta().click();
        steps.onStartPage().popupBillingBlock().button("Оплата картой").waitUntil(isEnabled()).click();;
    }

    @Test
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Клик по кнопке «Оплатить»")
    public void shouldClickPaidButton() {
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Оплатить").click();
        steps.onStartPage().popupBillingBlock().should(hasText("Оплатить счёт\nОплата отложенного платежа 1 000 ₽.\n" +
                "Счёт на отложенный платёж нужно обязательно оплатить на сумму 1 000 ₽. Если хотите пополнить " +
                "кошелёк на дополнительную сумму, выставьте обычный счёт на произвольную сумму и оплатите его.\n" +
                "Получить счёт"));
        steps.onStartPage().popupBillingBlock().button("Получить счёт").click();
        steps.onSettingsPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Файл со счётом будет загружен на ваш компьютер"));
    }
}
