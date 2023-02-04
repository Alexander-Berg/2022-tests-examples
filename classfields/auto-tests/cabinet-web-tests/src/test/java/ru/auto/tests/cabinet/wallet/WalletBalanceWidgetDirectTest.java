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
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRuleConfigurable;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DENISKOROBOV;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.WALLET;
import static ru.auto.tests.desktop.mock.MockStub.stub;
import static ru.auto.tests.desktop.mock.Paths.DEALER_ACCOUNT;
import static ru.auto.tests.desktop.mock.MockError.getUnknownError;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кошелёк - балансовый виджет, прямой дилер")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class WalletBalanceWidgetDirectTest {

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

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.setStubs(
                stub("desktop/SessionAuthDealer"),
                stub("cabinet/ApiAccessClient"),
                stub("cabinet/CommonCustomerGet"),
                stub("cabinet/DealerAccount"),
                stub("cabinet/ClientsGet"),
                stub("cabinet/DealerCampaigns"),
                stub("cabinet/DealerWalletProductActivationsTotalStats"),
                stub("cabinet/DealerWalletProductActivationsTotalOfferStats"),
                stub("cabinet/DealerWalletProductActivationsDailyStats"),
                stub("cabinet/DealerWalletDailyBalanceStats")
        ).create();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(WALLET).open();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Кнопка «Пополнить кошелёк»")
    public void shouldSeeWalletPopup() {
        steps.onCabinetWalletPage().walletBalanceWidget().rechargeButton().click();
        steps.onCabinetWalletPage().walletBalanceWidget().popupBillingBlock().waitUntil(isDisplayed());
        steps.onCabinetWalletPage().walletBalanceWidget().popupBillingBlock().closePopupIcon().click();
        steps.onCabinetWalletPage().walletBalanceWidget().popupBillingBlock().waitUntil(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение балансового виджета")
    public void shouldSeeBalanceWidget() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithCutting(steps.onCabinetWalletPage().walletBalanceWidget()
                        .waitUntil(isDisplayed()));

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithCutting(steps.onCabinetWalletPage().walletBalanceWidget()
                        .waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(DENISKOROBOV)
    @DisplayName("Отображение виджета с ошибкой")
    public void shouldSeeBalanceWidgetWarning() {
        mockRule.overwriteStub(
                3, stub().withGetDeepEquals(DEALER_ACCOUNT)
                        .withResponseBody(getUnknownError())
                        .withStatusCode(SC_INTERNAL_SERVER_ERROR)
        );

        urlSteps.refresh();
        steps.onCabinetWalletPage().walletBalanceWidget().should(hasText("Кошелек\nОшибка загрузки\nПопробуйте перезагрузить страницу или обратитесь в поддержку\nВвести промокод\nБух. документы"));

    }
}
