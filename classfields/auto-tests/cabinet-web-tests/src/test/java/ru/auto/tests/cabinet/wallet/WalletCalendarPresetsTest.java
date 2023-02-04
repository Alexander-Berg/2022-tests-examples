package ru.auto.tests.cabinet.wallet;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.step.cabinet.DateFormatter.currentMonth;
import static ru.auto.tests.desktop.step.cabinet.DateFormatter.firstDayOfMonth;
import static ru.auto.tests.desktop.step.cabinet.DateFormatter.today;
import static ru.auto.tests.desktop.step.cabinet.DateFormatter.weekAgo;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - календарь, пресеты")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class WalletCalendarPresetsTest {

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
                        "cabinet/DealerWalletProductActivationsDailyStats",
                        "cabinet/DealerWalletProductActivationsTotalStats")
                .post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).open();
        steps.onCabinetWalletPage().walletHeader().calendarButton().should(isDisplayed()).click();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по пресету «Неделя»")
    public void shouldClickWeekPreset() {
        steps.onCabinetWalletPage().walletHeader().calendar().preset("Неделя").should(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).addParam("from", weekAgo()).addParam("to", today())
                .shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по пресету «30 дней»")
    public void shouldClick30DaysPreset() {
        steps.onCabinetWalletPage().walletHeader().calendar().preset("30 дней").should(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по пресету текущего месяца")
    public void shouldClickCurrentMonthPreset() {
        steps.onCabinetWalletPage().walletHeader().calendar().preset(currentMonth()).should(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).addParam("from", firstDayOfMonth()).addParam("to", today())
                .shouldNotSeeDiff();
    }
}
