package ru.auto.tests.cabinet.agency.dashboard;

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
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.AgencyCabinetPagesSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static java.lang.String.valueOf;
import static org.hamcrest.Matchers.startsWith;
import static ru.auto.tests.desktop.consts.AutoruFeatures.AGENCY_CABINET;
import static ru.auto.tests.desktop.consts.Owners.CHERNOVPA;
import static ru.auto.tests.desktop.consts.Pages.CLIENTS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_AGENCY;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.utils.Utils.getRandomShortInt;

@Feature(AGENCY_CABINET)
@DisplayName("Кабинет агента. Дашборд")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class DashboardAgencyTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRuleConfigurable mockRule;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private AgencyCabinetPagesSteps steps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("cabinet/SessionAgencyClient"),
                stub("cabinet/AgencyAgencyGetClientId"),
                stub("cabinet/AgencyClientsGet"),
                stub("cabinet/ApiAccessClientAgency"),
                stub("cabinet/AgencyTeleponyStatsCallsDailyList"),
                stub("cabinet/DealerAccountAgencyClient"),
                stub("cabinet/CommonCustomerGetAgency"),
                stub("cabinet/DealerOffersDailyStats"),
                stub("cabinet/DealerWalletProductActivationsTotalStats")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_AGENCY).addParam(CLIENT_ID, "25718").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(CHERNOVPA)
    @DisplayName("Скриншот виджетов")
    public void shouldSeeDashboard() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetDashboardPage().dashboard());

        urlSteps.setProduction().open();
        steps.waitUntilPageIsFullyLoaded();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetDashboardPage().dashboard());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(CHERNOVPA)
    @DisplayName("Успешное выставление счета под агентством")
    public void shouldSeeSuccessfulPayment() {
        mockRule.setStubs(stub("cabinet/ApiClientInvoicePostAgency")).update();

        steps.onCabinetDashboardPage().dashboardWidget("Кошелёк").button("Пополнить счёт").click();
        steps.onCabinetDashboardPage().popupBillingBlock().inputSummForBill().sendKeys(valueOf(getRandomShortInt()));
        steps.onCabinetDashboardPage().popupBillingBlock().checkBoxOferta().click();
        steps.onCabinetDashboardPage().popupBillingBlock().buttonInBillingBlock("Выставить счёт").click();
        urlSteps.shouldUrl(startsWith("https://passport.yandex.ru/"), 2);

    }

    @Test
    @Category({Regression.class})
    @Owner(CHERNOVPA)
    @DisplayName("Переход на агентскую страницу Клиенты")
    public void shouldSeeUsersPage() {
        steps.onAgencyCabinetMainPage().header().cabinetPage("Клиенты").click();
        urlSteps.subdomain(SUBDOMAIN_AGENCY).path(CLIENTS).shouldNotSeeDiff();
    }
}
