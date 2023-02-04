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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static java.lang.String.valueOf;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Artem Gribanov (avgribanov)
 * @date 07.12.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд. Пополнение баланса")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class PaymentTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerCampaigns",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/ApiClientInvoicePersons",
                "cabinet/ApiClientInvoicePost").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Успешная оплата картой")
    public void shouldCardBill() {
        steps.onCabinetDashboardPage().popupBillingBlock().waitUntil(isDisplayed());
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys("12345");
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("теставтору теставтору теставтору").click();
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonCardPay().waitUntil(isEnabled()).click();
    }

    @Test
    @Category({Regression.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Успешное выставление счёта")
    public void shouldPayBill() {
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("теставтору теставтору теставтору").click();
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Файл со счётом будет загружен на ваш компьютер"));
    }
}
