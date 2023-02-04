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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.mock.MockDealerSettings.mockDealerSettings;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_SETTINGS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд. Автоматический овердрафт")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class AutoOverdraftTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    private static final String PAYER = "avto avto avto";
    private static final String EMAIL = "123@123.ru";

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccountOverdraft"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/ApiClientInvoicePersons"),
                stub("cabinet/ApiClientInvoicePost"),
                stub("cabinet/InvoicePdf"),
                stub("cabinet/ApiSubscriptionClientId1Money"),
                stub("cabinet/ApiSubscriptionClientId1Delete"),

                stub().withPutDeepEquals(DEALER_SETTINGS)
                        .withRequestBody(mockDealerSettings().setOverdraftEnabledRequest(true).getRequestBody())
                        .withStatusSuccessResponse(),

                stub().withPutDeepEquals(DEALER_SETTINGS)
                        .withRequestBody(
                                mockDealerSettings().setOverdraftBalancePersonIdRequest("9270839").getRequestBody()
                        )
                        .withStatusSuccessResponse()
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category(Regression.class)
    @Owner(DENISKOROBOV)
    @DisplayName("Подключение автоматического отложенного платежа")
    public void shouldActivateAutoPayment() {
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Автоматический отложенный платёж").click();
        steps.onStartPage().popupBillingBlock().should(hasText("Настройка отложенного платежа\nКак только в кошельке " +
                "останется меньше средств, чем требуется на 1 день размещения, отложенный платёж произойдёт без вашего " +
                "участия, а счёт придёт вам на почту.\nВыберите плательщика\nсогласен и принимаю условия оферты" +
                "\nПодключить"));
        steps.onStartPage().popupBillingBlock().choicePayer().click();
        steps.onStartPage().selectPayer(PAYER).click();
        steps.onStartPage().popupBillingBlock().inputEmail().sendKeys(EMAIL);
        steps.onStartPage().popupBillingBlock().checkBoxOferta().click();
        steps.onStartPage().popupBillingBlock().button("Подключить").click();
        steps.onStartPage().notifier().waitUntil(isDisplayed()).should(hasText("Данные сохранены"));
    }
}
