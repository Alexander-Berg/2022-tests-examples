package ru.auto.tests.cabinet.tradein;

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
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.step.BasePageSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.SALE;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.TRADE_IN;
import static ru.auto.tests.desktop.consts.Pages.USED;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Трейд-ин")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class TradeInTest {

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

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/ClientsGet",
                "cabinet/DealerCampaignProducts",
                "cabinet/DealerTradeIn2019_03_19_2019_03_29").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(TRADE_IN).addParam("from_date", "2019-03-19")
                .addParam("to_date", "2019-03-29").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Заглушка при отсутствии заявок")
    public void shouldSeeStub() {
        mockRule.with("cabinet/DealerTradeIn2019_02_07_2019_02_07").update();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(TRADE_IN).addParam("from_date", "2019-02-07")
                .addParam("to_date", "2019-02-07").open();

        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetTradeInPage().tradeInBlock());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetTradeInPage().tradeInBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение заявок")
    public void shouldSeeTradeInItems() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetTradeInPage().tradeInBlock());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetTradeInPage().tradeInBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Выбор другой даты")
    public void shouldChangeDate() {
        mockRule.with("cabinet/DealerTradeIn2019_03_20_2019_03_27Page1").update();

        steps.onCabinetTradeInPage().calendarButton().should(isDisplayed()).click();
        steps.onCabinetTradeInPage().calendar().selectPeriod("20 марта", "27 марта");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(TRADE_IN).addParam("from_date", "2019-03-20")
                .addParam("to_date", "2019-03-27").addParam("page", "1")
                .addParam("section", "ALL").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по объявлению в заявке")
    public void shouldClickOffer() {
        steps.onCabinetTradeInPage().getTradeInItem(0).title().should(isDisplayed()).click();
        urlSteps.switchToNextTab();
        urlSteps.testing().path(CARS).path(USED).path(SALE)
                .path("/volkswagen/amarok/1082963162-af45e1ae/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пагинатор")
    public void shouldClickNextPage() {
        mockRule.with("cabinet/DealerTradeIn2019_03_19_2019_03_29Page2").update();

        steps.onCabinetTradeInPage().pager().page("2").should(isDisplayed()).click();
        urlSteps.addParam("page", "2").addParam("section", "ALL").shouldNotSeeDiff();
        steps.onCabinetTradeInPage().tradeInItemsList().waitUntil(hasSize(greaterThan(0)));
    }

    @Test
    @Category({Regression.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Пагинатор, Показать ещё")
    public void shouldClickShowMoreButton() {
        mockRule.with("cabinet/DealerTradeIn2019_03_19_2019_03_29Page2").update();

        int listSize = steps.onCabinetTradeInPage().tradeInItemsList().size();
        steps.onCabinetTradeInPage().pager().button("Показать ещё").should(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        steps.onCabinetTradeInPage().tradeInItemsList().waitUntil(hasSize(greaterThan(listSize)));
    }
}
