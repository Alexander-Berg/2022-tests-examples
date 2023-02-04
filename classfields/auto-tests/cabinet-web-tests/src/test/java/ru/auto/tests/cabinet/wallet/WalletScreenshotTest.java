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
import pazone.ashot.Screenshot;
import ru.auto.tests.commons.runners.GuiceParametersRunnerFactory;
import ru.auto.tests.desktop.DesktopConfig;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;
import java.util.Collection;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static ru.auto.tests.cabinet.walkin.DatePickerTest.daysAgoText;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - отображение блоков на разных страницах")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class WalletScreenshotTest {

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
    private DesktopConfig config;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Parameterized.Parameter
    public String url;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"%swallet/"},
                {"%swallet/?view_type=offers"},
                {"%swallet/?page_type=recharges"}
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaigns",
                "cabinet/DealerWalletProductActivationsTotalStats",
                "cabinet/DealerWalletProductActivationsTotalOfferStatsPage2",
                "cabinet/DealerWalletProductActivationsTotalOfferStats",
                "cabinet/DealerWalletProductActivationsDailyStatsPage2",
                "cabinet/DealerWalletProductActivationsDailyStats",
                "cabinet/DealerWalletRechargesPage2",
                "cabinet/DealerWalletRecharges",
                "cabinet/DealerWalletDailyBalanceStats").post();

        urlSteps.fromUri(format(url, urlSteps.subdomain(SUBDOMAIN_CABINET))).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("График")
    public void shouldSeeGraph() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().graph());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().graph());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Итого за период")
    public void shouldSeeTotal() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другой даты")
    public void shouldChangeDate() {
        steps.onCabinetWalletPage().walletHeader().calendarButton().should(isDisplayed()).click();
        steps.onCabinetWalletPage().walletHeader().calendar().selectPeriod(daysAgoText(8), daysAgoText(1));
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory());

        urlSteps.setProduction().open();
        steps.onCabinetWalletPage().walletHeader().calendarButton().should(isDisplayed()).click();
        steps.onCabinetWalletPage().walletHeader().calendar().selectPeriod(daysAgoText(8), daysAgoText(1));
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пагинатор")
    public void shouldClickNextPage() {
        steps.onCabinetWalletPage().walletHistory().pager().page("2").should(isDisplayed()).click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        steps.onCabinetWalletPage().walletHistory().pager().currentPage().should(hasText("2"));
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        urlSteps.setProduction().fromUri(format(url, urlSteps.subdomain(SUBDOMAIN_CABINET))).open();
        steps.onCabinetWalletPage().walletHistory().pager().page("2").should(isDisplayed()).click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        steps.onCabinetWalletPage().walletHistory().pager().currentPage().should(hasText("2"));
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пагинатор, Показать ещё")
    public void shouldClickShowMoreButton() {
        steps.onCabinetWalletPage().walletHistory().pager().button("Показать ещё").should(isDisplayed()).click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        steps.onCabinetWalletPage().walletHistory().pager().currentPage().should(hasText("2"));
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        urlSteps.setProduction().fromUri(format(url, urlSteps.subdomain(SUBDOMAIN_CABINET))).open();
        steps.onCabinetWalletPage().walletHistory().pager().button("Показать ещё").should(isDisplayed()).click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        steps.onCabinetWalletPage().walletHistory().pager().currentPage().should(hasText("2"));
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetWalletPage().walletTotal(),
                        steps.onCabinetWalletPage().walletHistory(),
                        steps.onCabinetWalletPage().walletHistory().pager());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
