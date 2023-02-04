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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static java.lang.String.valueOf;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Дашборд. Автоматический овердрафт без задолжностей")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class AutoOverdraftWithoutSpentTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccountAutoOverdraft",
                "cabinet/ClientsGet",
                "cabinet/ApiClientInvoicePersons",
                "cabinet/ApiClientInvoicePost",
                "cabinet/InvoicePdf",
                "cabinet/DesktopSalonSilentPropertyUpdateOverdraft").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category(Regression.class)
    @Owner(DENISKOROBOV)
    @DisplayName("Подключение автоматического отложенного платежа, при отсутвие задолжностей")
    public void shouldActivateAutoPayment() {
        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Взять отложенный платёж").click();
        steps.onStartPage().popupBillingBlock().waitUntil(hasText(matchesPattern(("Отложенный платёж\nВы можете пополнить кошелёк на сумму " +
                "до 1 700 ₽, а оплатить в течение 15 дней до \\d{2}\\.\\d{2}\\.\\d{4}.\nСумма пополнения, ₽\nВыберите плательщика\nДля " +
                "добавления плательщика свяжитесь с менеджером\nсогласен и принимаю условия оферты\nПодключать " +
                "автоматически отложенный платёж при нулевом остатке\nВыставить счёт"))));
        steps.onStartPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onStartPage().popupBillingBlock().choicePayer().click();
        steps.onStartPage().selectPayer("avto avto avto").click();
        steps.onStartPage().popupBillingBlock().checkBoxOferta().click();
        steps.onStartPage().popupBillingBlock().button("Выставить счёт").click();
        steps.onCabinetOffersPage().notifier().waitUntil(isDisplayed())
                .should(hasText("Файл со счётом будет загружен на ваш компьютер")); }
}
