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
import ru.auto.tests.desktop.categories.Regression;
import ru.auto.tests.desktop.categories.Screenshooter;
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.isDisplayed;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. История загрузок. Пагинатор")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class FeedsDownloadHistoryPagerTest {

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
                "cabinet/FeedsHistory").post();

        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Нажатие на кнопку «Показать ещё»")
    public void shouldClickSeeMore() {
        steps.onCabinetFeedsPage().pager().button("Показать ещё").click();
        steps.onCabinetFeedsPage().dataFeed("30 июля").should(isDisplayed());
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().blockFeedsHistory());

        urlSteps.setProduction().open();
        steps.onCabinetFeedsPage().pager().button("Показать ещё").click();
        steps.onCabinetFeedsPage().dataFeed("30 июля").should(isDisplayed());
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().blockFeedsHistory());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Переход на 2 страницу")
    public void shouldClickSecondPage() {
        steps.onCabinetFeedsPage().pager().page("2").click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().blockFeedsHistory());

        urlSteps.setProduction().open();
        steps.onCabinetFeedsPage().pager().page("2").click();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().blockFeedsHistory());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Переход на следующую страницу по кнопке «Следующая»")
    public void shouldClickNext() {
        steps.onCabinetFeedsPage().pager().button("Следующая, Ctrl →").click();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().blockFeedsHistory());

        urlSteps.setProduction().open();
        steps.onCabinetFeedsPage().pager().button("Следующая, Ctrl →").click();

        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().blockFeedsHistory());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
