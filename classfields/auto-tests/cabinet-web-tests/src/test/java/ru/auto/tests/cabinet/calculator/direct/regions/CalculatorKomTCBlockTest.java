package ru.auto.tests.cabinet.calculator.direct.regions;

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
import ru.auto.tests.desktop.categories.Testing;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.element.cabinet.calculator.Services.PREMIUM_SERVICE;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 10.12.18
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Прямой дилер (Регионы). Коммерческий транспорт")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CalculatorKomTCBlockTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CalculatorPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/DirectDealerRegions",
                        "cabinet/ApiAccessClient",
                        "cabinet/DealerTariff/TrucksNewUsedOff",
                        "cabinet/DesktopClientsGet/Dealer",
                        "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Выключённый тариф. Скриншот")
    public void shouldSeeKomTCBlock() {
        steps.onNewCalculatorPage().komTCBlock().click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().komTCBlock().click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Услуги при отключённом тарифе")
    public void shouldSeeServices() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().komTCBlock());
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().komTCBlock().amount());
        assertThat(prevAmount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Коммерческий транспорт").should(not(isDisplayed()));

        steps.addServiceInBlock(PREMIUM_SERVICE, steps.onNewCalculatorPage().komTCBlock());

        int amount = steps.extractSum(steps.onNewCalculatorPage().komTCBlock().amount());
        assertThat(amount).isNotZero().describedAs("Проверяем что сумма размещения увеличилась");
        steps.onNewCalculatorPage().balance().summary("Коммерческий транспорт").should(not(isDisplayed()));
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Включение тарифа")
    public void shouldSeeTurnOnTariff() {
        steps.expandCalculatorAndBalanceBlocks(steps.onNewCalculatorPage().komTCBlock());
        int prevAmount = steps.extractSum(steps.onNewCalculatorPage().komTCBlock().amount());
        assertThat(prevAmount).isZero().describedAs("Проверяем что сумма размещения равна нулю");
        steps.onNewCalculatorPage().balance().summary("Коммерческий транспорт").should(not(isDisplayed()));

        mockRule.delete();
        mockRule.newMock().with("cabinet/Session/DirectDealerRegions",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/TrucksNewUsedOn",
                "cabinet/ApiServiceAutoruAdsRequestCommercialClientPost").post();

        steps.onNewCalculatorPage().komTCBlock().button("Подключить тариф").click();
        steps.onNewCalculatorPage().serviceStatusPopup("Ваш тариф успешно изменен").should(isDisplayed());
        steps.onNewCalculatorPage().balance().summary("Коммерческий транспорт").should(isDisplayed());
    }
}
