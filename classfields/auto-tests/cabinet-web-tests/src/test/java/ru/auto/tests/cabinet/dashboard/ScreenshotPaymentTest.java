package ru.auto.tests.cabinet.dashboard;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 07.12.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд. Пополнение баланса. Скриншоты")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class ScreenshotPaymentTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerCampaigns",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/ApiClientInvoicePersons").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Неактивные кнопки. Скриншот")
    public void shouldSeeInactiveButton() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetDashboardPage().activePopupWithoutBackground());

        urlSteps.setProduction().open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetDashboardPage().activePopupWithoutBackground());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Активные кнопки. Скриншот")
    public void shouldSeeActiveButton() {
        activateInput();
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().activePopupWithoutBackground());

        urlSteps.setProduction().open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        activateInput();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().activePopupWithoutBackground());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }


    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Активная кнопка Выставить счет при наведении. Скриншот")
    public void shouldSeeActiveButtonBillMoveCursor() {
        activateInput();
        steps.moveCursor(steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт"));
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().activePopupWithoutBackground());

        urlSteps.setProduction().open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        activateInput();
        steps.moveCursor(steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт"));
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().activePopupWithoutBackground());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Активная кнопка Оплата картой при наведении. Скриншот")
    public void shouldSeeActiveButtonCardMoveCursor() {
        activateInput();
        steps.moveCursor(steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Оплата картой"));
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().activePopupWithoutBackground());

        urlSteps.setProduction().open();
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        activateInput();
        steps.moveCursor(steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Оплата картой"));
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetDashboardPage().activePopupWithoutBackground());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Step("Делаем кнопки активными")
    private void activateInput() {
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys("12");
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("ОАЗИС").click();
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
    }
}
