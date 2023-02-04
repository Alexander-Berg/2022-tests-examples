package ru.auto.tests.cabinet.header;

import com.carlosbecker.guice.GuiceModules;
import com.carlosbecker.guice.GuiceTestRunner;
import io.qameta.allure.Feature;
import io.qameta.allure.Owner;
import io.qameta.allure.Step;
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
import ru.auto.tests.desktop.step.cabinet.CabinetOffersPageSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.auto.tests.commons.webdriver.WebDriverSteps.waitSomething;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.AVGRIBANOV;
import static ru.auto.tests.desktop.consts.Owners.JENKL;
import static ru.auto.tests.desktop.consts.Pages.SLASH;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isEnabled;

/**
 * @author Anton Tsyganov (jenkl)
 * @date 26.03.18
 */

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Шапка")
@RunWith(GuiceTestRunner.class)
@GuiceModules(CabinetTestsModule.class)
public class HeaderTest {
    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private CabinetOffersPageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                        "cabinet/ApiAccessClient",
                        "cabinet/CommonCustomerGet")
                .post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Хедер»")
    public void shouldSeeHeader() {
        steps.moveCursor(steps.onCabinetOffersPage().header());
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().header().waitUntil(isDisplayed()));

        urlSteps.onCurrentUrl().setProduction().open();

        steps.moveCursor(steps.onCabinetOffersPage().header());
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().header().waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Логотип на шапке")
    public void shouldSeeMainPage() {
        steps.onCabinetOffersPage().header().logo().click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SLASH).shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(JENKL)
    @DisplayName("Финансовый виджет»")
    public void shouldSeePersonalMenuBlock() {
        steps.onCabinetOffersPage().header().financialWidget().click();
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().header().financialWidget().waitUntil(isDisplayed()));

        urlSteps.onCurrentUrl().setProduction().open();
        steps.onCabinetOffersPage().header().financialWidget().click();

        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().header().financialWidget().waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }


    @Test
    @Category({Regression.class, Testing.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Кнопка «Пополнить» не активна")
    public void shouldSeeFinancialWidgetInactiveButton() {
        inactivateButtonBilling();
        steps.onCabinetOffersPage().header().financialWidget().button("Пополнить").should(not(isEnabled()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Кнопка «Пополнить» активна")
    public void shouldSeeFinancialWidgetActiveButton() {
        activateButtonBilling();
        steps.onCabinetOffersPage().header().financialWidget().button("Пополнить").should(isEnabled());
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Кнопка «Пополнить» не активна. Скриншот")
    public void shouldSeeFinancialWidgetInactiveButtonScreenshot() {
        inactivateButtonBilling();
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().header().financialWidgetOpen());

        urlSteps.onCurrentUrl().setProduction().open();
        inactivateButtonBilling();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithWaiting(
                steps.onCabinetOffersPage().header().financialWidgetOpen());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(AVGRIBANOV)
    @DisplayName("Кнопка «Пополнить» не активна. Скриншот")
    public void shouldSeeFinancialWidgetActiveButtonScreenshot() {
        activateButtonBilling();
        Screenshot testingScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().header().financialWidgetOpen().waitUntil(isDisplayed()));

        urlSteps.onCurrentUrl().setProduction().open();
        activateButtonBilling();
        Screenshot productionScreenshot = screenshotSteps.getElementScreenshotWithCutting(
                steps.onCabinetOffersPage().header().financialWidgetOpen().waitUntil(isDisplayed()));

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }


    @Step("Делаем кнопку пополнения активной")
    private void activateButtonBilling() {
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().header().financialWidget().click();
        steps.onCabinetOffersPage().header().financialWidget().rechargeAmount().click();
        steps.onCabinetOffersPage().header().financialWidget().rechargeAmount().sendKeys("123");
        steps.onCabinetOffersPage().header().financialWidget().click();
    }

    @Step("Делаем кнопку пополнения неактивной")
    private void inactivateButtonBilling() {
        waitSomething(2, TimeUnit.SECONDS);
        steps.onCabinetOffersPage().header().financialWidget().click();
    }

}
