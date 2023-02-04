package ru.auto.tests.cabinet.crm.call;

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
import ru.auto.tests.desktop.step.BasePageSteps;
import ru.auto.tests.desktop.step.ScreenshotSteps;
import ru.auto.tests.desktop.step.UrlSteps;
import pazone.ashot.Screenshot;

import javax.inject.Inject;

import static ru.auto.tests.desktop.consts.AutoruFeatures.CABINET_DEALER;
import static ru.auto.tests.desktop.consts.Owners.DSVICHIHIN;
import static ru.auto.tests.desktop.consts.Pages.CALLS;
import static ru.auto.tests.desktop.consts.Pages.SUBDOMAIN_MANAGER;
import static ru.auto.tests.desktop.consts.QueryParams.CLIENT_ID;

@Feature(CABINET_DEALER)
@DisplayName("Кабинет дилера. Менеджер. Звонки")
@GuiceModules(CabinetTestsModule.class)
@RunWith(GuiceTestRunner.class)
public class CallsStatsTest {

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    @Inject
    public MockRule mockRule;

    @Inject
    private BasePageSteps basePageSteps;

    @Inject
    private UrlSteps urlSteps;

    @Inject
    private ScreenshotSteps screenshotSteps;

    @Before
    public void before() {
        mockRule.newMock().with("cabinet/Session/Manager",
                "cabinet/DealerTariff",
                "cabinet/ApiAccessClientManager",
                "cabinet/CommonCustomerGetManager",
                "cabinet/Calltracking",
                "cabinet/CalltrackingAggregated",
                "cabinet/CalltrackingSettings").post();

        urlSteps.subdomain(SUBDOMAIN_MANAGER).path(CALLS).addParam(CLIENT_ID, "16453").open();
    }

    @Test
    @Category({Regression.class, Screenshooter.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Отображение статистики")
    public void shouldSeeStats() {
        Screenshot testingScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsPage().stats());

        urlSteps.setProduction().open();
        Screenshot productionScreenshot = screenshotSteps
                .getElementScreenshotWithWaiting(basePageSteps.onCallsPage().stats());

        screenshotSteps.screenshotsShouldBeTheSame(testingScreenshot, productionScreenshot);
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Пропущенные»")
    public void shouldClickMissedUrl() {
        basePageSteps.onCallsPage().stats().buttonContains("Пропущенные").click();
        urlSteps.addParam("call_result", "MISSED_GROUP").shouldNotSeeDiff();
    }

    @Test
    @Category({Regression.class, Testing.class})
    @Owner(DSVICHIHIN)
    @DisplayName("Клик по ссылке «Принятые»")
    public void shouldClickAnsweredUrl() {
        basePageSteps.onCallsPage().stats().buttonContains("Принятые").click();
        urlSteps.addParam("call_result", "ANSWERED_GROUP").shouldNotSeeDiff();
    }
}
