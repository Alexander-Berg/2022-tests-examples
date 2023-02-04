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
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 31.01.19
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд. Пополнение баланса. Овердрафт")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class DeferredPaymentTest {

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
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Взять отложенный платёж").click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonRecharge().should(not(isEnabled()));
    }

    @Test
    @Category({Regression.class})
    @Owner(JENKL)
    @DisplayName("Неактивность кнопки. Все поля пустые")
    public void shouldButtonBillInactiveNull() {
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().click();
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys("1234");
        steps.onCabinetDashboardPage().popupBillingBlock().choicePayer().click();
        steps.onCabinetDashboardPage().selectPayer("ОАЗИС").click();
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonRecharge().should(isEnabled());
        steps.onCabinetDashboardPage().popupBillingBlock().closePopupIcon().click();
        steps.onCabinetDashboardPage().popupBillingBlock().should(not(isDisplayed()));
    }
}
