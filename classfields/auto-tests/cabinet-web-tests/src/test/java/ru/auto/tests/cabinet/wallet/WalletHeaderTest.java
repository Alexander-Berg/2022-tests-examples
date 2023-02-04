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
import org.openqa.selenium.Keys;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - шапка")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class WalletHeaderTest {

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
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Поиск по VIN")
    public void shouldSearchByVin() {
        String vin = "JTEBR3FJ50K040106";
        steps.onCabinetWalletPage().walletHeader().vinInput().sendKeys(vin);
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).addParam("page_type", "expenses")
                .addParam("view_type", "offers").addParam("vin_number", vin).shouldNotSeeDiff();

        steps.onCabinetWalletPage().walletHeader().clearIcon().waitUntil(isDisplayed()).click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).addParam("page_type", "expenses")
                .addParam("view_type", "offers").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Некорректный VIN")
    public void shouldSeeVinError() {
        steps.onCabinetWalletPage().walletHeader().vinInput().sendKeys("ABC");
        steps.onCabinetWalletPage().walletHeader().vinInput().sendKeys(Keys.RETURN);
        steps.onCabinetWalletPage().walletHeader().vinError().waitUntil(isDisplayed());
    }
}
