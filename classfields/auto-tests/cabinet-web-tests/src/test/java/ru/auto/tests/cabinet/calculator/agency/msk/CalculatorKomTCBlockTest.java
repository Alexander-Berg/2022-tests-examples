package ru.auto.tests.cabinet.calculator.agency.msk;

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
import ru.auto.tests.desktop.element.cabinet.calculator.KomTCCalculatorBlock;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import ru.auto.tests.desktop.step.cabinet.CalculatorPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.CALCULATOR;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 10.12.18
 */
@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Калькулятор. Агентский дилер.(Москва) Коммерческий транспорт")
@GuiceModules(CabinetTestsModule.class)
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
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

    @Parameterized.Parameter
    public String tab;

    @Parameterized.Parameters(name = "{index}: Коммерческий транспорт. Вкладка {0}")
    public static Collection<String> getParameters() {
        return asList(
                KomTCCalculatorBlock.HEAVE_COMMERCIAL_VEHICLES_USED,
                KomTCCalculatorBlock.HEAVE_COMMERCIAL_VEHICLES_NEW,
                KomTCCalculatorBlock.HEAVE_COMMERCIAL_VEHICLES,
                KomTCCalculatorBlock.SPECIAL_VEHICLES
        );
    }

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/AgencyDealerMoscow",
                "cabinet/ApiAccessClient",
                "cabinet/DealerTariff/TrucksOff",
                "cabinet/CommonCustomerGet").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(CALCULATOR).open();
        steps.onNewCalculatorPage().komTCBlock().click();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Блок «Коммерческий транспорт». Скриншот")
    public void shouldSeeKomTCBlock() {
        steps.onNewCalculatorPage().komTCBlock().tab(tab).click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onNewCalculatorPage().komTCBlock().click();
        steps.onNewCalculatorPage().komTCBlock().tab(tab).click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onNewCalculatorPage().komTCBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Подчеркивание выбраной вкладки")
    public void shouldSeeOpenedUsedCarsCalculator() {
        steps.onNewCalculatorPage().komTCBlock().tab(tab).click();
        steps.onNewCalculatorPage().komTCBlock().activeTab(tab)
                .waitUntil(isDisplayed());
    }
}
