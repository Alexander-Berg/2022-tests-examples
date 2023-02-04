package ru.auto.tests.cabinet.orders;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.SUCHKOVDENIS;
import static ru.auto.tests.desktop.consts.Pages.CARS;
import static ru.auto.tests.desktop.consts.Pages.NEW;
import static ru.auto.tests.desktop.consts.Pages.ORDERS;
import static ru.auto.tests.desktop.consts.Pages.SETTINGS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.auto.tests.desktop.consts.Pages.SUBSCRIPTIONS;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Заявки на новый автомобиль")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class RequestsNewCarsTest {

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
                "cabinet/DealerCampaignProductsMatchApplicationCarsNew",
                "cabinet/DealerCampaignProductMatchApplicationCarsNewActivate",
                "cabinet/MatchApplicationsPage1",
                "cabinet/MatchApplicationsPage2").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(ORDERS).path(CARS).path(NEW).addParam("from", "2020-02-19")
                .addParam("to", "2020-02-28").open();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Включение/выключение заявок на подбор нового автомобиля")
    public void shouldTurnOn() {
        String buttonTitle = "Легковые новые";
        steps.onCabinetOrdersNewCarsPage().inactiveToggle(buttonTitle).should(isDisplayed()).click();
        steps.onCabinetOrdersNewCarsPage().activeToggle(buttonTitle).waitUntil(isDisplayed());
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение заявок")
    public void shouldSeeRequestItems() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetOrdersNewCarsPage().recallsNewCarsBlock());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetOrdersNewCarsPage().recallsNewCarsBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Выбор другой даты")
    public void shouldChangeDate() {
        steps.onCabinetOrdersNewCarsPage().calendarButton().should(isDisplayed()).click();
        steps.onCabinetOrdersNewCarsPage().calendar().selectPeriod("20 февраля", "27 февраля");
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(ORDERS).path(CARS).path(NEW).addParam("from", "2020-02-20")
                .addParam("to", "2020-02-27").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Клик по кнопке «Пересылать заявки»")
    public void shouldClickResendRequestsButton() {
        steps.onCabinetOrdersNewCarsPage().button("Пересылать заявки").waitUntil(isDisplayed()).click();
        steps.switchToNextTab();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(SETTINGS).path(SUBSCRIPTIONS).fragment("match-applications").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение общей информации по заявкам")
    public void shouldSeeTotalInfo() {
        steps.onCabinetOrdersNewCarsPage().totalInfo().should(hasText("60 заявок→37 500 ₽"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Открытие и закрытие блока детальной информации по заявке")
    public void shouldOpenAndCloseDetailedInfoBlock() {
        steps.onCabinetOrdersNewCarsPage().getRecallItem(0).title().should(isDisplayed()).click();
        steps.onCabinetOrdersNewCarsPage().detailedRequestInfo().waitUntil(isDisplayed()).should(isDisplayed());
        steps.onCabinetOrdersNewCarsPage().recallsNewCarsBlock().click();
        steps.onCabinetOrdersNewCarsPage().detailedRequestInfo().should(not(isDisplayed()));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Отображение контента блока детальной информации по заявке")
    public void shouldSeeRequestDetailedInfoBlock() {
        steps.onCabinetOrdersNewCarsPage().getRecallItem(0).title().should(isDisplayed()).click();
        steps.onCabinetOrdersNewCarsPage().detailedRequestInfo().content().waitUntil(hasText("—\nВремя оформления\n13:22, " +
                "4 марта 2020\nПодбирает\nМарка\nBMW\nМодель\nX1\nКредит\nДа\nКомментарий\nХочу купить!"));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Пагинатор, переход на вторую страницу")
    public void shouldClickNextPage() {
        steps.onCabinetOrdersNewCarsPage().pager().page("2").should(isDisplayed()).click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        steps.onCabinetOrdersNewCarsPage().requestItemsList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(SUCHKOVDENIS)
    @DisplayName("Пагинатор, Показать ещё")
    public void shouldClickShowMoreButton() {
        int listSize = steps.onCabinetOrdersNewCarsPage().requestItemsList().size();
        steps.onCabinetOrdersNewCarsPage().pager().button("Показать ещё").should(isDisplayed()).click();
        urlSteps.shouldNotSeeDiff();
        steps.onCabinetOrdersNewCarsPage().requestItemsList().should(hasSize(greaterThan(listSize)));
    }
}
