package ru.auto.tests.cabinet.wallet;

import com.carlosbecker.guice.GuiceModules;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.junit4.DisplayName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - пополнения")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class WalletRechargesItemsTest {

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

    @Parameterized.Parameter
    public String itemTitle;

    @Parameterized.Parameter(1)
    public String itemParam;

    @Parameterized.Parameters(name = "{0}, {1}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"Размещение объявления", "placement"},
                {"Поднятие в поиске", "boost"},
                {"Trade-In «С пробегом»", "trade-in-request%3Acars%3Aused"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerWalletProductActivationsDailyStats",
                "cabinet/DealerWalletProductActivationsTotalStats").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Итого за период")
    public void shouldSeeTotal() {
        steps.onCabinetWalletPage().walletTotal().item(itemTitle).should(isDisplayed()).click();
        urlSteps.addParam("products", itemParam).shouldNotSeeDiff();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory());

        urlSteps.setProduction().subdomain(SUBDOMAIN_CABINET).path(WALLET).open();
        steps.onCabinetWalletPage().walletTotal().item(itemTitle).should(isDisplayed()).click();
        urlSteps.addParam("products", itemParam).shouldNotSeeDiff();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
