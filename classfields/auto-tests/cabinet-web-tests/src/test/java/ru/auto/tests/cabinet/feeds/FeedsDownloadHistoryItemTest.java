package ru.auto.tests.cabinet.feeds;

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
import pazone.ashot.Screenshot;
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;

import javax.inject.Inject;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.RCARD;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.hasText;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. Страница конкретного фида")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FeedsDownloadHistoryItemTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Inject
    private BasePageSteps steps;

    @Inject
    private UrlSteps urlSteps;

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/FeedsHistory",
                "cabinet/FeedsHistoryIdError",
                "cabinet/FeedsHistoryIdNotice").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", "ERROR").open();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Клик по VIN")
    public void shouldClickVIN() {
        steps.onCabinetFeedsPage().feedStatusBlock().getSaleStatus(0).linkVIN("SALLMAME4CA379581").click();
        steps.switchToNextTab();
        urlSteps.testing().path(RCARD).path("/1090001578-af37300d/").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Открытие детали ошибки")
    public void shouldClickDetails() {
        steps.onCabinetFeedsPage().feedStatusBlock().statusLink("С предупреждениями").click();
        urlSteps.replaceParam("error_type", "NOTICE").shouldNotSeeDiff();
        steps.onCabinetFeedsPage().feedStatusBlock().getSaleStatus(0).linkDetail("Показать ещё").click();
        steps.onCabinetFeedsPage().feedStatusBlock().getSaleStatus(0)
                .openDetails().should(hasText("Не указано значение комплектации, выберите из списка: SYNC Edition, " +
                        "Trend, Special Edition"));
    }

    @Test
    @Category({Regression.class})
    @Owner(KRISKOLU)
    @DisplayName("Клик по ссылке «К списку загрузок»")
    public void shouldClickDownloadListUrl() {
        mockRule.with("cabinet/FeedsHistory").update();

        steps.onCabinetFeedsPage().button("К списку загрузок").click();
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).shouldNotSeeDiff();
        steps.onCabinetFeedsPage().downloadHistory().feedsList().should(hasSize(greaterThan(0)));
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Отображение фида в истории загрузок")
    public void shouldSeeHistoryFeed() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedBlock());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
