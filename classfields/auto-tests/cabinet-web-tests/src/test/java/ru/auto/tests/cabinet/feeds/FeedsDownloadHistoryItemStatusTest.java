package ru.auto.tests.cabinet.feeds;

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
import ru.auto.tests.desktop.module.CabinetTestsModule;
import ru.auto.tests.desktop.rule.MockRule;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;
import java.util.Collection;

import static java.util.Arrays.asList;
import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.KRISKOLU;
import static ru.auto.tests.desktop.consts.Pages.FEEDS;
import static ru.auto.tests.desktop.consts.Pages.HISTORY;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_CABINET;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Фиды. Список объявлений при клике на разные статусы")
@RunWith(Parameterized.class)
@GuiceModules(CabinetTestsModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class FeedsDownloadHistoryItemStatusTest {

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

    @Parameterized.Parameter
    public String startStatusUrlParam;

    @Parameterized.Parameter(1)
    public String expectedStatus;

    @Parameterized.Parameter(2)
    public String expectedStatusUrlParam;

    @Parameterized.Parameters(name = "name = {index}: {0} {2}")
    public static Collection<Object[]> getData() {
        return asList(new Object[][]{
                {"NOTICE", "С ошибками", "ERROR"},
                {"ERROR", "С предупреждениями", "NOTICE"},
                {"ERROR", "Успешные", "NONE"},
        });
    }

    @Before
    public void before() {
        mockRule.newMock().with("desktop/SessionAuthDealer",
                "cabinet/ApiAccessClient",
                "cabinet/CommonCustomerGet",
                "cabinet/DealerAccount",
                "cabinet/DealerTariff",
                "cabinet/ClientsGet",
                "cabinet/FeedsHistoryId",
                "cabinet/FeedsHistoryIdError",
                "cabinet/FeedsHistoryIdNotice").post();
    }

    @Test
    @Category({Regression.class, Screenshooter.class})
    @Owner(KRISKOLU)
    @DisplayName("Список объявлений при клике на разные статусы")
    public void shouldClickStatus() {
        urlSteps.subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", startStatusUrlParam).open();
        steps.onCabinetFeedsPage().feedStatusBlock().statusLink(expectedStatus).click();
        urlSteps.replaceParam("error_type", expectedStatusUrlParam).shouldNotSeeDiff();
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        urlSteps.setProduction().subdomain(SUBDOMAIN_CABINET).path(FEEDS).path(HISTORY).path("/22719436/")
                .addParam("error_type", startStatusUrlParam).open();
        steps.onCabinetFeedsPage().feedStatusBlock().statusLink(expectedStatus).click();
        urlSteps.replaceParam("error_type", expectedStatusUrlParam).shouldNotSeeDiff();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(steps.onCabinetFeedsPage().feedStatusBlock());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }
}
