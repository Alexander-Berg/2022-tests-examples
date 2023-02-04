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

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. Страница конкретного фида. Пагинатор")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FeedsDownloadHistoryItemPagerTest {

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
                "cabinet/FeedsHistoryIdNotice",
                "cabinet/FeedsHistoryIdNoticePage2").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", "NOTICE")
                .open();
        screenshotSteps.setWindowSizeForScreenshot();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Смена страницы по клику на номер страницы")
    public void shouldClickSecondPage() {
        steps.onCabinetFeedsPage().pager().page("2").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        urlSteps.setProduction().subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", "NOTICE").open();
        steps.hideElement(steps.onBasePage().branch());
        steps.onCabinetFeedsPage().pager().page("2").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNextPage() {
        steps.onCabinetFeedsPage().pager().button("Следующая, Ctrl →").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        urlSteps.setProduction().subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", "NOTICE").open();
        steps.onCabinetFeedsPage().pager().button("Следующая, Ctrl →").click();
        urlSteps.addParam("page", "2").shouldNotSeeDiff();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Переход на следующую страницу по кнопке «Показать еще»")
    public void shouldClickShowMore() {
        steps.onCabinetFeedsPage().pager().button("Показать ещё").click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        urlSteps.setProduction().subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", "NOTICE").open();
        steps.onCabinetFeedsPage().pager().button("Показать ещё").click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
